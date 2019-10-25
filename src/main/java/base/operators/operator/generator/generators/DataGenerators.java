package base.operators.operator.generator.generators;

import base.operators.operator.Operator;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.EqualStringCondition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class DataGenerators {
    static final String PARAMETER_GENERATOR_TYPE = "generator_type";
    static final String PARAMETER_NUMBER_OF_EXAMPLES = "number_of_examples";
    static final String PARAMETER_SERIES_STEP_SIZE_FLAG = "use_stepsize";

    private DataGenerators() {
        throw new AssertionError("Utility class");
    }

    public static List<ParameterType> getParameterTypes(Operator parent) {
        List<ParameterType> types = new ArrayList();
        String[] generatingNames = (String[])Arrays.stream(DataGenerators.GeneratorType.values()).map(DataGenerators.GeneratorType::getName).toArray((x$0) -> {
            return new String[x$0];
        });
        types.add(new ParameterTypeCategory("generator_type", "The type of generator to create the ExampleSet.", generatingNames, 0, false));
        ParameterType type = new ParameterTypeInt("number_of_examples", "The number of Examples to generate.", 0, 2147483647, 100, false);
        type.registerDependencyCondition(new EqualStringCondition(parent, "generator_type", false, new String[]{DataGenerators.GeneratorType.NUMERIC_SERIES.getName(), DataGenerators.GeneratorType.DATE_SERIES.getName(), DataGenerators.GeneratorType.ATTRIBUTE_FUNCTIONS.getName()}));
        types.add(type);
        type = new ParameterTypeBoolean("use_stepsize", "If this parameter is set to true, a 'start value' and a 'stepsize' will be used in the series generation. If tis parameter is set to false a 'start value' and 'stop value' is used.", false, false);
        type.registerDependencyCondition(new EqualStringCondition(parent, "generator_type", true, new String[]{DataGenerators.GeneratorType.NUMERIC_SERIES.getName(), DataGenerators.GeneratorType.DATE_SERIES.getName()}));
        types.add(type);
        DataGenerators.GeneratorType[] var4 = DataGenerators.GeneratorType.values();
        int var5 = var4.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            DataGenerators.GeneratorType value = var4[var6];
            types.addAll(value.getInstance(parent).getParameterTypes());
        }

        return types;
    }

    public static DataGenerator getGenerator(Operator parent) throws UndefinedParameterError {
        return DataGenerators.GeneratorType.byName(parent.getParameterAsString("generator_type")).getInstance(parent);
    }

    static enum GeneratorType {
        ATTRIBUTE_FUNCTIONS(AttributeFunctionsDataGenerator::new),
        NUMERIC_SERIES(NumericSeriesDataGenerator::new),
        DATE_SERIES(DateSeriesDataGenerator::new),
        COMMA_SEPARATED_TEXT(CsvTextDataGenerator::new);

        private final Function<Operator, DataGenerator> generator;

        private GeneratorType(Function<Operator, DataGenerator> generator) {
            this.generator = generator;
        }

        public DataGenerator getInstance(Operator parent) {
            return (DataGenerator)this.generator.apply(parent);
        }

        public static DataGenerators.GeneratorType byName(String name) {
            return valueOf(name.toUpperCase(Locale.ENGLISH).replace(' ', '_'));
        }

        public String getName() {
            return this.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        }
    }
}
