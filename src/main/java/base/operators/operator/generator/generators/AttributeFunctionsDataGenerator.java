package base.operators.operator.generator.generators;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.Tools;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorProgress;
import base.operators.operator.ProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeExpression;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.expression.ExampleResolver;
import base.operators.tools.expression.Expression;
import base.operators.tools.expression.ExpressionException;
import base.operators.tools.expression.ExpressionParser;
import base.operators.tools.expression.internal.ExpressionParserUtils;
import base.operators.tools.math.container.Range;
import java.util.ArrayList;
import java.util.List;

public class AttributeFunctionsDataGenerator extends AbstractDataGenerator {
    private static final String PARAMETER_ATTRIBUTE_GENERATOR_FUNCTIONS = "function_descriptions";
    private static final String PARAMETER_ATTRIBUTE_GENERATOR_ADD_ID_ATTRIBUTE = "add_id_attribute";
    private final class Settings
    {
        private final int numberOfExamples;
        private final boolean addIdAttribute;
        private final List<String[]> generatorFunctions;
        private Settings() throws UndefinedParameterError {
            Operator operator = getParent();
            this.numberOfExamples = operator.getParameterAsInt("number_of_examples");
            this.addIdAttribute = operator.getParameterAsBoolean("add_id_attribute");
            this.generatorFunctions = operator.getParameterList("function_descriptions");
        }
    }

    AttributeFunctionsDataGenerator(Operator parent) { super(parent); }

    @Override
    public ExampleSetMetaData generateExampleSetMetaData() throws UserError {
        Settings settings = new Settings();
        ExampleSetMetaData emd = new ExampleSetMetaData();
        emd.setNumberOfExamples(settings.numberOfExamples);
        emd.attributesAreKnown();

        if (settings.addIdAttribute) {
            AttributeMetaData idAttribute = new AttributeMetaData(AttributeFactory.createAttribute("id", 3));
            idAttribute.setRole("id");
            idAttribute.setValueRange(new Range(1.0D, ((Integer)emd.getNumberOfExamples().getValue()).doubleValue()), SetRelation.EQUAL);
            emd.addAttribute(idAttribute);
        }

        ExampleResolver exampleResolver = new ExampleResolver(emd);
        ExpressionParser expParser = ExpressionParserUtils.createAllModulesParser(getParent(), exampleResolver);

        for (String[] nameFunctionPair : settings.generatorFunctions) {
            String name = nameFunctionPair[0];
            String function = nameFunctionPair[1];
            try {
                Expression parsedExp = expParser.parse(function);
                AttributeMetaData amd = ExpressionParserUtils.generateAttributeMetaData(emd, name, parsedExp.getExpressionType());
                exampleResolver.addAttributeMetaData(amd);
                emd.addAttribute(amd);
            } catch (ExpressionException e) {
                OutputPort portByIndex = (OutputPort)getParent().getOutputPorts().getPortByIndex(0);
                if (e.getCause() instanceof base.operators.tools.expression.internal.UnknownResolverVariableException) {

                    emd.addAttribute(new AttributeMetaData(name, 1));
                    continue;
                }
                portByIndex.addError(new SimpleMetaDataError(ProcessSetupError.Severity.ERROR, portByIndex, "cannot_create_exampleset_metadata", new Object[] { e.getShortMessage() }));
                return new ExampleSetMetaData();
            }
        }
        return emd;
    }

    @Override
    public ExampleSet generateExampleSet() throws OperatorException {
        Settings settings = new Settings();
        ExampleSet exampleSet = ExampleSets.from(new Attribute[0]).withBlankSize(settings.numberOfExamples).build();

        OperatorProgress operatorProgress = getParent().getProgress();
        operatorProgress.setTotal(settings.generatorFunctions.size());

        if (settings.addIdAttribute) {
            operatorProgress.setTotal(operatorProgress.getTotal() + 1);
            Attribute idAttribute = Tools.createSpecialAttribute(exampleSet, "id", 3);
            int currentId = 1;
            for (Example example : exampleSet) {
                example.setValue(idAttribute, currentId);
                currentId++;
            }
            operatorProgress.step();
        }

        ExampleResolver resolver = new ExampleResolver(exampleSet);
        ExpressionParser parser = ExpressionParserUtils.createAllModulesParser(getParent(), resolver);

        for (String[] nameFunctionPair : settings.generatorFunctions) {
            String name = nameFunctionPair[0];
            String function = nameFunctionPair[1];
            try {
                ExpressionParserUtils.addAttribute(exampleSet, name, function, parser, resolver, getParent());
            } catch (ExpressionException e) {
                throw ExpressionParserUtils.convertToUserError(getParent(), function, e);
            }
            operatorProgress.step();
        }
        operatorProgress.complete();
        return exampleSet;
    }


    @Override
    protected List<ParameterType> getParameterTypesInternal() {
        List<ParameterType> types = new ArrayList<ParameterType>();
        ParameterTypeList parameterTypeList = new ParameterTypeList("function_descriptions", "List of functions to generate.", new ParameterTypeString("attribute_name", "Specifies the name of the constructed attribute"), new ParameterTypeExpression("function_expressions", "Function and arguments to use for generation.", new ParameterTypeExpression.OperatorVersionCallable(getParent())));
        parameterTypeList.setExpert(false);
        parameterTypeList.setOptional(false);
        parameterTypeList.setPrimary(true);
        types.add(parameterTypeList);
        ParameterTypeBoolean parameterTypeBoolean = new ParameterTypeBoolean("add_id_attribute", "If this parameter is set to true, an additional (numeric) id attribute is generated, which can be used in the function expressions.", false, false);
        parameterTypeBoolean.setOptional(false);
        types.add(parameterTypeBoolean);
        return types;
    }

    @Override
    protected DataGenerators.GeneratorType getGeneratorType() {
        return DataGenerators.GeneratorType.ATTRIBUTE_FUNCTIONS; }
}
