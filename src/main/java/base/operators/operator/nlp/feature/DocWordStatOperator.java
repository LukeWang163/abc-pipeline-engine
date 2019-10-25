package base.operators.operator.nlp.feature;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.MemoryExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.feature.core.DocWordStat;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 词频统计
 * 对于批量文档，统计文档中单词的词频。
 *
 */
public class DocWordStatOperator extends Operator {
	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort sequentialVocabExampleSetOutput = getOutputPorts().createPort("sequential vocabulary example set");
	private OutputPort wordFrequencyExampleSetOutput = getOutputPorts().createPort("word frequencyexample set");
	private OutputPort fullVocabExampleSetOutput = getOutputPorts().createPort("full vocabulary example set");

	public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
	public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";

	public DocWordStatOperator(OperatorDescription description){
		super(description);

		exampleSetInput.addPrecondition(
				new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
						this, ID_ATTRIBUTE_NAME, DOC_ATTRIBUTE_NAME)));

	}

	@Override
	public void doWork() throws OperatorException {
		String doc_attribute_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
		String id_attribute_name = getParameterAsString(ID_ATTRIBUTE_NAME);
		ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
		Attributes attributes = exampleSet.getAttributes();
		Attribute id_attribute = attributes.get(id_attribute_name);
		Attribute doc_attribute = attributes.get(doc_attribute_name);
		DocWordStat docWordStat = new DocWordStat(exampleSet, id_attribute, doc_attribute);
		//顺序词表结果exampleset
		Map<String, List<String>> orderWordResult = docWordStat.orderWordResult;
		List<Attribute> orderAttributeList = new ArrayList<>();
		Attribute order_id_attribute = AttributeFactory.createAttribute(id_attribute_name, id_attribute.getValueType());
		orderAttributeList.add(order_id_attribute);
		Attribute order_word_attribute = AttributeFactory.createAttribute("word", Ontology.STRING);
		orderAttributeList.add(order_word_attribute);
		MemoryExampleTable orderExampleTable = new MemoryExampleTable(orderAttributeList);
		for (Map.Entry<String, List<String>> idEntry : orderWordResult.entrySet()) {
			for (int i = 0; i < idEntry.getValue().size(); i++) {
				DataRowFactory factory = new DataRowFactory(0, '.');
				DataRow dataRow = factory.create(orderAttributeList.size());
				dataRow.set(order_id_attribute, id_attribute.isNumerical()? Double.parseDouble(idEntry.getKey()): order_id_attribute.getMapping().mapString(idEntry.getKey()));
				dataRow.set(order_word_attribute, order_word_attribute.getMapping().mapString(idEntry.getValue().get(i)));
				orderExampleTable.addDataRow(dataRow);
			}
		}
		ExampleSet sequentialVocabExampleSetOut = new SimpleExampleSet(orderExampleTable);
		sequentialVocabExampleSetOutput.deliver(sequentialVocabExampleSetOut);


		//词频统计结果exampleset
		Map<String, Map<String, Integer>> wordFrequencyResult = docWordStat.wordFrequencyResult;
		List<Attribute> frequencyAttributeList = new ArrayList<>();
		Attribute frequency_id_attribute = AttributeFactory.createAttribute(id_attribute_name, id_attribute.getValueType());
		frequencyAttributeList.add(frequency_id_attribute);
		Attribute frequency_word_attribute = AttributeFactory.createAttribute("word", Ontology.STRING);
		frequencyAttributeList.add(frequency_word_attribute);
		Attribute frequency_count_attribute = AttributeFactory.createAttribute("count", Ontology.NUMERICAL);
		frequencyAttributeList.add(frequency_count_attribute);

		MemoryExampleTable frequencyExampleTable = new MemoryExampleTable(frequencyAttributeList);
		for (Map.Entry<String, Map<String, Integer>> idEntry : wordFrequencyResult.entrySet()) {
			for (Map.Entry<String, Integer> wordCount : wordFrequencyResult.get(idEntry.getKey()).entrySet()){
				DataRowFactory factory = new DataRowFactory(0, '.');
				DataRow dataRow = factory.create(frequencyAttributeList.size());
				dataRow.set(frequency_id_attribute, id_attribute.isNumerical()? Double.parseDouble(idEntry.getKey()): frequency_id_attribute.getMapping().mapString(idEntry.getKey()));
				dataRow.set(frequency_word_attribute, frequency_word_attribute.getMapping().mapString(wordCount.getKey()));
				dataRow.set(frequency_count_attribute, wordCount.getValue());
				frequencyExampleTable.addDataRow(dataRow);
			}
		}
		ExampleSet frequencyExampleSetOut = new SimpleExampleSet(frequencyExampleTable);
		wordFrequencyExampleSetOutput.deliver(frequencyExampleSetOut);

		//全表词典结果exampleset
		Map<Integer, String> fullTableWordVocabulary = docWordStat.fullTableWordVocabulary;
		List<Attribute> vocabAttributeList = new ArrayList<>();
		Attribute vocab_id_attribute = AttributeFactory.createAttribute("word_id", Ontology.NUMERICAL);
		vocabAttributeList.add(vocab_id_attribute);
		Attribute vocab_word_attribute = AttributeFactory.createAttribute("word", Ontology.STRING);
		vocabAttributeList.add(vocab_word_attribute);
		MemoryExampleTable vocabExampleTable = new MemoryExampleTable(vocabAttributeList);
		for (Map.Entry<Integer, String> entry : fullTableWordVocabulary.entrySet()) {
			DataRowFactory factory = new DataRowFactory(0, '.');
			DataRow dataRow = factory.create(vocabAttributeList.size());
			dataRow.set(vocab_id_attribute, entry.getKey());
			dataRow.set(vocab_word_attribute, vocab_word_attribute.getMapping().mapString(entry.getValue()));
			vocabExampleTable.addDataRow(dataRow);
		}
		ExampleSet vocabExampleSetOut = new SimpleExampleSet(vocabExampleTable);
		fullVocabExampleSetOutput.deliver(vocabExampleSetOut);

	}

	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput, false));
		types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the text attribute.", exampleSetInput, false));

		return types;
	}
}

