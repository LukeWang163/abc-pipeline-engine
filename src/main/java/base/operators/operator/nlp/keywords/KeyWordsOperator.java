package base.operators.operator.nlp.keywords;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.*;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.keywords.core.KeyWords;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.Ontology;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wagnpanpan
 * create time:  2019.07.30.
 * description:
 */

public class KeyWordsOperator extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort exampleSetOutput1 = getOutputPorts().createPort("example set 1");
    private OutputPort exampleSetOutput2 = getOutputPorts().createPort("example set 2");


    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String SIZE = "size";
    public static final String LANGUAGE = "language";
    public static final String METHOD = "method";
    public static final String DAMPING_FACTOR = "damping_factor";//阻尼系数
    public static final String MAX_ITER = "max_iter";//迭代次数
    public static final String MIN_DIFF = "min_diff";//迭代结束误差
    public static final String WINDOWS = "windows";

    public static String[] METHODS = {"tfidf","textrank"};
    public static String[] LANGUAGES = {"Chinese","English"};


    public KeyWordsOperator(OperatorDescription description){
        super(description);

        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ID_ATTRIBUTE_NAME,DOC_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        String id_column = getParameterAsString(ID_ATTRIBUTE_NAME);
        String doc_column = getParameterAsString(DOC_ATTRIBUTE_NAME);
        int size = getParameterAsInt(SIZE);
        int language = getParameterAsInt(LANGUAGE);
        int method = getParameterAsInt(METHOD);
        Float damping_factor = (float) getParameterAsDouble(DAMPING_FACTOR);
        int max_iter = getParameterAsInt(MAX_ITER);
        Float min_diff = (float)getParameterAsDouble(MIN_DIFF);
        int windows = getParameterAsInt(WINDOWS);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();

        Attributes attributes = exampleSet.getAttributes();
        Attribute id_seleted = attributes.get(id_column);
        Attribute doc_seleted = attributes.get(doc_column);
        //获取内容
        List<String> idList = new ArrayList<>();
        List<String> docList = new ArrayList<>();
        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            idList.add(example.getValueAsString(id_seleted));
            docList.add(example.getValueAsString(doc_seleted));
        }
        //英文的话判断是否含有中文字符
        if (1 == language) {
            for (String doc : docList) {
                if (isCHinese(doc) == true) {
                    throw new UnsupportedOperationException("语言选择为英文，文档内容中含有中文字符！");
                }
            }
        }
        if (0 == method) {
            System.out.println("Stage2: 通过TFIDF获取关键词");
            //关键词结果组成<id,<word, tfidf>>对
            Map<String, Map<String, Double>> res = KeyWords.getKeyWordsTFIDF(idList, docList, size);

            // 构造第一个输出表
            List<Attribute> attributeList1 = new ArrayList<>();
            Attribute new_id_attribute_1 = AttributeFactory.createAttribute(id_column, id_seleted.isNumerical() ? Ontology.NUMERICAL : Ontology.NOMINAL);
            attributeList1.add(new_id_attribute_1);
            Attribute new_keyword_attribute_1 = AttributeFactory.createAttribute("keyWords", Ontology.STRING);
            attributeList1.add(new_keyword_attribute_1);
            Attribute new_tfidf_attribute_1 = AttributeFactory.createAttribute("tfidf", Ontology.NUMERICAL);
            attributeList1.add(new_tfidf_attribute_1);

            MemoryExampleTable exampleTable1 = new MemoryExampleTable(attributeList1);
            for (Map.Entry<String, Map<String, Double>> idEntry : res.entrySet()) {
                for (Map.Entry<String, Double> wordEntry : idEntry.getValue().entrySet()) {
                    DataRowFactory factory = new DataRowFactory(0, '.');
                    DataRow dataRow = factory.create(attributeList1.size());
                    dataRow.set(new_id_attribute_1, id_seleted.isNumerical()? Double.parseDouble(idEntry.getKey()): new_id_attribute_1.getMapping().mapString(idEntry.getKey()));
                    dataRow.set(new_keyword_attribute_1, new_keyword_attribute_1.getMapping().mapString(wordEntry.getKey()));
                    dataRow.set(new_tfidf_attribute_1, wordEntry.getValue());
                    exampleTable1.addDataRow(dataRow);
                }
            }

            ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable1);
            exampleSetOutput1.deliver(exampleSet1);

            // 构造第二个输出表
            ExampleTable exampleTable2 = exampleSet.getExampleTable();
            Attribute new_keyword_attribute_2 = AttributeFactory.createAttribute("keyWords", Ontology.STRING);
            exampleTable2.addAttribute(new_keyword_attribute_2);
            exampleSet.getAttributes().addRegular(new_keyword_attribute_2);

            for (int j = 0; j < exampleSet.size(); j++) {
                Example example = exampleSet.getExample(j);
                example.setValue(new_keyword_attribute_2, new_keyword_attribute_2.getMapping().mapString(StringUtils.join(res.get(example.getValueAsString(id_seleted)).keySet().toArray(), " ")));
            }

            exampleSetOutput2.deliver(exampleSet);

            System.out.println("Stage3: 写入数据完毕");
        } else if (1 == method) {
            System.out.println("Stage2: 通过TextRank获取关键词");
            Map<String, Map<String, Double>> res = new HashMap<>();
            for (int i = 0; i < idList.size(); i++) {
                Map<String, Double> per_res = KeyWords.getKeyWordsTextRank(docList.get(i), size, damping_factor, max_iter, min_diff,
                        windows);
                res.put(idList.get(i), per_res);
            }

            System.out.println("Stage3: 准备写入数据");
            // 构造第一个输出表
            List<Attribute> attributeList1 = new ArrayList<>();
            Attribute new_id_attribute_1 = AttributeFactory.createAttribute(id_column, id_seleted.isNumerical()?Ontology.NUMERICAL:Ontology.NOMINAL);
            attributeList1.add(new_id_attribute_1);
            Attribute new_keyword_attribute_1 = AttributeFactory.createAttribute("keyWords", Ontology.STRING);
            attributeList1.add(new_keyword_attribute_1);
            Attribute new_textrank_attribute_1 = AttributeFactory.createAttribute("textrank", Ontology.NUMERICAL);
            attributeList1.add(new_textrank_attribute_1);

            MemoryExampleTable exampleTable1 = new MemoryExampleTable(attributeList1);
            for (Map.Entry<String, Map<String, Double>> idEntry : res.entrySet()) {
                for (Map.Entry<String, Double> wordEntry : idEntry.getValue().entrySet()) {
                    DataRowFactory factory = new DataRowFactory(0, '.');
                    DataRow dataRow = factory.create(attributeList1.size());
                    dataRow.set(new_id_attribute_1, id_seleted.isNumerical()? Double.parseDouble(idEntry.getKey()): new_id_attribute_1.getMapping().mapString(idEntry.getKey()));
                    dataRow.set(new_keyword_attribute_1, new_keyword_attribute_1.getMapping().mapString(wordEntry.getKey()));
                    dataRow.set(new_textrank_attribute_1, wordEntry.getValue());
                    exampleTable1.addDataRow(dataRow);
                }
            }

            ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable1);
            exampleSetOutput1.deliver(exampleSet1);

            // 构造第二个输出表
            ExampleTable exampleTable2 = exampleSet.getExampleTable();
            Attribute new_keyword_attribute_2 = AttributeFactory.createAttribute("keyWords", Ontology.STRING);
            exampleTable2.addAttribute(new_keyword_attribute_2);
            exampleSet.getAttributes().addRegular(new_keyword_attribute_2);

            for (int j = 0; j < exampleSet.size(); j++) {
                Example example = exampleSet.getExample(j);
                example.setValue(new_keyword_attribute_2, new_keyword_attribute_2.getMapping().mapString(StringUtils.join(res.get(example.getValueAsString(id_seleted)).keySet().toArray(), " ")));
            }
            exampleSetOutput2.deliver(exampleSet);

            System.out.println("Stage3: 写入数据完毕");

        }

    }
	/**
	 *
	 * Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ： 4E00-9FBF：CJK 统一表意符号
	 * Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ：F900-FAFF：CJK 兼容象形文字
	 * Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ：3400-4DBF：CJK
	 * 统一表意符号扩展 A Character.UnicodeBlock.GENERAL_PUNCTUATION ：2000-206F：常用标点
	 * Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ：3000-303F：CJK 符号和标点
	 * Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ：FF00-FFEF：半角及全角形式
	 */

	public static boolean isCHinese(char c) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
				|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION // GENERAL_PUNCTUATION 判断中文的“号
				|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION // CJK_SYMBOLS_AND_PUNCTUATION 判断中文的。号
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS // HALFWIDTH_AND_FULLWIDTH_FORMS 判断中文的，号
		)
			return true;
		return false;
	}

	public static boolean isCHinese(String str) {
		char[] ch = str.toCharArray();
		for (char c : ch) {
			if (isCHinese(c))
				return true;
		}
		return false;
	}

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeInt(SIZE,"The size of key words.", 1, Integer.MAX_VALUE, 5));
        types.add(new ParameterTypeCategory(LANGUAGE, "The language of text.",
                LANGUAGES, 0, false));
        types.add(new ParameterTypeCategory(METHOD, "The method of compute keywords weight.",
                METHODS, 0, false));

        ParameterType type = new ParameterTypeDouble(DAMPING_FACTOR, "The damping factor of text rank.", 0.0001,1, 0.85);
        type.registerDependencyCondition(new EqualStringCondition(this, METHOD, false, "textrank"));
        types.add(type);
        type = new ParameterTypeInt(MAX_ITER, "The maximum number of iterations in textrank.", 1, Integer.MAX_VALUE, 200);
        type.registerDependencyCondition(new EqualStringCondition(this, METHOD, false, "textrank"));
        types.add(type);
        type = new ParameterTypeDouble(MIN_DIFF, "The iteration end error in textrank.",0.0000001,1, 0.001);
        type.registerDependencyCondition(new EqualStringCondition(this, METHOD, false, "textrank"));
        types.add(type);
        type = new ParameterTypeInt(WINDOWS, "The window size of words.",1, Integer.MAX_VALUE, 5);
        type.registerDependencyCondition(new EqualStringCondition(this, METHOD, false, "textrank"));
        types.add(type);
        return types;
    }

}
