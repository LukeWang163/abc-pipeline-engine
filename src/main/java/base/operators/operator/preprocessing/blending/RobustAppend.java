package base.operators.operator.preprocessing.blending;

import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.MissingIOObjectException;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.InputPortExtender;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.MetaDataInfo;
import base.operators.operator.ports.metadata.Precondition;
import base.operators.operator.preprocessing.join.ExampleSetMerge;
import base.operators.operator.tools.AttributeSubsetSelector;
import base.operators.studio.internal.ProcessStoppedRuntimeException;
import base.operators.tools.Ontology;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.tools.parameter.internal.DataManagementParameterHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

public class RobustAppend extends Operator {
    private final InputPortExtender inputExtender = new InputPortExtender("example set", this.getInputPorts()) {
        @Override
        protected Precondition makePrecondition(InputPort port) {
            return new ExampleSetPrecondition(port) {
                {
                    this.setOptional(true);
                }

                @Override
                public void makeAdditionalChecks(ExampleSetMetaData emd) {
                    Iterator var2 = RobustAppend.this.inputExtender.getMetaData(true).iterator();

                    while(var2.hasNext()) {
                        MetaData metaData = (MetaData)var2.next();
                        if (metaData instanceof ExampleSetMetaData) {
                            MetaDataInfo result = emd.equalHeader((ExampleSetMetaData)metaData);
                            if (result != MetaDataInfo.YES) {
                                RobustAppend.this.addError(new SimpleProcessSetupError(result == MetaDataInfo.NO ? Severity.ERROR : Severity.WARNING, RobustAppend.this.getPortOwner(), "exampleset.sets_incompatible", new Object[0]));
                                break;
                            }
                        }
                    }

                }
            };
        }
    };
    private final OutputPort mergedOutput = (OutputPort)this.getOutputPorts().createPort("merged set");

    public RobustAppend(OperatorDescription description) {
        super(description);
        this.inputExtender.start();
        this.getTransformer().addRule(this.inputExtender.makeFlatteningPassThroughRule(this.mergedOutput));
        this.getTransformer().addRule(() -> {
            List<MetaData> metaDatas = this.inputExtender.getMetaData(true);
            List<ExampleSetMetaData> emds = new ArrayList(metaDatas.size());
            Iterator var3 = metaDatas.iterator();

            while(var3.hasNext()) {
                MetaData metaData = (MetaData)var3.next();
                if (metaData instanceof ExampleSetMetaData) {
                    emds.add((ExampleSetMetaData)metaData);
                }
            }

            if (emds.size() > 0) {
                ExampleSetMetaData resultEMD = ((ExampleSetMetaData)emds.get(0)).clone();

                for(int i = 1; i < emds.size(); ++i) {
                    ExampleSetMetaData mergerEMD = (ExampleSetMetaData)emds.get(i);
                    resultEMD.getNumberOfExamples().add(mergerEMD.getNumberOfExamples());
                    Iterator var6 = resultEMD.getAllAttributes().iterator();

                    while(var6.hasNext()) {
                        AttributeMetaData amd = (AttributeMetaData)var6.next();
                        String name = amd.getName();
                        AttributeMetaData mergingAMD = mergerEMD.getAttributeByName(name);
                        if (mergingAMD != null) {
                            if (amd.isNominal()) {
                                amd.getValueSet().addAll(mergingAMD.getValueSet());
                            } else {
                                amd.getValueRange().union(mergingAMD.getValueRange());
                            }

                            amd.getValueSetRelation().merge(mergingAMD.getValueSetRelation());
                            amd.getNumberOfMissingValues().add(mergingAMD.getNumberOfMissingValues());
                        }
                    }
                }

                this.mergedOutput.deliverMD(resultEMD);
            }

        });
    }

    @Override
    public void doWork() throws OperatorException {
        List<ExampleSet> allExampleSets = this.inputExtender.getData(ExampleSet.class, true);
        this.mergedOutput.deliver(this.merge(allExampleSets));
    }

