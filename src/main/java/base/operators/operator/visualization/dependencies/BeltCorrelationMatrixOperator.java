package base.operators.operator.visualization.dependencies;

import base.operators.adaption.belt.CompatibilityTools;
import base.operators.adaption.belt.ContextAdapter;
import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnTypes;
import base.operators.belt.column.Columns;
import base.operators.belt.execution.Context;
import base.operators.belt.execution.ExecutionAbortedException;
import base.operators.belt.execution.Workload;
import base.operators.belt.reader.NumericRow;
import base.operators.belt.table.BeltConverter;
import base.operators.belt.table.Table;
import base.operators.belt.transform.RowTransformer;
import base.operators.core.concurrency.ConcurrencyContext;
import base.operators.example.AttributeWeights;
import base.operators.example.ExampleSet;
import base.operators.operator.*;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.GenerateNewMDRule;
import base.operators.operator.tools.AttributeSubsetSelector;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.studio.concurrency.internal.BackgroudOperatorConcurrencyContext;
import base.operators.tools.ProcessTools;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class BeltCorrelationMatrixOperator
        extends Operator
{
    private static final String PARAMETER_NORMALIZE_WEIGHTS = "normalize_weights";
    private static final String PARAMETER_SQUARED_CORRELATION = "squared_correlation";
    private static final OperatorVersion VERSION_INCORRECT_PASSTHROUGH = new OperatorVersion(8, 2, 0);
    private static final OperatorVersion LAST_LEGACY_CORE_COMPATIBLE = new OperatorVersion(8, 2, 1);

    private static final int ATTRIBUTES_THRESHOLD_SMALL = 15;

    private static final int ATTRIBUTES_THRESHOLD_MEDIUM = 150;

    private static final int ATTRIBUTES_THRESHOLD_LARGE = 1500;

    private static class MeanAccumulator
    {
        private final double[] sumsX;

        private final double[] sumsY;

        private final int[] counts;

        MeanAccumulator(int nColumns) {
            int nPairs = nColumns * (nColumns - 1) / 2;
            this.sumsX = new double[nPairs];
            this.sumsY = new double[nPairs];
            this.counts = new int[nPairs];
        }
    }

    private static void addRow(MeanAccumulator means, NumericRow row) {
        int k = 0;
        for (int i = 0; i < row.width() - 1; i++) {
            double x = row.get(i);
            for (int j = i + 1; j < row.width(); j++) {
                double y = row.get(j);
                if (!Double.isNaN(x) && !Double.isNaN(y)) {
                    means.sumsX[k] = means.sumsX[k] + x;
                    means.sumsY[k] = means.sumsY[k] + y;
                    means.counts[k] = means.counts[k] + 1;
                }
                k++;
            }
        }
    }

    private static void combineAccumulators(MeanAccumulator accumulator, MeanAccumulator other) {
        for (int i = 0; i < accumulator.sumsX.length; i++) {
            accumulator.sumsX[i] = accumulator.sumsX[i] + other.sumsX[i];
            accumulator.sumsY[i] = accumulator.sumsY[i] + other.sumsY[i];
            accumulator.counts[i] = accumulator.counts[i] + other.counts[i];
        }
    }

    private static class CorrelationAccumulator
    {
        private final double[] sumXY;

        private final double[] sumXSquared;

        private final double[] sumYSquared;

        CorrelationAccumulator(int nColumns) {
            int nPairs = nColumns * (nColumns - 1) / 2;
            this.sumXY = new double[nPairs];
            this.sumXSquared = new double[nPairs];
            this.sumYSquared = new double[nPairs];
        }
    }

    private static void addRow(double[] meansX, double[] meansY, CorrelationAccumulator sumsOfSquares, NumericRow row) {
        int k = 0;
        for (int i = 0; i < row.width() - 1; i++) {
            double x = row.get(i);
            for (int j = i + 1; j < row.width(); j++) {
                double y = row.get(j);
                double deviationX = x - meansX[k];
                double deviationY = y - meansY[k];
                if (!Double.isNaN(x) && !Double.isNaN(y)) {
                    sumsOfSquares.sumXY[k] = sumsOfSquares.sumXY[k] + deviationX * deviationY;
                    sumsOfSquares.sumXSquared[k] = sumsOfSquares.sumXSquared[k] + deviationX * deviationX;
                    sumsOfSquares.sumYSquared[k] = sumsOfSquares.sumYSquared[k] + deviationY * deviationY;
                }
                k++;
            }
        }
    }

    private static void combineAccumulators(CorrelationAccumulator accumulator, CorrelationAccumulator other) {
        for (int i = 0; i < accumulator.sumXY.length; i++) {
            accumulator.sumXY[i] = accumulator.sumXY[i] + other.sumXY[i];
            accumulator.sumXSquared[i] = accumulator.sumXSquared[i] + other.sumXSquared[i];
            accumulator.sumYSquared[i] = accumulator.sumYSquared[i] + other.sumYSquared[i];
        }
    }

    private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
    private OutputPort exampleSetOutput = (OutputPort)getOutputPorts().createPort("example set");
    private OutputPort matrixOutput = (OutputPort)getOutputPorts().createPort("matrix");
    private OutputPort weightsOutput = (OutputPort)getOutputPorts().createPort("weights");

    private ConcurrencyContext context = new BackgroudOperatorConcurrencyContext(this);

    private AttributeSubsetSelector subsetSelector = new AttributeSubsetSelector(this, this.exampleSetInput);

    public BeltCorrelationMatrixOperator(OperatorDescription description) {
        super(description);
        getTransformer().addPassThroughRule(this.exampleSetInput, this.exampleSetOutput);
        getTransformer().addRule(new GenerateNewMDRule(this.matrixOutput, NumericalMatrix.class));
        getTransformer().addRule(new GenerateNewMDRule(this.weightsOutput, AttributeWeights.class));
    }

    @Override
    public void doWork() throws OperatorException {
        NumericalMatrix matrix;
        ExampleSet inputSet = (ExampleSet)this.exampleSetInput.getData(ExampleSet.class);
        ExampleSet inputSubset = this.subsetSelector.getSubset(inputSet, false);
        Table table = BeltConverter.convert(inputSubset, this.context).getTable();

        getProgress().setCheckForStop(false);
        getProgress().setTotal(100);

        boolean legacyMode = getCompatibilityLevel().isAtMost(LAST_LEGACY_CORE_COMPATIBLE);
        if (legacyMode) {

            Context legacyContext = ContextAdapter.adapt(this.context);
            table = CompatibilityTools.convertDatetimeToMilliseconds(table, legacyContext);
        }

        boolean squared = getParameterAsBoolean("squared_correlation");

        int nColumns = table.width();
        boolean[] labelValidities = new boolean[nColumns];
        List<String> validLabels = new ArrayList<String>(nColumns);

        List<String> labels = table.labels();
        for (int i = 0; i < nColumns; i++) {
            Column column = table.column(i);
            boolean valid = (column.type().category() == Column.Category.NUMERIC || ColumnTypes.TIME.equals(column.type()));

            if (!valid && Columns.isBicategorical(column)) {
                valid = (!legacyMode || column.getDictionary(Object.class).isBoolean());
            }
            labelValidities[i] = valid;
            if (valid) {
                validLabels.add(labels.get(i));
            }
        }
        checkForStop();

        String[] labelArray = (String[])labels.toArray(new String[0]);
        if (validLabels.size() < 2) {
            matrix = identityMatrix(labelArray);
            matrix.setUseless(true);
        } else {
            try {
                matrix = calculateCorrelation(table, labelArray, labelValidities, validLabels, squared);
            } catch (ExecutionAbortedException e) {

                throw new ProcessStoppedException(this);
            }
        }

        AttributeWeights weights = calculateWeights(labelArray, labelValidities, matrix);

        getProgress().complete();

        if (getCompatibilityLevel().isAbove(VERSION_INCORRECT_PASSTHROUGH)) {
            this.exampleSetOutput.deliver(inputSet);
        } else {
            this.exampleSetOutput.deliver(inputSubset);
        }

        this.weightsOutput.deliver(weights);
        this.matrixOutput.deliver(matrix);
    }

    private NumericalMatrix identityMatrix(String[] attributes) {
        NumericalMatrix matrix = new NumericalMatrix("Correlation", attributes, true);
        for (int m = 0; m < attributes.length; m++) {
            for (int n = m; n < attributes.length; n++) {
                matrix.setValue(m, n, (m == n) ? 1.0D : 0.0D / 0.0);
            }
        }
        matrix.setTheoreticalMin(-1.0D);
        matrix.setTheoreticalMax(1.0D);
        return matrix;
    }

    private NumericalMatrix calculateCorrelation(Table table, String[] attributes, boolean[] attributeMask, List<String> validAttributes, boolean squared) throws ExecutionAbortedException, ProcessStoppedException {
        Context context = ContextAdapter.adapt(this.context);

        RowTransformer transformer = createTransformer(table, validAttributes);

        MeanAccumulator sums = (MeanAccumulator)transformer.callback(i -> silentProgress(i, 49.0D, 0.0D)).reduceNumeric(() ->
                new MeanAccumulator(validAttributes.size()), BeltCorrelationMatrixOperator::addRow, BeltCorrelationMatrixOperator::combineAccumulators, context);

        double[] meansX = sums.sumsX;
        double[] meansY = sums.sumsY;
        for (int i = 0; i < meansX.length; i++) {
            int count = sums.counts[i];
            meansX[i] = meansX[i] / count;
            meansY[i] = meansY[i] / count;
        }

        CorrelationAccumulator sumOfSquares = (CorrelationAccumulator)transformer.callback(i -> silentProgress(i, 49.0D, 49.0D)).reduceNumeric(() ->
                new CorrelationAccumulator(validAttributes.size()), (accumulator, row) ->
                addRow(meansX, meansY, accumulator, row), BeltCorrelationMatrixOperator::combineAccumulators, context);

        double[] correlations = sumOfSquares.sumXY;
        for (int i = 0; i < correlations.length; i++) {
            double sqrt = Math.sqrt(sumOfSquares.sumXSquared[i] * sumOfSquares.sumYSquared[i]);
            double correlation = sumOfSquares.sumXY[i] / sqrt;
            if (squared) {
                correlation *= correlation;
            }
            correlations[i] = correlation;
        }

        NumericalMatrix matrix = identityMatrix(attributes);
        int k = 0;
        for (int i = 0; i < attributes.length - 1; i++) {
            for (int j = i + 1; j < attributes.length; j++) {
                if (attributeMask[i] && attributeMask[j]) {
                    matrix.setValue(i, j, correlations[k]);
                    k++;
                }
            }
        }
        getProgress().setCompleted(99);
        checkForStop();

        return matrix;
    }

    private void silentProgress(double progress, double multiplicator, double offset) {
        try {
            getProgress().setCompleted((int)(progress * multiplicator + offset));
        } catch (ProcessStoppedException processStoppedException) {}
    }

    private RowTransformer createTransformer(Table table, List<String> validAttributes) {
        Workload workload;
        int numberOfAttributes = validAttributes.size();
        if (numberOfAttributes < 15) {
            workload = Workload.SMALL;
        } else if (numberOfAttributes < 150) {
            workload = Workload.MEDIUM;
        } else if (numberOfAttributes < 1500) {
            workload = Workload.LARGE;
        } else {
            workload = Workload.HUGE;
        }

        return table.transform(validAttributes).workload(workload);
    }

    private AttributeWeights calculateWeights(String[] attributeNames, boolean[] attributeValidities, NumericalMatrix matrix) {
        boolean normalizeWeights = getParameterAsBoolean("normalize_weights");
        boolean squared = getParameterAsBoolean("squared_correlation");
        int nAttributes = attributeNames.length;

        AttributeWeights weights = new AttributeWeights();

        for (int i = 0; i < nAttributes; i++) {
            if (attributeValidities[i]) {
                double sum = 0.0D;
                for (int j = 0; j < nAttributes; j++) {
                    if (!Double.isNaN(matrix.getValue(i, j)))
                    {
                        if (squared) {
                            sum += 1.0D - matrix.getValue(i, j);
                        } else {
                            sum += 1.0D - matrix.getValue(i, j) * matrix.getValue(i, j);
                        }
                    }
                }
                weights.setWeight(attributeNames[i], sum / nAttributes);
            }
        }

        if (normalizeWeights) {
            weights.normalize();
        }
        return weights;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> parameters = super.getParameterTypes();
        parameters.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(this.subsetSelector.getParameterTypes(), true));
        parameters.add(new ParameterTypeBoolean("normalize_weights", "Indicates if the attributes weights should be normalized.", true, false));

        parameters.add(new ParameterTypeBoolean("squared_correlation", "Indicates if the squared correlation should be calculated.", false, false));

        return parameters;
    }

    @Override
    public OperatorVersion[] getIncompatibleVersionChanges() { return (OperatorVersion[])ArrayUtils.addAll(super.getIncompatibleVersionChanges(), new OperatorVersion[] { VERSION_INCORRECT_PASSTHROUGH, LAST_LEGACY_CORE_COMPATIBLE }); }
}

