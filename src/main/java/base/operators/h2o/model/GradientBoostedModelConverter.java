//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package base.operators.h2o.model;

import com.google.common.collect.ImmutableMap;
import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.HeaderExampleSet;
import base.operators.example.table.BinominalAttribute;
import base.operators.h2o.model.custom.CustomisationUtils;
import base.operators.h2o.model.custom.gbm.CompressedTree;
import base.operators.h2o.model.custom.gbm.GBMModel;
import base.operators.h2o.model.custom.gbm.IcedBitSet;
import base.operators.h2o.model.custom.gbm.TreeVisitor;
import base.operators.operator.Model;
import base.operators.operator.OperatorException;
import base.operators.operator.learner.tree.AbstractSplitCondition;
import base.operators.operator.learner.tree.SplitCondition;
import base.operators.operator.learner.tree.Tree;
import base.operators.operator.learner.tree.TreeModel;
import hex.Distribution.Family;
import hex.Model.Parameters.FoldAssignmentScheme;
import hex.tree.SharedTreeModel.SharedTreeParameters.HistogramType;
import hex.tree.gbm.GBMModel.GBMOutput;
import hex.tree.gbm.GBMModel.GBMParameters;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import water.Iced;
import water.Key;
import water.Keyed;
import water.Lockable;

public class GradientBoostedModelConverter {
    private static DecimalFormat threeDecimalDigits = new DecimalFormat("#0.000");
    private static final int MAX_SPLIT_LABEL_LENGTH = 20;
    // public static final Map<Class<?>, Class<?>> CLASS_CONVERSIONS = ImmutableMap.builder().put(Class.forName("base.operators.h2o.model.custom.gbm.GBMModel.GBMParameters"), Class.forName("base.operators.h2o.model.custom.gbm.GBMModel.GBMParameters")).put(Class.forName("base.operators.h2o.model.custom.gbm.GBMModel.GBMParameters"), Class.forName("base.operators.h2o.model.custom.gbm.GBMModel.GBMParameters")).build();
    // public static final Map<Class<? extends Enum<?>>, Class<? extends Enum<?>>> ENUM_CONVERSIONS = ImmutableMap.builder().put(Class.forName("hex.Model.Parameters.FoldAssignmentScheme"), Class.forName("base.operators.h2o.model.custom.Model.Parameters.FoldAssignmentScheme").put(Class.forName("hex.tree.SharedTreeModel.SharedTreeParameters.HistogramType"), (Class<?>)base.operators.h2o.model.custom.gbm.SharedTreeModel.SharedTreeParameters.HistogramType.class).put((Class<?>)Family.class, (Class<?>)base.operators.h2o.model.custom.Distribution.Family.class).build();
    public static final Set<Class<?>> IGNORED_H2O_SUPERCLASSES = Collections.unmodifiableSet(new HashSet(Arrays.asList(Keyed.class, Lockable.class, Iced.class)));

    public static final Map<Class<?>, Class<?>> CLASS_CONVERSIONS1 = ImmutableMap.<Class<?>, Class<?>>builder()
            .put(hex.tree.gbm.GBMModel.GBMParameters.class, base.operators.h2o.model.custom.gbm.GBMModel.GBMParameters.class)
            .put(hex.tree.gbm.GBMModel.GBMOutput.class, base.operators.h2o.model.custom.gbm.GBMModel.GBMOutput.class)
            .build();
    public static final Map<Class<? extends Enum<?>>, Class<? extends Enum<?>>> ENUM_CONVERSIONS = ImmutableMap.<Class<? extends Enum<?>>, Class<? extends Enum<?>>>builder()
            .put(hex.Model.Parameters.FoldAssignmentScheme.class, base.operators.h2o.model.custom.Model.Parameters.FoldAssignmentScheme.class)
            .put(hex.tree.SharedTreeModel.SharedTreeParameters.HistogramType.class, base.operators.h2o.model.custom.gbm.SharedTreeModel.SharedTreeParameters.HistogramType.class)
            .put(hex.Distribution.Family.class, base.operators.h2o.model.custom.Distribution.Family.class)
            .build();
    public GradientBoostedModelConverter() {
    }

    public static GBMModel convertGBMModel(hex.tree.gbm.GBMModel h2oGBMModel) throws OperatorException {
        GBMModel model = null;

        try {
            model = (GBMModel)CustomisationUtils.convertObject(h2oGBMModel, GBMModel.class, CLASS_CONVERSIONS1, ENUM_CONVERSIONS, IGNORED_H2O_SUPERCLASSES);
            model._myDefaultThreshold = h2oGBMModel.defaultThreshold();
            ((GBMModel.GBMOutput)model._output)._myStringRepresentation = ((GBMOutput)h2oGBMModel._output).toString();
            ((GBMModel.GBMOutput)model._output)._trees = convertTree(h2oGBMModel);
            return model;
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException var3) {
            throw new OperatorException("Could not convert GBM model!", var3);
        }
    }

