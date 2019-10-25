package base.operators.operator.clustering.clusterer;

import base.operators.adaption.belt.CompatibilityTools;
import base.operators.adaption.belt.ContextAdapter;
import base.operators.belt.execution.Context;
import base.operators.belt.execution.ExecutionAbortedException;
import base.operators.belt.execution.Workload;
import base.operators.belt.reader.NumericRow;
import base.operators.belt.reader.NumericRowReader;
import base.operators.belt.reader.Readers;
import base.operators.belt.table.BeltConverter;
import base.operators.belt.table.Table;
import base.operators.core.concurrency.ConcurrencyContext;
import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.example.Tools;
import base.operators.example.set.HeaderExampleSet;
import base.operators.operator.*;
import base.operators.operator.clustering.CentroidClusterModel;
import base.operators.operator.clustering.ClusterModel;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeInt;
import base.operators.studio.concurrency.internal.BackgroudOperatorConcurrencyContext;
import base.operators.tools.RandomGenerator;
import base.operators.tools.math.similarity.DistanceMeasure;

import java.util.*;


public class BeltKMeans extends RMAbstractClusterer {
    public static final String PARAMETER_K = "k";
    public static final String PARAMETER_MAX_RUNS = "max_runs";
    public static final String PARAMETER_MAX_OPTIMIZATION_STEPS = "max_optimization_steps";
    private static final int SQUARED_EUCLIDEAN_INDEX = 6;
    private static final OperatorVersion INFINITE_LOOP_BUGFIX_CHANGES_RESULT = new OperatorVersion(9, 0, 1);

    private ConcurrencyContext context = new BackgroudOperatorConcurrencyContext(this);


    public BeltKMeans(OperatorDescription description) { super(description); }

    @Override
    public void setPresetMeasure(DistanceMeasure me) { super.setPresetMeasure(me); }

