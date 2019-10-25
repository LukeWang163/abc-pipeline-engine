package base.operators.operator.generator.generators;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.generator.generators.DataGenerators.GeneratorType;
import base.operators.operator.Operator;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.ParameterTypeTupel;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.math.container.Range;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Stream;

class NumericSeriesDataGenerator extends AbstractDataGenerator {
    private static final String PARAMETER_NUMERIC_SERIES_CONFIGURATION = "numeric_series_configuration";
    private static final DoubleFunction<String> SMALLER_THAN_ZERO = (x) -> {
        return x < 0.0D ? "&lt;0" : null;
    };
    private static final DoubleFunction<String> SMALLER_THAN_OR_EQUAL_ZERO = (x) -> {
        return x <= 0.0D ? "&#8804;0" : null;
    };

    NumericSeriesDataGenerator(Operator parent) {
        super(parent);
    }

    @Override
    public ExampleSetMetaData generateExampleSetMetaData() throws UserError {
        NumericSeriesDataGenerator.Settings settings = new NumericSeriesDataGenerator.Settings();
        ExampleSetMetaData md = new ExampleSetMetaData();
        md.setNumberOfExamples(settings.numberOfExamples);

        for(int i = 0; i < settings.attributeNames.length; ++i) {
            NumericSeriesDataGenerator.NumericSeriesFunction currentType = settings.seriesFunction[i];
            double startValue = settings.startValues[i];
            double stopValue = settings.stopValues[i];
            Range range = this.checkSettingsAndGetRange(currentType, startValue, stopValue, settings.attributeNames[i]);
            AttributeMetaData newAttribute = new AttributeMetaData(settings.attributeNames[i], 4);
            newAttribute.setValueRange(range, SetRelation.EQUAL);
            if (Double.isNaN(range.getUpper())) {
                MDInteger missings = new MDInteger(1);
                missings.increaseByUnknownAmount();
                newAttribute.setNumberOfMissingValues(missings);
            }
            md.addAttribute(newAttribute);
        }
        return md;
    }

    @Override
    public ExampleSet generateExampleSet() throws UndefinedParameterError, ProcessStoppedException {
        NumericSeriesDataGenerator.Settings cnf = new NumericSeriesDataGenerator.Settings();
        int numberOfAttributes = cnf.attributeNames.length;
        this.getParent().getProgress().setTotal(cnf.numberOfExamples + numberOfAttributes);
        NumericSeriesDataGenerator.SingleNumericSeriesGenerator[] dataGenerators = new NumericSeriesDataGenerator.SingleNumericSeriesGenerator[numberOfAttributes];
        Attribute[] attributes = new Attribute[numberOfAttributes];

        for(int i = 0; i < numberOfAttributes; ++i) {
            dataGenerators[i] = new NumericSeriesDataGenerator.SingleNumericSeriesGenerator(cnf.startValues[i], cnf.stopValues[i], cnf.stepSizes[i], cnf.seriesFunction[i]);
            attributes[i] = AttributeFactory.createAttribute(cnf.attributeNames[i], 2);
            this.getParent().getProgress().step();
        }

        ExampleSetBuilder builder = ExampleSets.from(attributes);
        for(int example = 0; example < cnf.numberOfExamples; ++example) {
            double[] row = new double[numberOfAttributes];
            for(int attr = 0; attr < numberOfAttributes; ++attr) {
                row[attr] = dataGenerators[attr].getNext();
            }
            builder.addRow(row);
            this.getParent().getProgress().step();
        }
        this.getParent().getProgress().complete();
        return builder.build();
    }

    @Override
    protected List<ParameterType> getParameterTypesInternal() {
        List<ParameterType> types = new ArrayList();
        String[] numericSeriesFunctionNames = (String[])Arrays.stream(NumericSeriesDataGenerator.NumericSeriesFunction.values()).map(NumericSeriesDataGenerator.NumericSeriesFunction::getName).toArray((x$0) -> {
            return new String[x$0];
        });
        ParameterType type = new ParameterTypeList("numeric_series_configuration", "Numeric series list to generate.", new ParameterTypeString("attribute_name", "Attribute name"), new ParameterTypeTupel("series_settings (type ; min ; max/stepsize)", "Numeric series settings.", new ParameterType[]{new ParameterTypeCategory("type", "", numericSeriesFunctionNames, 0), new ParameterTypeDouble("min_value", "min value", -1.7976931348623157E308D, 1.7976931348623157E308D, 0.0D), new ParameterTypeDouble("max_value/stepsize", "max value/stepsize", -1.7976931348623157E308D, 1.7976931348623157E308D, 1.0D)}), false);
        type.setOptional(false);
        type.setPrimary(true);
        types.add(type);
        return types;
    }

    @Override
    protected GeneratorType getGeneratorType() {
        return GeneratorType.NUMERIC_SERIES;
    }

    private Range checkSettingsAndGetRange(NumericSeriesDataGenerator.NumericSeriesFunction function, double startValue, double stopValue, String name) {
        Optional<String> error = Stream.of(new Double[] {startValue, stopValue}).map(function::verify).filter(Objects::nonNull).findAny();
        Range range = new Range();
        if (error.isPresent()) {
            OutputPort port = (OutputPort)this.getParent().getOutputPorts().getPortByIndex(0);
            List<? extends QuickFix> fixes = Collections.singletonList(new ParameterSettingQuickFix(this.getParent(), "numeric_series_configuration"));
            port.addError(new SimpleMetaDataError(Severity.WARNING, port, fixes, "data_generator.WrongValuesForSeriesSetting", new Object[]{name, function.getName(), error.get()}));
            return range;
        } else {
            range.add(function.applyAsDouble(startValue));
            range.add(function.applyAsDouble(stopValue));
            return range;
        }
    }

