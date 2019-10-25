package base.operators.h2o;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.HeaderExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.BinominalMapping;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowReader;
import base.operators.example.table.DoubleArrayDataRow;
import base.operators.example.table.ListDataRowReader;
import base.operators.example.table.NominalMapping;
import base.operators.example.table.PolynominalMapping;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
import water.DKV;
import water.Futures;
import water.Key;
import water.fvec.Frame;
import water.fvec.NewChunk;
import water.fvec.Vec;
import water.fvec.Vec.ESPC;

public class H2OConverter {
    public H2OConverter() { }

    public static Frame toH2OFrame(ExampleSet exampleSet, String frameName, boolean convertSpecial, boolean failForMissingLabel, Operator op) throws OperatorException {
        return toH2OFrame(exampleSet, frameName, convertSpecial, failForMissingLabel, op, (HeaderExampleSet)null);
    }

    public static Frame toH2OFrame(ExampleSet exampleSet, String frameName, boolean convertSpecial, boolean failForMissingLabel, Operator op, HeaderExampleSet header) throws OperatorException {
        ClusterManager.startCluster();
        Attributes attributes = exampleSet.getAttributes();
        Attribute label = attributes.getLabel();
        Attribute weight = attributes.getWeight();
        int size;
        if (convertSpecial) {
            size = attributes.allSize();
        } else {
            size = attributes.size();
            if (label != null) {
                ++size;
            }

            if (weight != null) {
                ++size;
            }
        }

        String[] vecNames = new String[size];
        Vec[] vecs = new Vec[size];
        Iterator<Attribute> attributesIterator = convertSpecial ? attributes.allAttributes() : attributes.iterator();
        ArrayList attrsToConvert = new ArrayList();

        while(attributesIterator.hasNext()) {
            attrsToConvert.add(attributesIterator.next());
        }

        if (!convertSpecial && weight != null) {
            attrsToConvert.add(weight);
        }

        if (!convertSpecial && label != null) {
            attrsToConvert.add(label);
        }

        int labelIndex = attrsToConvert.size() - 1;
        long allStart = 0L;
        NewChunk[] ncs = new NewChunk[attrsToConvert.size()];

        for(int i = 0; i < attrsToConvert.size(); ++i) {
            Attribute attribute = (Attribute)attrsToConvert.get(i);
            vecNames[i] = attribute.getName();
            Key<Vec> vecKey = i == 0 ? Vec.newKey() : vecs[0].group().addVec();
            if (attribute.isNominal()) {
                NominalMapping mapping = attribute.getMapping();
                String[] domain = (String[])mapping.getValues().toArray(new String[0]);
                if (header != null) {
                    Attribute headerAttribute = header.getAttributes().get(attribute.getName());
                    if (headerAttribute == null) {
                        throw new OperatorException("The attribute " + attribute.getName() + " is missing from the model!");
                    }

                    NominalMapping headerMapping = headerAttribute.getMapping();
                    domain = (String[])headerMapping.getValues().toArray(new String[0]);
                }

                vecs[i] = new Vec(vecKey, Vec.ESPC.rowLayout(vecKey, new long[]{0L, (long)exampleSet.size()}), domain, (byte)4);
            } else if (attribute.isNumerical() || attribute.isDateTime()) {
                vecs[i] = new Vec(vecKey, ESPC.rowLayout(vecKey, new long[]{0L, (long)exampleSet.size()}));
            }

            ncs[i] = new NewChunk(vecs[i], 0);
        }

        StreamSupport.stream(exampleSet.spliterator(), false).forEachOrdered((example) -> {
            for(int i = 0; i < attrsToConvert.size(); ++i) {
                ncs[i].addNum(example.getValue((Attribute)attrsToConvert.get(i)));
            }

        });
        Futures fs = new Futures();

        for(int i = 0; i < attrsToConvert.size(); ++i) {
            ncs[i].close(fs);
            DKV.put(vecs[i]._key, vecs[i], fs);
        }

        fs.blockForPending();
        if (failForMissingLabel && label != null && vecs[labelIndex].naCnt() > 0L) {
            throw new UserError(op, 162, new Object[]{label.getName()});
        } else {
            Frame frame = new Frame(Key.make(frameName), vecNames, vecs);
            DKV.put(frame);
            return frame;
        }
    }