    @Override
    protected ClusterModel generateInternalClusterModel(ExampleSet exampleSet) throws OperatorException {
        try {
            int k = getParameterAsInt("k");
            int maxOptimizationSteps = getParameterAsInt("max_optimization_steps");
            int maxRuns = getParameterAsInt("max_runs");
            boolean kpp = getParameterAsBoolean("determine_good_start_values");
            boolean addAsLabel = addsLabelAttribute();
            boolean removeUnlabeled = getParameterAsBoolean("remove_unlabeled");


            getProgress().setTotal(maxRuns * maxOptimizationSteps);

            DistanceMeasure measure = getInitializedMeasure(exampleSet);


            Tools.checkAndCreateIds(exampleSet);


            Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, new String[0]);
            if (exampleSet.size() < k) {
                throw new UserError(this, '?', new Object[] { Integer.valueOf(k) });
            }

            //ConcurrencyContext context = Resources.getConcurrencyContext(this);
            Context beltContext = ContextAdapter.adapt(this.context);

            RandomGenerator generator = RandomGenerator.getRandomGenerator(this);

            Attributes attributes = exampleSet.getAttributes();
            ArrayList<String> attributeNames = new ArrayList<String>();
            for (Attribute attribute : attributes) {
                attributeNames.add(attribute.getName());
            }

            Table originalTable = BeltConverter.convert(exampleSet, context).getTable();
            Table table = CompatibilityTools.convertDatetimeToMilliseconds(originalTable.columns(attributeNames),
                    ContextAdapter.adapt(context));

            double minimalIntraClusterDistance = Double.POSITIVE_INFINITY;
            CentroidClusterModel bestModel = null;
            int[] bestAssignments = null;

            HeaderExampleSet headerExampleSet1 = BeltConverter.convertHeader(table);

            for (int run = 0; run < maxRuns; run++) {

                CentroidClusterModel model = new CentroidClusterModel(headerExampleSet1, k, attributeNames, measure, addAsLabel, removeUnlabeled);


                int clusterIndex = 0;

                if (kpp) {
                    List<double[]> initialCentroids = kmeansPlusPlus(k, table, measure, generator, beltContext);

                    int c = 0;
                    for (double[] point : initialCentroids) {
                        model.assignExample(c++, point);
                    }
                } else {
                    int[] initialIndices = new int[k];
                    for (Integer index : generator.nextIntSetWithRange(0, table.height(), k)) {
                        initialIndices[clusterIndex++] = index.intValue();
                    }

                    Table initialCentroids = table.rows(initialIndices, beltContext);
                    initialCentroids.transform().workload(Workload.SMALL).reduceNumeric(() ->
                            Integer.valueOf(0), (dummy, row) ->
                            model.assignExample(row.position(), getAsDoubleArray(row, new double[table.width()])), (dummy1, dummy2) -> {

                    }, beltContext);
                }

                model.finishAssign();


                int[] centroidAssignments = new int[table.height()];
                boolean stable = false;

                for (int step = 0; step < maxOptimizationSteps && !stable; step++) {
                    getProgress().step();

                    double[][] coordinateSumsAndCount = (double[][])table.transform().workload(Workload.HUGE).reduceNumeric(() ->

                            new double[k][table.width() + 1], (coordinateSumsAndCountAcc, row) -> {

                        double[] exampleValues = getAsDoubleArray(row, new double[table.width()]);
                        double nearestDistance = measure.calculateDistance(model.getCentroidCoordinates(0), exampleValues);
                        int nearestIndex = 0;
                        for (int centroidIndex = 1; centroidIndex < k; centroidIndex++) {
                            double distance = measure.calculateDistance(model.getCentroidCoordinates(centroidIndex), exampleValues);

                            if (distance < nearestDistance) {
                                nearestDistance = distance;
                                nearestIndex = centroidIndex;
                            }
                        }

                        centroidAssignments[row.position()] = nearestIndex;
                        for (int d = 0; d < table.width(); d++) {
                            coordinateSumsAndCountAcc[nearestIndex][d] = coordinateSumsAndCountAcc[nearestIndex][d] + exampleValues[d];
                        }
                        coordinateSumsAndCountAcc[nearestIndex][table.width()] = coordinateSumsAndCountAcc[nearestIndex][table.width()] + 1.0D;
                    }, (coordinateSumsAndCountAcc1, coordinateSumsAndCountAcc2) -> {

                        for (int c = 0; c < k; c++) {
                            for (int d = 0; d < table.width() + 1; d++) {
                                coordinateSumsAndCountAcc1[c][d] = coordinateSumsAndCountAcc1[c][d] + coordinateSumsAndCountAcc2[c][d];
                            }
                        }
                    }, beltContext);

                    for (int j = 0; j < k; j++) {
                        model.getCentroid(j).assignMultipleExamples(coordinateSumsAndCount[j]);
                    }

                    stable = model.finishAssign();
                }

                double distanceSum = ((double[])table.transform().workload(Workload.MEDIUM).reduceNumeric(() -> {
                    return new double[1];
                }, (distanceSumAcc, row) -> {
                    double distance = measure.calculateDistance(model.getCentroidCoordinates(centroidAssignments[row.position()]), this.getAsDoubleArray(row, new double[table.width()]));
                    distanceSumAcc[0] += distance * distance;
                }, (distanceSumAcc1, distanceSumAcc2) -> {
                    distanceSumAcc1[0] += distanceSumAcc2[0];
                }, beltContext))[0];
                if (distanceSum < minimalIntraClusterDistance || bestModel == null) {
                    bestModel = model;
                    minimalIntraClusterDistance = distanceSum;
                    bestAssignments = centroidAssignments;
                }

                this.getProgress().setCompleted((run + 1) * maxOptimizationSteps);
            }

            bestModel.setClusterAssignments(bestAssignments, exampleSet);

            if (addsClusterAttribute()) {
                addClusterAssignments(exampleSet, bestAssignments);
            }

            getProgress().complete();

            return bestModel;
        } catch (ExecutionAbortedException e) {
            throw new ProcessStoppedException(this);
        }
    }

    private List<double[]> kmeansPlusPlus(int k, Table table, DistanceMeasure measure, RandomGenerator generator, Context beltContext) throws ProcessStoppedException {
        List<Integer> centerRowIndices = new ArrayList<Integer>();
        List<double[]> centerCoordinates = new ArrayList<double[]>();
        boolean keepInfiniteLoop = getCompatibilityLevel().isAtMost(INFINITE_LOOP_BUGFIX_CHANGES_RESULT);


        centerRowIndices.add(generator.nextIntSetWithRange(0, table.height(), 1).iterator().next());
        NumericRowReader rowReader = Readers.numericRowReader(table);
        rowReader.setPosition(((Integer)centerRowIndices.get(0)).intValue() - 1);
        rowReader.move();
        centerCoordinates.add(getAsDoubleArray(rowReader, new double[table.width()]));


        while (centerRowIndices.size() < k) {
            int centerRowIndex;
            do {
                this.checkForStop();

                double[] minDistances = new double[table.height()];
                double[] sumMinDistance = (double[])table.transform().workload(Workload.HUGE).reduceNumeric(() ->
                        new double[1], (sumMinDistanceAcc, row) -> {
                    double minDistance = -1.0D;
                    double[] point = new double[table.width()];
                    for (double[] center : centerCoordinates) {
                        getAsDoubleArray(row, point);
                        double distance = measure.calculateDistance(point, center);
                        if (!Double.isNaN(distance) && (minDistance < 0.0D || minDistance > distance)) {
                            minDistance = distance;
                        }
                    }
                    minDistances[row.position()] = minDistance * minDistance;
                    sumMinDistanceAcc[0] = sumMinDistanceAcc[0] + minDistances[row.position()];

                }, (sumMinDistanceAcc1, sumMinDistanceAcc2) -> {
                    sumMinDistanceAcc1[0] += sumMinDistanceAcc2[0];
                }, beltContext);

                double draw = generator.nextDoubleInRange(0.0D, sumMinDistance[0]);
                double cumSumMinDistance = 0.0D;
                for (centerRowIndex = 0; centerRowIndex < minDistances.length - 1; centerRowIndex++) {
                    cumSumMinDistance += minDistances[centerRowIndex];
                    if (cumSumMinDistance > draw && (keepInfiniteLoop || !centerRowIndices.contains(Integer.valueOf(centerRowIndex)))) {
                        break;
                    }
                }

            } while (centerRowIndices.contains(Integer.valueOf(centerRowIndex)));
            centerRowIndices.add(Integer.valueOf(centerRowIndex));
            rowReader.setPosition(centerRowIndex - 1);
            rowReader.move();
            centerCoordinates.add(getAsDoubleArray(rowReader, new double[table.width()]));
        }

        return centerCoordinates;
    }

    private double[] getAsDoubleArray(NumericRow row, double[] values) {
        for (int i = 0; i < row.width() && i < values.length; i++) {
            values[i] = row.get(i);
        }
        return values;
    }

    @Override
    public Class<? extends ClusterModel> getClusterModelClass() { return CentroidClusterModel.class; }

    @Override
    protected boolean usesDistanceMeasures() { return true; }

    @Override
    protected boolean usesPresetMeasure() { return true; }

    @Override
    protected boolean handlesInfiniteValues() { return false; }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeInt("k", "The number of clusters which should be detected.", 2, 2147483647, 5, false));

        types.add(new ParameterTypeInt("max_runs", "The maximal number of runs of k-Means with random initialization that are performed.", 1, 2147483647, 10, false));

        ParameterTypeBoolean parameterTypeBoolean = new ParameterTypeBoolean("determine_good_start_values", "Determine the first k centroids using the K-Means++ heuristic described in \"k-means++: The Advantages of Careful Seeding\" by David Arthur and Sergei Vassilvitskii 2007", true);
        parameterTypeBoolean.setExpert(false);
        types.add(parameterTypeBoolean);

        types.addAll(getMeasureParameterTypes());

        types.add(new ParameterTypeInt("max_optimization_steps", "The maximal number of iterations performed for one run of k-Means.", 1, 2147483647, 100, false));

        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        return types;
    }

    @Override
    protected Map<String, Object> getMeasureParametersDefaults() {
        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put("measure_types", Integer.valueOf(3));
        defaults.put("divergence", Integer.valueOf(6));
        return defaults;
    }

    @Override
    protected boolean affectedByLabelFix() { return false; }

    @Override
    public OperatorVersion[] getIncompatibleVersionChanges() {
        OperatorVersion[] old = super.getIncompatibleVersionChanges();
        OperatorVersion[] versions = (OperatorVersion[])Arrays.copyOf(old, old.length + 1);
        versions[old.length] = INFINITE_LOOP_BUGFIX_CHANGES_RESULT;
        return versions;
    }
}

