/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package base.operators.operator.nlp.segment;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.*;
import base.operators.operator.nlp.segment.kshortsegment.predict.segment.Segment;
import base.operators.operator.nlp.segment.kshortsegment.predict.segment.TokenizeAnnotator;
import base.operators.operator.nlp.segment.kshortsegment.training.NatureDictionaryMaker;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.*;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.tools.Ontology;

import java.util.List;


public class KShortSegmentModelApplier extends Operator {

    private final InputPort exampleSetInput = getInputPorts().createPort("unsegment data");
	private final InputPort modelInput = getInputPorts().createPort("user model");
	private final InputPort dictExampleSetInput = getInputPorts().createPort("user extra dictionary");
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("segment data");
	private final OutputPort modelOutput = getOutputPorts().createPort("model");

	public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
	public static final String WORD_ATTRIBUTE_NAME = "word_attribute_name";
	public static final String POS_ATTRIBUTE_NAME = "pos_attribute_name";
	public static final String IS_POS = "whether_part_of_speech_tagging_or_not";

	public KShortSegmentModelApplier(OperatorDescription description) {
		super(description);
		modelInput.addPrecondition(
				new SimplePrecondition(modelInput, new ModelMetaData(Model.class, new ExampleSetMetaData())));
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new ExampleSetMetaData()));
		getTransformer().addRule(new ModelApplicationRule(exampleSetInput, exampleSetOutput, modelInput, false));
		getTransformer().addRule(new PassThroughRule(modelInput, modelOutput, false));
	}

	/**
	 * Applies the operator and labels the {@link ExampleSet}. The example set in the input is not
	 * consumed.
	 */
	@Override
	public void doWork() throws OperatorException {
		ExampleSet inputExampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
		ExampleSet dictInputExampleSet = null;
		String word_name = null;
		String pos_name = null;
		NatureDictionaryMaker model = null;
		if(dictExampleSetInput.isConnected()){
			dictInputExampleSet = dictExampleSetInput.getData(ExampleSet.class);
			word_name = getParameterAsString(WORD_ATTRIBUTE_NAME);
			pos_name = getParameterAsString(POS_ATTRIBUTE_NAME);
		}
		if(modelInput.isConnected()){
			model = (NatureDictionaryMaker)modelInput.getData(Model.class);
		}
		String doc_name = getParameterAsString(DOC_ATTRIBUTE_NAME);

		boolean isPos = getParameterAsBoolean(IS_POS);

		ExampleTable exampleTable = inputExampleSet.getExampleTable();
		Attribute new_attribute = AttributeFactory.createAttribute(doc_name+"_seg", Ontology.STRING);
		exampleTable.addAttribute(new_attribute);
		inputExampleSet.getAttributes().addRegular(new_attribute);

		Segment segment = new TokenizeAnnotator(model, dictInputExampleSet, word_name, pos_name);
		for(Example example : inputExampleSet){
			String docContent = example.getValueAsString(inputExampleSet.getAttributes().get(doc_name));
			String result = segment.segment(docContent, isPos);
			example.setValue(new_attribute, new_attribute.getMapping().mapString(result));
		}

		exampleSetOutput.deliver(inputExampleSet);
		modelOutput.deliver(model);
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == modelOutput) {
			return getParameterAsBoolean("keep_model");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the attribute to convert.", exampleSetInput,
				false));
		types.add(new ParameterTypeAttribute(WORD_ATTRIBUTE_NAME, "The attribute name of the word in dictionary.", dictExampleSetInput,
				true));
		types.add(new ParameterTypeAttribute(POS_ATTRIBUTE_NAME, "The attribute name of the part of speech and frequency in dictionary.", dictExampleSetInput,
				true));
		types.add(new ParameterTypeBoolean(IS_POS, "Whether part of speech tagging or not.", false, false));
		return types;
	}
}
