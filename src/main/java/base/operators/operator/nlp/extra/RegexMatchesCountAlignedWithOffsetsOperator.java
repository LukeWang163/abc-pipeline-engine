package base.operators.operator.nlp.extra;

import base.operators.example.*;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.MemoryExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.operator.preprocessing.filter.ExampleFilter;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexMatchesCountAlignedWithOffsetsOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String TOKEN_ATTRIBUTE_NAME = "token_attribute_name";

    public static final String EDIT_REGEX_EXPRESSION = "edit_regex_expression";
    public static final String REGEX_FEATURE_NAME = "regex_feature_name";
    public static final String REGEX_EXPRESSION = "regex_expression";

    public static final String OFFSET = "offset";
    public static final String OFFSETS = "offsets_list";

    public static final String NORMALIZE_BY_MATCH_COUNT = "normalize_by_match_count";

    public RegexMatchesCountAlignedWithOffsetsOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ID_ATTRIBUTE_NAME, TOKEN_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        String[] offsets = ParameterTypeEnumeration
                .transformString2Enumeration(getParameterAsString(OFFSETS));

        String id_col = getParameterAsString(ID_ATTRIBUTE_NAME);
        String token_col = getParameterAsString(TOKEN_ATTRIBUTE_NAME);
        List<String[]> regex_list = getParameterList(EDIT_REGEX_EXPRESSION);
        boolean normalize = getParameterAsBoolean(NORMALIZE_BY_MATCH_COUNT);

        //获得数据的id集合
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        Set<Double> id_set = new HashSet<>();
        for (int i = 0; i < exampleSet.size(); i++) {
            id_set.add(exampleSet.getExample(i).getValue(attributes.get(id_col)));
        }

        //创建新的数据表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute new_id_attribute = AttributeFactory.createAttribute(id_col, attributes.get(id_col).getValueType());
        attributeList.add(new_id_attribute);
        for (int j = 0; j < regex_list.size(); j++) {
            Attribute new_feature_attribute = AttributeFactory.createAttribute(regex_list.get(j)[0], Ontology.NUMERICAL);
            attributeList.add(new_feature_attribute);
        }
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        //按照id进行exampleset的筛选
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        OperatorDescription description = null;
        try {
            description = new OperatorDescription(loader, null, null, "com.rapidminer.operator.preprocessing.filter.ExampleFilter", null, null, null, null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Iterator<Double> it = id_set.iterator();
        while (it.hasNext()) {
            double idValue = it.next();
            ExampleFilter examplesFilter = new ExampleFilter(description);
            examplesFilter.setParameter("condition_class", "attribute_value_filter");
            examplesFilter.setParameter("parameter_string", id_col+"="+idValue);
            ExampleSet filterExampleSet = examplesFilter.apply(exampleSet);
            List<String> token_list = new ArrayList<>();
            for(Example example: filterExampleSet){
                token_list.add(example.getValueAsString(filterExampleSet.getAttributes().get(token_col)));
            }

            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(new_id_attribute, idValue);
            for (int k = 0; k < regex_list.size(); k++) {
                Pattern regex = Pattern.compile(regex_list.get(k)[1]);
                for (int l = 0; l < token_list.size(); l++) {
                    int countMatches = 0;
                    int countAlignedMatches = 0;
                    Matcher matcher = regex.matcher (token_list.get(l));
                    while (matcher.find ()) {
                        countMatches++;
                        int position = matcher.start();
                        for (int u = 0; u < offsets.length; u++) {
                            int offset = l + Integer.valueOf(offsets[u]);
                            if (offset >= 0 && offset < token_list.size()) {
                                String offsetText = token_list.get(offset);
                                if (offsetText.length() > position) {
                                    Matcher offsetMatcher =
                                            regex.matcher (offsetText.substring(position));
                                    if (offsetMatcher.lookingAt())
                                        countAlignedMatches++;
                                }
                            }
                        }
                    }
                    if (countAlignedMatches > 0){
                        dataRow.set(attributeList.get(k+1), normalize
                                ? ((double)countAlignedMatches)/countMatches
                                : (double)countAlignedMatches);
                    }else{
                        dataRow.set(attributeList.get(k+1), 0);
                    }
                }
            }
            exampleTable.addDataRow(dataRow);

        }
        ExampleSet exampleSetOut = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSetOut);

    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the id attribute which token belongs.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(TOKEN_ATTRIBUTE_NAME, "The name of the token attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeEnumeration(OFFSETS, "", new ParameterTypeInt(OFFSET,"The offest of token.", 0, Integer.MAX_VALUE, false), false));

        ParameterType type = new ParameterTypeList(EDIT_REGEX_EXPRESSION, "Regex expression list",
                new ParameterTypeString(REGEX_FEATURE_NAME, "Regex expression name.", false, false),
                new ParameterTypeRegexp(REGEX_EXPRESSION, "Regex expression.", false), false);
        types.add(new ParameterTypeBoolean(NORMALIZE_BY_MATCH_COUNT,"Is the number of matches used for standardization?", false,false));

        types.add(type);
        return types;
    }


}
