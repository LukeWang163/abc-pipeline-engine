package base.operators.operator.nlp.ner;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nio.file.FileObject;
import base.operators.operator.nlp.ner.location.LocAnnotate;
import base.operators.operator.nlp.ner.location.LocationWrapper;
import base.operators.operator.nlp.ner.location.NSDictionary;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationOperator extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private InputPort modelInput = getInputPorts().createPort("user dictionary");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	static NSDictionary DICT;

	public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
	public static final String DICTIONARY_MODE = "dictionary_mode";
	public static String[] MODES_EN = {"system dictionary","user dictionary","merge dictionary"};
	public static String[] MODES_CH = {"系统词典","自定义词典","合并词典"};

	public LocationOperator(OperatorDescription description){
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
		Attributes attributes = exampleSet.getAttributes();

		String[] doc_column = getParameterAsString(DOC_ATTRIBUTE_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
		int mode = getParameterAsInt(DICTIONARY_MODE);
		InputStream templateModelStream = null;
		if(modelInput.isConnected()){
			// 获取自定义词典
			if(!(mode==0)) {
				//读取输入词典
				templateModelStream = modelInput.getData(FileObject.class).openStream();//模板文件流
			}
		}
		DICT = NSDictionary.getIstance(mode, templateModelStream);

		ExampleTable exampleTable = exampleSet.getExampleTable();
		for (int l = 0; l < doc_column.length; l++) {
			if(!"".equals(doc_column[l])){
				Attribute new_attribute = AttributeFactory.createAttribute(doc_column[l]+"_location", Ontology.STRING);
				exampleTable.addAttribute(new_attribute);
				attributes.addRegular(new_attribute);
			}
		}

		for (int i = 0; i < exampleSet.size(); i++) {
			Example example = exampleSet.getExample(i);
			for (int j = 0; j < doc_column.length; j++) {
				if(!"".equals(doc_column[j])){
					String content = example.getValueAsString(attributes.get(doc_column[j]));
					ArrayList<LocationWrapper> outSetNew = LocationOperator.annotate(content, mode, templateModelStream);
					List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
					if(outSetNew == null) {
						example.setValue(attributes.get(doc_column[j]+"_location") , attributes.get(doc_column[j]+"_location").getMapping().mapString("[]"));
					} else {
						for(int k = 0; k < outSetNew.size(); k++){
							Map<String, Object> map = new HashMap<String, Object>();
							LocationWrapper loc=outSetNew.get(k);
							loc.getWord();//合并后的词
							loc.getStartIndex();//合并后的词开始下标
							loc.getEndIndex();//合并后的词结束下标
							loc.getLocation().getName();//原子词
							loc.getLocation().getLevel();//末级地址级别
							loc.getLocation().getLongtitudeWGS();

							loc.getLocationPath();//词的上级节点路径，从自己到最上级依次从前往后，第0个元素是自己
							map.put("word", loc.getWord());
							map.put("index", loc.getStartIndex());
							map.put("level", loc.getLocation().getLevel());
							map.put("longtitude", loc.getLocation().getLongtitudeGCJ());
							map.put("latitude", loc.getLocation().getLatitudeGCJ());
							list.add(map);
						}
						example.setValue(attributes.get(doc_column[j]+"_location") , attributes.get(doc_column[j]+"_location").getMapping().mapString(list.toString()));
					}
				}
			}
		}
		exampleSetOutput.deliver(exampleSet);
	}

	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttributes(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput,
				false));
		types.add(new ParameterTypeCategory(DICTIONARY_MODE, "The mode of dictionary using.",
				MODES_EN, 0, false));
		return types;
	}

	public static ArrayList<LocationWrapper> annotate(String text, int type, InputStream stream) {
		if (text == null || "".equals(text)) {
			return null;
		}

		int length = text.codePointCount(0, text.length());
		List<Integer> commonPrefixList = new ArrayList<Integer>();
		String subString = null;
		// 判断是否有扩展字符
		boolean hasExtendedCharset = hasExtendedCharset(text);
		// 存储所有字符位的词典匹配结果
		ArrayList<List<Integer>> prexIdArrayList = new ArrayList<List<Integer>>();
		// 按字符移位挨个进行匹配
		for (int i = 0; i < length; i++) {
			if (hasExtendedCharset) {// 如果包含有扩展字符
				subString = getSubString(text, length, i);
			} else {
				subString = text.substring(i);
			}
			commonPrefixList = DICT.getLocTrie().commonPrefixSearch(subString);// 判断是否有前缀（判断该字符串是否有根节点）
			//System.out.println("   subString:"+subString+"\tcommonPrefixList:"+commonPrefixList);
			// 不管本字符开始位置是否匹配成功，匹配结果均按字符位置下标存储
			prexIdArrayList.add(i, commonPrefixList);
		}
		//System.out.println("Factory create instance begin.....");
		LocAnnotate an = new LocAnnotate(prexIdArrayList, length,
				hasExtendedCharset, type, stream);
		//System.out.println("Factory annotate  begin.....");
		return an.annotate(type, stream);

	}

	/**
	 * 检测句子中是否含有Unicode扩展字符集
	 */
	private static boolean hasExtendedCharset(String sentence) {
		if (sentence == null)
			return false;
		boolean hasExtendedCharset = false;
		// int codePointCount = sentence.length();
		if (sentence.length() != sentence.codePointCount(0, sentence.length())) {
			hasExtendedCharset = true;
			// codePointCount = sentence.codePointCount(0, sentence.length());
		}
		return hasExtendedCharset;
	}

	/**
	 * 截取子字符串
	 *
	 * @param sentence
	 * @param codePointCount
	 * @param offset
	 * @return
	 */
	public static String getSubString(String sentence, int codePointCount,
									  int offset) {
		StringBuilder sb = new StringBuilder();
		for (int i = offset; i < codePointCount; i++) {
			int index = sentence.offsetByCodePoints(0, i);
			int cpp = sentence.codePointAt(index);
			sb.appendCodePoint(cpp);
		}
		return sb.toString();
	}

}
