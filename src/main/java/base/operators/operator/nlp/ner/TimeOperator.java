package base.operators.operator.nlp.ner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.ner.time.DateUtil;
import base.operators.operator.nlp.ner.time.TimeAnnotate;
import base.operators.operator.nlp.ner.time.TimeUnit;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;

public class TimeOperator extends Operator {
	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
	public static final String FUTURE_TREND_IDENTIFY = "future_trend_identify";

	public TimeOperator(OperatorDescription description){
		super(description);
		try {
			String[] names = getParameterAsString(DOC_ATTRIBUTE_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
			for (int i = 0; i < names.length; i++) {
				exampleSetInput.addPrecondition(
						new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
								this, names[i])));
			}

		} catch (UndefinedParameterError undefinedParameterError) {
			undefinedParameterError.printStackTrace();
		}

	}

	@Override
	public void doWork() throws OperatorException {
		String[] doc_column = getParameterAsString(DOC_ATTRIBUTE_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
		boolean future_trend_identify = getParameterAsBoolean(FUTURE_TREND_IDENTIFY);

		TimeAnnotate normalizer = new TimeAnnotate();
		normalizer.setPreferFuture(future_trend_identify);

		ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
		Attributes attributes = exampleSet.getAttributes();
		ExampleTable exampleTable = exampleSet.getExampleTable();
		for (int l = 0; l < doc_column.length; l++) {
			Attribute new_attribute = AttributeFactory.createAttribute(doc_column[l]+"_time", Ontology.STRING);
			exampleTable.addAttribute(new_attribute);
			attributes.addRegular(new_attribute);
		}

		for (int i = 0; i < exampleSet.size(); i++) {
			Example example = exampleSet.getExample(i);
			for(int j=0; j < doc_column.length; j++) {
				String content = example.getValueAsString(attributes.get(doc_column[j]));
				normalizer.parse(content);
				ArrayList<TimeUnit> unit = normalizer.getTimeUnit();
				JSONArray jsonArray = new JSONArray();
				for (int k = 0; k < unit.size(); k++) {
					JSONObject json = new JSONObject();
					int startIndex = unit.get(k).getStartIndex();
					int endIndex = unit.get(k).getEndIndex();
					json.put("word", unit.get(k).getTimeExpression());
					json.put("time", DateUtil.formatDateDefault(unit.get(k).getTime()));
					json.put("begin", startIndex);
					json.put("end", endIndex);
					jsonArray.add(json);
				}
				if(jsonArray.size() == 0) {
					example.setValue(attributes.get(doc_column[j]+"_time"), attributes.get(doc_column[j]+"_time").getMapping().mapString("[]"));
				} else {
					example.setValue(attributes.get(doc_column[j]+"_time"), attributes.get(doc_column[j]+"_time").getMapping().mapString(jsonArray.toJSONString()));
				}

			}

		}
		exampleSetOutput.deliver(exampleSet);

	}

	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeAttributes(DOC_ATTRIBUTE_NAME, "The name of the document attribute.",exampleSetInput,
				false));
		types.add(new ParameterTypeBoolean(FUTURE_TREND_IDENTIFY, "Future trend identify.", false));
		return types;
	}

}
