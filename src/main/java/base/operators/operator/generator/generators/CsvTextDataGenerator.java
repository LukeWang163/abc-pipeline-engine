package base.operators.operator.generator.generators;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.generator.generators.DataGenerators.GeneratorType;
import base.operators.operator.Operator;
import base.operators.operator.OperatorCreationException;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.preprocessing.GuessValueTypes;
import base.operators.parameter.ParameterHandler;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.ParameterTypeText;
import base.operators.parameter.TextType;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.OperatorService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class CsvTextDataGenerator extends AbstractDataGenerator {
    private static final String PARAMETER_TEXT_INPUT_FORM = "input_csv_text";
    private static final String PARAMETER_TEXT_COLUMN_SEPARATOR = "column_separator";
    private static final String PARAMETER_TEXT_DECIMAL_POINT_CHARACTER = "decimal_point_character";
    private static final String PARAMETER_TEXT_PARSE_ALL_AS_NOMINAL = "parse_all_as_nominal";
    private static final String PARAMETER_TEXT_TRIM_ATTRIBUTES = "trim_attribute_names";
    private static final String NEWLINE = "\n";

    CsvTextDataGenerator(Operator parent) {
        super(parent);
    }

    @Override
    public ExampleSetMetaData generateExampleSetMetaData() throws UserError {
        CsvTextDataGenerator.Settings settings = new CsvTextDataGenerator.Settings();
        ExampleSetMetaData md = new ExampleSetMetaData();
        md.setNumberOfExamples(settings.numberOfExamples);
        String[] var3 = settings.newAttributeNames;
        int var4 = var3.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String newAttributeName = var3[var5];
            md.addAttribute(new AttributeMetaData(newAttributeName, 0));
        }
        return md;
    }

    @Override
    public ExampleSet generateExampleSet() throws OperatorException {
        CsvTextDataGenerator.Settings settings = new CsvTextDataGenerator.Settings();
        String[] rows = settings.inputString.split("\n");
        String[] columns = rows[0].split(settings.separator, 0);
        this.getParent().getProgress().setTotal(rows.length);
        Attribute[] attributes = (Attribute[])Arrays.stream(settings.newAttributeNames).map((name) -> {
            return AttributeFactory.createAttribute(name, 1);
        }).toArray((x$0) -> {
            return new Attribute[x$0];
        });
        ExampleSetBuilder builder = ExampleSets.from(attributes);
        double[] values = new double[columns.length];
        Arrays.fill(values, 0.0D / 0.0);

        for(int row = 1; row < rows.length; ++row) {
            String[] currentCells = rows[row].split(settings.separator);
            double[] currentValues = (double[])values.clone();

            for(int cell = 0; cell < currentCells.length; ++cell) {
                if (!currentCells[cell].isEmpty()) {
                    currentValues[cell] = (double)attributes[cell].getMapping().mapString(currentCells[cell]);
                }
            }

            builder.addRow(currentValues);
            this.getParent().getProgress().step();
        }

        ExampleSet exampleSet = builder.build();
        if (!settings.parseAllAsNominal) {
            try {
                GuessValueTypes guessValuesTypes = (GuessValueTypes)OperatorService.createOperator(GuessValueTypes.class);
                guessValuesTypes.setParameter("decimal_point_character", settings.decimalPointCharacter);
                exampleSet = guessValuesTypes.apply(exampleSet);
            } catch (OperatorCreationException var11) {
                throw new OperatorException("Cannot create GuessValueTypes: " + var11, var11);
            }
        }

        this.getParent().getProgress().complete();
        return exampleSet;
    }

    @Override
    protected List<ParameterType> getParameterTypesInternal() {
        List<ParameterType> types = new ArrayList();
        ParameterType type = new ParameterTypeText("input_csv_text", "CSV text.", TextType.PLAIN);
        type.setExpert(false);
        type.setOptional(false);
        type.setPrimary(true);
        types.add(type);
        type = new ParameterTypeString("column_separator", "Column separator.", ",", false);
        types.add(type);
        type = new ParameterTypeBoolean("parse_all_as_nominal", "If this parameter is set to true, all attributes are forced to nominal type.", false, true);
        type.setOptional(false);
        types.add(type);
        type = new ParameterTypeString("decimal_point_character", "Decimal point character.", ".", true);
        type.setOptional(false);
        type.registerDependencyCondition(new BooleanParameterCondition(this.getParent(), "parse_all_as_nominal", false, false));
        types.add(type);
        type = new ParameterTypeBoolean("trim_attribute_names", "If this parameter is set to true, attribute names will be trimmed.", true, true);
        type.setOptional(false);
        types.add(type);
        return types;
    }

    @Override
    protected GeneratorType getGeneratorType() {
        return GeneratorType.COMMA_SEPARATED_TEXT;
    }

    private final class Settings {
        private final String inputString;
        private final String separator;
        private final String decimalPointCharacter;
        private final boolean parseAllAsNominal;
        private final String[] newAttributeNames;
        private final int numberOfExamples;

        private Settings() throws UndefinedParameterError {
            ParameterHandler parent = CsvTextDataGenerator.this.getParent();
            this.separator = parent.getParameterAsString("column_separator");
            this.parseAllAsNominal = parent.getParameterAsBoolean("parse_all_as_nominal");
            this.decimalPointCharacter = !this.parseAllAsNominal ? parent.getParameterAsString("decimal_point_character") : null;
            this.inputString = parent.getParameterAsString("input_csv_text");
            String[] rows = this.inputString.split("\n");
            this.numberOfExamples = rows.length - 1;
            String[] attributeNames = rows[0].split(this.separator);
            boolean trimAttributeNames = parent.getParameterAsBoolean("trim_attribute_names");
            if (trimAttributeNames) {
                attributeNames = (String[])Arrays.stream(attributeNames).map(String::trim).toArray((x$0) -> {
                    return new String[x$0];
                });
            }
            this.newAttributeNames = attributeNames;
        }
    }
}
