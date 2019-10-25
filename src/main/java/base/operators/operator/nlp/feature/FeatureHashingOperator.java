package base.operators.operator.nlp.feature;


import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.MemoryExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.feature.core.FeatureHashing;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeInt;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;

/**
 * 对于给定分好词的文档，产生针对于n-gram文本信息的哈希特征。
 */
public class FeatureHashingOperator extends Operator {
	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
	public static final String HASH_BITE_SIZE = "hash_bite_size";
	public static final String N_GRAM_SIZE = "n_gram_size";

	public FeatureHashingOperator(OperatorDescription description){
		super(description);
		exampleSetInput.addPrecondition(
				new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
						this, (DOC_ATTRIBUTE_NAME))));

	}

	public void doWork() throws OperatorException {
		String doc_attribute_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
		int hashBiteSize = getParameterAsInt(HASH_BITE_SIZE);
		int nGramSize = getParameterAsInt(N_GRAM_SIZE);
		ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
		Attributes attributes = exampleSet.getAttributes();
		Attribute doc_attribute = attributes.get(doc_attribute_name);

		int dimension = (int) (Math.pow(2, hashBiteSize));
		List<Attribute> attributeList = new ArrayList<>();
		Attribute new_doc_attribute = AttributeFactory.createAttribute(doc_attribute_name, doc_attribute.getValueType());
		attributeList.add(new_doc_attribute);
		for (int i = 0; i < dimension; i++) {
			Attribute feature_attribute = AttributeFactory.createAttribute("feature"+i, Ontology.NUMERICAL);
			attributeList.add(feature_attribute);
		}
		MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
		FeatureHashing featureHashing = new FeatureHashing(hashBiteSize, nGramSize);
		for (Example example : exampleSet) {
			String text = example.getValueAsString(doc_attribute);
			int[] feature = featureHashing.getSingleTextFeatureHashing(text);
			DataRowFactory factory = new DataRowFactory(0, '.');
			DataRow dataRow = factory.create(attributeList.size());
			dataRow.set(new_doc_attribute, new_doc_attribute.getMapping().mapString(text));
			for (int j = 1; j < attributeList.size(); j++) {
				dataRow.set(attributeList.get(j), feature[j-1]);
			}
			exampleTable.addDataRow(dataRow);
		}
		ExampleSet exampleSetOut = new SimpleExampleSet(exampleTable);
		exampleSetOutput.deliver(exampleSetOut);
	}

	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the text attribute.", exampleSetInput, false));
		types.add(new ParameterTypeInt(HASH_BITE_SIZE, "The size of hash size.", 1, Integer.MAX_VALUE, 10));
		types.add(new ParameterTypeInt(N_GRAM_SIZE, "The size of gram.", 1, Integer.MAX_VALUE, 2));

		return types;
	}

}