    private static CompressedTree[][] convertTree(hex.tree.gbm.GBMModel h2oGBMModel) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Key<hex.tree.CompressedTree>[][] treeKeys = ((GBMOutput)h2oGBMModel._output)._treeKeys;
        CompressedTree[][] result = new CompressedTree[treeKeys.length][];

        for(int treeIdx = 0; treeIdx < ((GBMOutput)h2oGBMModel._output)._treeKeys.length; ++treeIdx) {
            int nclass = treeKeys[treeIdx].length;
            result[treeIdx] = new CompressedTree[nclass];

            for(int classIdx = 0; classIdx < nclass; ++classIdx) {
                Key<hex.tree.CompressedTree> treeKey = treeKeys[treeIdx][classIdx];
                if (treeKey != null) {
                    result[treeIdx][classIdx] = (CompressedTree)CustomisationUtils.convertObject(treeKey.get(), CompressedTree.class, Collections.emptyMap(), Collections.emptyMap(), IGNORED_H2O_SUPERCLASSES);
                }
            }
        }

        return result;
    }

    public static GradientBoostedModel convert(ExampleSet trainingExampleSet, hex.tree.gbm.GBMModel gbmModel) throws OperatorException {
        String modelString = gbmModel.toString();
        H2ONativeModelObject h2oNativeModelObject = new H2ONativeModelObject(gbmModel);
        GBMModel customGBMModel = convertGBMModel(gbmModel);
        return new GradientBoostedModel(trainingExampleSet, h2oNativeModelObject, modelString, gbmModel._warnings, customGBMModel);
    }

    public static List<Model> convertModels(HeaderExampleSet trainingHeader, GBMModel gbmModel) {
        List<Model> models = null;
        models = new ArrayList(((GBMModel.GBMOutput)gbmModel._output)._ntrees);

        for(int treeIndex = 0; treeIndex < ((GBMModel.GBMOutput)gbmModel._output)._ntrees; ++treeIndex) {
            for(int labelIndex = 0; labelIndex < ((GBMModel.GBMOutput)gbmModel._output)._trees[treeIndex].length; ++labelIndex) {
                if (((GBMModel.GBMOutput)gbmModel._output)._trees[treeIndex][labelIndex] != null) {
                    CompressedTree h2oTree = ((GBMModel.GBMOutput)gbmModel._output)._trees[treeIndex][labelIndex];
                    GradientBoostedModelConverter.RMTreeVisitor visitor = new GradientBoostedModelConverter.RMTreeVisitor(h2oTree, trainingHeader, ((GBMModel.GBMOutput)gbmModel._output)._names);

                    try {
                        visitor.visit();
                    } catch (Exception var8) {
                    }

                    models.add(new TreeModel(trainingHeader, visitor.getRMTree()));
                }
            }
        }

        return models;
    }

    public static List<String> convertModelNames(HeaderExampleSet trainingHeader, GBMModel gbmModel) {
        List<String> modelNames = null;
        int treeCount = ((GBMModel.GBMOutput)gbmModel._output)._ntrees;
        modelNames = new ArrayList(treeCount);

        for(int treeIndex = 0; treeIndex < treeCount; ++treeIndex) {
            for(int labelIndex = 0; labelIndex < ((GBMModel.GBMOutput)gbmModel._output)._trees[treeIndex].length; ++labelIndex) {
                if (((GBMModel.GBMOutput)gbmModel._output)._trees[treeIndex][labelIndex] != null) {
                    if (trainingHeader.getAttributes().getLabel().isNominal() && trainingHeader.getAttributes().getLabel().getMapping().getValues().size() > 2) {
                        modelNames.add("Tree " + (treeIndex + 1) + " (" + (String)trainingHeader.getAttributes().getLabel().getMapping().getValues().get(labelIndex) + ")");
                    } else {
                        modelNames.add("Tree " + (treeIndex + 1));
                    }
                }
            }
        }

        return modelNames;
    }

    private static class H2OSplit {
        private final Attribute attribute;
        private final double value;
        private final int[] values;
        private final GradientBoostedModelConverter.H2OSplit.Relation relation;

        private H2OSplit(Attribute attribute, GradientBoostedModelConverter.H2OSplit.Relation relation, double value) {
            this.attribute = attribute;
            this.value = value;
            this.values = null;
            this.relation = relation;
        }

        private H2OSplit(Attribute attribute, GradientBoostedModelConverter.H2OSplit.Relation relation, int[] values) {
            this.attribute = attribute;
            this.value = -1.0D;
            this.values = values;
            this.relation = relation;
        }

        private SplitCondition left() {
            return new GradientBoostedModelConverter.H2OSplit.AbstractH2OSplitCondition(this.attribute) {
                @Override
                public String getRelation() {
                    return H2OSplit.this.relation.leftOp;
                }
            };
        }

        private SplitCondition right() {
            return new GradientBoostedModelConverter.H2OSplit.AbstractH2OSplitCondition(this.attribute) {
                @Override
                public String getRelation() {
                    return H2OSplit.this.relation.rightOp;
                }
            };
        }

        private abstract class AbstractH2OSplitCondition extends AbstractSplitCondition {
            public AbstractH2OSplitCondition(Attribute attribute) {
                super(attribute.getName());
            }

            @Override
            public String getValueString() {
                if (H2OSplit.this.values == null) {
                    return H2OSplit.this.attribute.isNominal() ? (String)H2OSplit.this.attribute.getMapping().getValues().get((int)H2OSplit.this.value) : GradientBoostedModelConverter.threeDecimalDigits.format(H2OSplit.this.value);
                } else if (!H2OSplit.this.attribute.isNominal()) {
                    throw new IllegalStateException("Nominal split on numerical attribute");
                } else {
                    StringBuilder sb = new StringBuilder("{");

                    for(int i = 0; i < H2OSplit.this.values.length; ++i) {
                        sb.append((String)H2OSplit.this.attribute.getMapping().getValues().get(H2OSplit.this.values[i]));
                        if (i < H2OSplit.this.values.length - 1) {
                            sb.append(",");
                            if (sb.length() > 20) {
                                sb.append("... (" + (H2OSplit.this.values.length - i - 1) + " more)");
                                break;
                            }
                        }
                    }

                    sb.append("}");
                    return sb.toString();
                }
            }

            @Override
            public boolean test(Example example) {
                throw new UnsupportedOperationException("H2O split conditions are for display purposes only");
            }
        }

        private static enum Relation {
            LESS("<", ">="),
            NOT_EQUAL("!=", "="),
            IN("in", "not in"),
            NOT_IN("not in", "in");

            private final String leftOp;
            private final String rightOp;

            private Relation(String leftOp, String rightOp) {
                this.leftOp = leftOp;
                this.rightOp = rightOp;
            }
        }
    }

    private static class RMTreeVisitor extends TreeVisitor<Exception> {
        private Tree root = null;
        private Map<Tree, Tree> rootMap = new HashMap();
        private Map<Tree, GradientBoostedModelConverter.H2OSplit> splitMap = new HashMap();
        private Tree current;
        private HeaderExampleSet trainingHeader;
        private String[] names;

        public RMTreeVisitor(CompressedTree ct, HeaderExampleSet trainingHeader, String[] names) {
            super(ct);
            this.trainingHeader = trainingHeader;
            this.names = names;
        }

        public Tree getRMTree() {
            return this.root;
        }

        @Override
        protected void pre(int col, float fcmp, IcedBitSet gcmp, int equal) throws Exception {
            Tree node = new Tree(this.trainingHeader);
            Attribute attribute = this.trainingHeader.getAttributes().get(this.names[col]);
            if (equal == 1) {
                this.splitMap.put(node, new GradientBoostedModelConverter.H2OSplit(attribute, GradientBoostedModelConverter.H2OSplit.Relation.NOT_EQUAL, (double)fcmp));
            } else {
                int[] values;
                if (equal == 0) {
                    if (attribute.isNominal()) {
                        if (attribute instanceof BinominalAttribute) {
                            this.splitMap.put(node, new GradientBoostedModelConverter.H2OSplit(attribute, GradientBoostedModelConverter.H2OSplit.Relation.NOT_EQUAL, 1.0D));
                        } else {
                            values = IntStream.rangeClosed(0, (int)Math.ceil((double)(fcmp - 1.0F))).toArray();
                            this.splitMap.put(node, new GradientBoostedModelConverter.H2OSplit(attribute, GradientBoostedModelConverter.H2OSplit.Relation.IN, values));
                        }
                    } else {
                        this.splitMap.put(node, new GradientBoostedModelConverter.H2OSplit(attribute, GradientBoostedModelConverter.H2OSplit.Relation.LESS, (double)fcmp));
                    }
                } else {
                    values = IntStream.range(0, gcmp.size()).filter((i) -> {
                        return gcmp.contains(i);
                    }).toArray();
                    this.splitMap.put(node, new GradientBoostedModelConverter.H2OSplit(attribute, GradientBoostedModelConverter.H2OSplit.Relation.NOT_IN, values));
                }
            }

            if (this.root == null) {
                this.root = node;
            } else {
                this.addChild(this.current, node);
                this.rootMap.put(node, this.current);
            }

            this.current = node;
        }

        @Override
        protected void post(int col, float fcmp, int equal) throws Exception {
            this.current = (Tree)this.rootMap.get(this.current);
        }

        @Override
        protected void leaf(float pred) throws Exception {
            Tree leaf = new Tree(this.trainingHeader);
            leaf.setLeaf(GradientBoostedModelConverter.threeDecimalDigits.format((double)pred));
            this.addChild(this.current, leaf);
        }

        private void addChild(Tree parent, Tree child) {
            if (parent.getNumberOfChildren() == 0) {
                parent.addChild(child, ((GradientBoostedModelConverter.H2OSplit)this.splitMap.get(this.current)).left());
            } else {
                if (parent.getNumberOfChildren() != 1) {
                    throw new IllegalStateException("Tree is not binary");
                }

                parent.addChild(child, ((GradientBoostedModelConverter.H2OSplit)this.splitMap.get(this.current)).right());
            }

        }
    }
}