    public static ExampleSet toExampleSet(Frame frame) throws OperatorException {
        Vec[] vecs = frame.vecs();
        if (vecs.length == 0) {
            return ExampleSets.from(new Attribute[0]).build();
        } else {
            List<Attribute> attributes = new ArrayList();

            for(int j = 0; j < vecs.length; ++j) {
                int type = 0;
                Vec currentVec = vecs[j];
                byte h2oType = currentVec.get_type();
                if (h2oType == 3) {
                    type = 2;
                    attributes.add(AttributeFactory.createAttribute(frame.name(j), type));
                } else if (h2oType == 4) {
                    type = 1;
                    Map<Integer, String> nomMap = new HashMap();

                    for(int i = 0; i < currentVec.cardinality(); ++i) {
                        nomMap.put(i, vecs[j].factor((long)i));
                    }

                    Attribute a = AttributeFactory.createAttribute(frame.name(j), type);
                    a.setMapping(new PolynominalMapping(nomMap));
                    attributes.add(a);
                }

                if (type == 0) {
                    throw new OperatorException("Conversion error. Unexpected H2O type: " + vecs[j].get_type_str());
                }
            }

            Iterator<DataRow> it = ((List)LongStream.range(0L, vecs[0].length()).mapToObj((ix) -> {
                double[] row = new double[vecs.length];

                for(int j = 0; j < vecs.length; ++j) {
                    row[j] = vecs[j].at(ix);
                }

                return new DoubleArrayDataRow(row);
            }).collect(Collectors.toList())).iterator();
            DataRowReader drr = new ListDataRowReader(it);
            return ExampleSets.from(attributes).withDataRowReader(drr).build();
        }
    }

    public static void fillPredictionAttributes(ExampleSet exampleSet, Frame predictionFrame, HeaderExampleSet trainingHeader) throws OperatorException {
        Vec[] predictionVecs = predictionFrame.vecs();
        String[] predictionVecNames = predictionFrame.names();
        if (predictionVecNames.length == 0) {
            throw new OperatorException("The predictionFrame is empty!");
        } else {
            Attributes attributes = exampleSet.getAttributes();
            Attribute[] predictionAttributes = new Attribute[predictionVecNames.length];
            predictionAttributes[0] = attributes.getPredictedLabel();
            Attribute predictionAttr;
            if (trainingHeader.getAttributes().getLabel().isNominal()) {
                List<String> predictionVecNamesFromTraining = trainingHeader.getAttributes().getLabel().getMapping().getValues();

                for(int col = 1; col < predictionVecs.length; ++col) {
                    String confidenceAttributeName = "confidence(" + (String)predictionVecNamesFromTraining.get(col - 1) + ")";
                    predictionAttr = attributes.get(confidenceAttributeName);
                    if (predictionAttr == null) {
                        throw new OperatorException("Confidence attribute not found! Expected name: " + confidenceAttributeName);
                    }

                    predictionAttributes[col] = predictionAttr;
                }
            }

            long allStart = 0L;

            int row;
            for(row = 0; row < predictionVecs.length; ++row) {
                predictionAttr = predictionAttributes[row];
                Vec predictionVec = predictionVecs[row];
                if (predictionAttr.isNominal()) {
                    String[] domain = predictionVec.domain();
                    if (domain == null) {
                        throw new OperatorException("H2O generated numerical predicted label instead of nominal! Please check your parameter setup.");
                    }

                    Object mapping;
                    if (predictionAttr.getValueType() == 6) {
                        mapping = new BinominalMapping();

                        for(int i = 0; i < domain.length; ++i) {
                            ((NominalMapping)mapping).setMapping(domain[i], i);
                        }
                    } else {
                        Map<Integer, String> map = new HashMap();

                        for(int i = 0; i < domain.length; ++i) {
                            map.put(i, domain[i]);
                        }

                        mapping = new PolynominalMapping(map);
                    }

                    predictionAttr.setMapping((NominalMapping)mapping);
                }
            }

            row = 0;

            for(Iterator var18 = exampleSet.iterator(); var18.hasNext(); ++row) {
                Example example = (Example)var18.next();

                for(int col = 0; col < predictionVecs.length; ++col) {
                    predictionAttr = predictionAttributes[col];
                    Vec predictionVec = predictionVecs[col];
                    example.setValue(predictionAttr, predictionVec.at((long)row));
                }
            }

        }
    }
}