    public ExampleSet merge(final List<ExampleSet> allExampleSets) throws OperatorException {
        if (allExampleSets.size() == 0) {
            throw new MissingIOObjectException(ExampleSet.class);
        } else {
            this.checkForCompatibility(allExampleSets);
            ExampleSet firstSet = (ExampleSet)allExampleSets.get(0);
            List<Attribute> newAttributeList = new ArrayList();
            Map<Attribute, String> specialAttributesMap = new LinkedHashMap();
            Iterator a = firstSet.getAttributes().allAttributeRoles();

            Iterator var9;
            ExampleSet exampleSet;
            Attribute oldAttribute;
            while(a.hasNext()) {
                AttributeRole role = (AttributeRole)a.next();
                oldAttribute = role.getAttribute();
                int newType;
                if (oldAttribute.isNominal()) {
                    newType = 1;
                } else if (oldAttribute.isNumerical()) {
                    newType = 4;
                } else if (oldAttribute.isDateTime()) {
                    newType = 9;
                } else {
                    var9 = allExampleSets.iterator();

                    while(var9.hasNext()) {
                        exampleSet = (ExampleSet)var9.next();
                        oldAttribute = exampleSet.getAttributes().get(oldAttribute.getName());
                        if (oldAttribute.getValueType() != oldAttribute.getValueType()) {
                            this.throwIncompatible(oldAttribute, oldAttribute);
                        }
                    }

                    newType = oldAttribute.getValueType();
                }

                Attribute newAttribute = AttributeFactory.createAttribute(oldAttribute.getName(), newType, oldAttribute.getBlockType());
                newAttributeList.add(newAttribute);
                if (role.isSpecial()) {
                    specialAttributesMap.put(newAttribute, role.getSpecialName());
                }
            }

            int totalSize = 0;

            Iterator var15;
            ExampleSet set;
            for(var15 = allExampleSets.iterator(); var15.hasNext(); totalSize += set.size()) {
                set = (ExampleSet)var15.next();
            }

            var15 = newAttributeList.iterator();

            label94:
            while(true) {
                Attribute attribute;
                do {
                    if (!var15.hasNext()) {
                        ExampleSetBuilder builder = ExampleSets.from(newAttributeList);
                        builder.withBlankSize(totalSize);
                        builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(this));
                        int[] sizes = new int[allExampleSets.size()];
                        int i = 0;

                        ExampleSet resultSet;
                        for(Iterator var22 = allExampleSets.iterator(); var22.hasNext(); ++i) {
                            resultSet = (ExampleSet)var22.next();
                            sizes[i] = resultSet.size();
                        }

                        final int[] sizesSums = new int[sizes.length];
                        sizesSums[0] = sizes[0];

                        for(int j = 1; j < sizes.length; ++j) {
                            sizesSums[j] = sizesSums[j - 1] + sizes[j];
                        }

                        Iterator var27 = newAttributeList.iterator();

                        while(var27.hasNext()) {
                            final Attribute newAttribute = (Attribute)var27.next();
                            builder.withColumnFiller(newAttribute, new IntToDoubleFunction() {
                                private final String attributeName = newAttribute.getName();
                                private ExampleSet oldSet = null;
                                private Attribute oldAttribute = null;
                                private int start = 0;
                                private int end = 0;
                                private int oldExampleSetIndex = -1;

                                @Override
                                public synchronized double applyAsDouble(int i) {
                                    if (i < this.start || i >= this.end) {
                                        try {
                                            RobustAppend.this.checkForStop();
                                        } catch (ProcessStoppedException var4) {
                                            throw new ProcessStoppedRuntimeException();
                                        }

                                        int startIndex = 0;
                                        this.end = 0;
                                        if (this.oldExampleSetIndex > -1 && i >= sizesSums[this.oldExampleSetIndex]) {
                                            startIndex = this.oldExampleSetIndex + 1;
                                            this.end = sizesSums[this.oldExampleSetIndex];
                                        }

                                        for(int j = startIndex; j < sizesSums.length; ++j) {
                                            this.start = this.end;
                                            this.end = sizesSums[j];
                                            if (this.end > i) {
                                                this.oldExampleSetIndex = j;
                                                this.oldSet = (ExampleSet)allExampleSets.get(j);
                                                this.oldAttribute = this.oldSet.getAttributes().get(this.attributeName);
                                                break;
                                            }
                                        }
                                    }

                                    double oldValue = this.oldSet.getExample(i - this.start).getValue(this.oldAttribute);
                                    if (Double.isNaN(oldValue)) {
                                        return 0.0D / 0.0;
                                    } else {
                                        return this.oldAttribute.isNominal() ? (double)newAttribute.getMapping().mapString(this.oldAttribute.getMapping().mapIndex((int)oldValue)) : oldValue;
                                    }
                                }
                            });
                        }

                        resultSet = builder.withRoles(specialAttributesMap).build();
                        resultSet.getAnnotations().addAll(firstSet.getAnnotations());
                        return resultSet;
                    }

                    attribute = (Attribute)var15.next();
                } while(!attribute.isNominal());

                var9 = allExampleSets.iterator();

                while(true) {
                    do {
                        do {
                            if (!var9.hasNext()) {
                                continue label94;
                            }

                            exampleSet = (ExampleSet)var9.next();
                            oldAttribute = exampleSet.getAttributes().get(attribute.getName());
                        } while(oldAttribute == null);
                    } while(!oldAttribute.isNominal());

                    Iterator var12 = oldAttribute.getMapping().getValues().iterator();

                    while(var12.hasNext()) {
                        String value = (String)var12.next();
                        attribute.getMapping().mapString(value);
                    }
                }
            }
        }
    }

    private void throwIncompatible(Attribute oldAttribute, Attribute otherAttribute) throws UserError {
        throw new UserError(this, 925, new Object[]{"Attribute '" + oldAttribute.getName() + "' has incompatible types (" + Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(oldAttribute.getValueType()) + " and " + Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(otherAttribute.getValueType()) + ") in two input sets."});
    }

    private void checkForCompatibility(List<ExampleSet> allExampleSets) throws OperatorException {
        ExampleSet first = (ExampleSet)allExampleSets.get(0);
        Iterator var3 = allExampleSets.iterator();

        while(var3.hasNext()) {
            ExampleSet exampleSet = (ExampleSet)var3.next();
            this.checkForCompatibility(first, exampleSet);
        }

    }

    private void checkForCompatibility(ExampleSet first, ExampleSet second) throws OperatorException {
        if (first.getAttributes().allSize() != second.getAttributes().allSize()) {
            throw new UserError(this, 925, new Object[]{"numbers of attributes are different"});
        } else {
            Iterator firstIterator = first.getAttributes().allAttributes();

            Attribute firstAttribute;
            Attribute secondAttribute;
            do {
                if (!firstIterator.hasNext()) {
                    return;
                }

                firstAttribute = (Attribute)firstIterator.next();
                secondAttribute = second.getAttributes().get(firstAttribute.getName());
            } while(secondAttribute != null);

            throw new UserError(this, 925, new Object[]{"Attribute with name '" + firstAttribute.getName() + "' is not part of second example set."});
        }
    }

    @Override
    public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
        return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator((InputPort)this.getInputPorts().getPortByIndex(0), ExampleSetMerge.class, (AttributeSubsetSelector)null);
    }
}