    private static class SingleNumericSeriesGenerator {
        private final DoubleUnaryOperator numericFunction;
        private final double stepSize;
        private final double lowerBound;
        private final double upperBound;
        private double nextValue;

        SingleNumericSeriesGenerator(double startValue, double stopValue, double stepSize, DoubleUnaryOperator numericFunction) {
            if (startValue > stopValue) {
                this.lowerBound = stopValue;
                this.upperBound = startValue;
            } else {
                this.lowerBound = startValue;
                this.upperBound = stopValue;
            }

            this.nextValue = startValue;
            this.stepSize = stepSize;
            this.numericFunction = numericFunction;
        }

        double getNext() {
            return this.numericFunction.applyAsDouble(this.getNextValue());
        }

        private double getNextValue() {
            double currentValue = this.nextValue;
            double next = this.nextValue + this.stepSize;
            if (next < this.lowerBound) {
                next = this.lowerBound;
            } else if (next > this.upperBound) {
                next = this.upperBound;
            }

            this.nextValue = next;
            return currentValue;
        }
    }

    private static enum NumericSeriesFunction implements DoubleUnaryOperator {
        LINEAR(DoubleUnaryOperator.identity()),
        QUADRATIC((x) -> {
            return Math.pow(x, 2.0D);
        }),
        SQUARE_ROOT(Math::sqrt, NumericSeriesDataGenerator.SMALLER_THAN_ZERO),
        POWER_OF_10((x) -> {
            return Math.pow(10.0D, x);
        }),
        POWER_OF_2((x) -> {
            return Math.pow(2.0D, x);
        }),
        POWER_OF_E(Math::exp),
        LN(Math::log, NumericSeriesDataGenerator.SMALLER_THAN_OR_EQUAL_ZERO),
        LOG10(Math::log10, NumericSeriesDataGenerator.SMALLER_THAN_OR_EQUAL_ZERO),
        LOG2((x) -> {
            return Math.log(x) / Math.log(2.0D);
        }, NumericSeriesDataGenerator.SMALLER_THAN_OR_EQUAL_ZERO);

        private final DoubleUnaryOperator operator;
        private final DoubleFunction<String> rangeCheck;

        private NumericSeriesFunction(DoubleUnaryOperator operator) {
            this(operator, (x) -> {
                return null;
            });
        }

        private NumericSeriesFunction(DoubleUnaryOperator operator, DoubleFunction<String> rangeCheck) {
            this.operator = operator;
            this.rangeCheck = rangeCheck;
        }

        @Override
        public double applyAsDouble(double value) {
            return this.operator.applyAsDouble(value);
        }

        public static NumericSeriesDataGenerator.NumericSeriesFunction byName(String string) {
            return valueOf(string.toUpperCase(Locale.ENGLISH).replace(" ", "_"));
        }

        public String getName() {
            return this.name().toLowerCase(Locale.ENGLISH).replace("_", " ");
        }

        public String verify(double value) {
            return (String)this.rangeCheck.apply(value);
        }
    }

    private final class Settings {
        private final int numberOfExamples;
        private final double[] startValues;
        private final double[] stopValues;
        private final double[] stepSizes;
        private final NumericSeriesDataGenerator.NumericSeriesFunction[] seriesFunction;
        private final String[] attributeNames;

        private Settings() throws UndefinedParameterError {
            Operator parent = NumericSeriesDataGenerator.this.getParent();
            this.numberOfExamples = parent.getParameterAsInt("number_of_examples");
            boolean useStepSize = parent.getParameterAsBoolean("use_stepsize");
            List<String[]> numericSeriesConfiguration = parent.getParameterList("numeric_series_configuration");
            int size = numericSeriesConfiguration.size();
            this.attributeNames = new String[size];
            this.seriesFunction = new NumericSeriesDataGenerator.NumericSeriesFunction[size];
            this.startValues = new double[size];
            double[] secondSettings = new double[size];
            int i = 0;

            for(Iterator var8 = numericSeriesConfiguration.iterator(); var8.hasNext(); ++i) {
                String[] pair = (String[])var8.next();
                this.attributeNames[i] = pair[0];
                String[] settings = ParameterTypeTupel.transformString2Tupel(pair[1]);
                this.seriesFunction[i] = NumericSeriesDataGenerator.NumericSeriesFunction.byName(settings[0]);
                this.startValues[i] = Double.parseDouble(settings[1]);
                secondSettings[i] = Double.parseDouble(settings[2]);
            }

            if (useStepSize) {
                this.stepSizes = secondSettings;
                this.stopValues = new double[size];
                Arrays.setAll(this.stopValues, (pos) -> {
                    return this.startValues[pos] + (double)(this.numberOfExamples - 1) * this.stepSizes[pos];
                });
            } else {
                this.stepSizes = new double[size];
                this.stopValues = secondSettings;
                Arrays.setAll(this.stepSizes, (pos) -> {
                    return (this.stopValues[pos] - this.startValues[pos]) / (double)(this.numberOfExamples - 1);
                });
            }
        }
    }
}
