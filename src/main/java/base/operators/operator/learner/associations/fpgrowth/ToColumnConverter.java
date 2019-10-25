package base.operators.operator.learner.associations.fpgrowth;

import base.operators.belt.buffer.Buffers;
import base.operators.belt.buffer.ObjectBuffer;
import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.ColumnTypes;
import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.nio.model.CSVResultSetConfiguration;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.CSVParseException;
import base.operators.tools.I18N;
import base.operators.tools.LineParser;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;


class ToColumnConverter
{
    private static final ColumnType<ItemSet> ITEM_SET_TYPE = ColumnTypes.objectType("base.operators.fpgrowth.itemset", ItemSet.class, null);

    private final Operator operator;

    ToColumnConverter(Operator operator) { this.operator = operator; }


    Column convert(ExampleSet exampleSet, Set<String> mandatoryItems) throws OperatorException {
        LineParser lineParser;
        boolean trimItemNames = this.operator.getParameterAsBoolean("trim_item_names");
        int inputFormat = this.operator.getParameterAsInt("input_format");
        switch (inputFormat) {
            case 0:
                preProcessItemListInAColumn(exampleSet);
                lineParser = initLineParser();
                return convert(exampleSet, lineParser, trimItemNames);
            case 1:
                return convert(exampleSet, trimItemNames);
        }

        ExampleSet workingSet = preProcessDummyCodedColumns(exampleSet);
        fillRegexMandatoryItems(workingSet, mandatoryItems);
        return convert(workingSet);
    }






    private void preProcessItemListInAColumn(ExampleSet exampleSet) {
        int nominalRegularCount = exampleSet.getAttributes().size();

        if (nominalRegularCount > 1) {
            String message = I18N.getErrorMessage("fp_growth.more_nominal_attributes", new Object[] { Integer.valueOf(nominalRegularCount - 1) });
            this.operator.logWarning(message);
        }
    }





    private ExampleSet preProcessDummyCodedColumns(ExampleSet exampleSet) throws UserError {
        ExampleSet workingSet = (ExampleSet)exampleSet.clone();

        Iterator<Attribute> it = workingSet.getAttributes().iterator();
        while (it.hasNext()) {
            Attribute a = (Attribute)it.next();
            if (a.getMapping().size() != 2) {
                it.remove();
            }
        }
        if (workingSet.getAttributes().size() == 0) {
            throw new UserError(this.operator, "fp_growth.no_binominals");
        }
        int removeCount = exampleSet.getAttributes().size() - workingSet.getAttributes().size();
        if (removeCount > 0) {
            String message = I18N.getErrorMessage("fp_growth.non_binominal_attributes", new Object[] { Integer.valueOf(removeCount) });
            this.operator.logWarning(message);
        }

        return workingSet;
    }

    private Column convert(ExampleSet exampleSet) throws UndefinedParameterError {
        String positiveValueString = this.operator.getParameterAsString("positive_value");
        boolean manualPositiveValue = (positiveValueString != null && !"".equals(positiveValueString));
        Attribute[] attributes = exampleSet.getAttributes().createRegularAttributeArray();
        int[] positiveIndices = new int[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            Attribute attribute = attributes[i];
            if (manualPositiveValue) {
                positiveIndices[i] = attribute.getMapping().mapString(positiveValueString);
            } else {
                positiveIndices[i] = attribute.getMapping().getPositiveIndex();
            }
        }
        int i = 0;
        ObjectBuffer<ItemSet> buffer = Buffers.objectBuffer(exampleSet.size());
        for (Example example : exampleSet) {
            buffer.set(i++, new ItemSet(example, attributes, positiveIndices));
        }
        return buffer.toColumn(ITEM_SET_TYPE);
    }




    private Column convert(ExampleSet exampleSet, boolean trimItemNames) {
        Attribute[] attributes = exampleSet.getAttributes().createRegularAttributeArray();
        int i = 0;
        ObjectBuffer<ItemSet> buffer = Buffers.objectBuffer(exampleSet.size());
        for (Example example : exampleSet) {
            buffer.set(i++, new ItemSet(example, attributes, trimItemNames));
        }
        return buffer.toColumn(ITEM_SET_TYPE);
    }

    private Column convert(ExampleSet exampleSet, LineParser lineParser, boolean trimItemNames) throws UserError {
        Attribute attribute = (Attribute)exampleSet.getAttributes().iterator().next();
        ObjectBuffer<ItemSet> buffer = Buffers.objectBuffer(exampleSet.size());
        int i = 0;
        for (Example example : exampleSet) {
            String[] items;
            if (!Double.isNaN(example.getValue(attribute))) {
                try {
                    items = lineParser.parse(example.getValueAsString(attribute));
                } catch (CSVParseException e) {
                    throw new UserError(this.operator, "fp_growth.parsing_failure", new Object[] { Integer.valueOf(i), attribute.getName(), e.getMessage() });
                }
            } else {
                items = null;
            }
            buffer.set(i++, new ItemSet(items, trimItemNames));
        }
        return buffer.toColumn(ITEM_SET_TYPE);
    }

    private LineParser initLineParser() throws OperatorException {
        try (CSVResultSetConfiguration parserConfiguration = new CSVResultSetConfiguration()) {
            parserConfiguration.setSkipComments(false);
            parserConfiguration.setTrimLines(false);
            parserConfiguration.setUseQuotes(this.operator.getParameterAsBoolean("use_quotes"));
            if (this.operator.isParameterSet("item_separators")) {
                parserConfiguration.setColumnSeparators(this.operator.getParameterAsString("item_separators"));
            }

            if (this.operator.isParameterSet("escape_character")) {
                parserConfiguration.setEscapeCharacter(this.operator.getParameterAsChar("escape_character"));
            }

            if (this.operator.isParameterSet("quotes_character")) {
                parserConfiguration.setQuoteCharacter(this.operator.getParameterAsChar("quotes_character"));
            }

            return new LineParser(parserConfiguration);
        }
    }

    private void fillRegexMandatoryItems(ExampleSet exampleSet, Set<String> mandatoryItems) throws UndefinedParameterError {
        String mustContainRegExp = this.operator.getParameterAsString("must_contain_regexp");
        if (mustContainRegExp != null && !mustContainRegExp.isEmpty()) {
            Pattern mustContainPattern = Pattern.compile(mustContainRegExp);
            for (Attribute attribute : exampleSet.getAttributes()) {
                String name = attribute.getName();
                if (mustContainPattern.matcher(name).matches()) {
                    mandatoryItems.add(name);
                }
            }
        }
    }
}

