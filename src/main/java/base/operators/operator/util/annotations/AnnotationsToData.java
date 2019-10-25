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
package base.operators.operator.util.annotations;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.tools.Ontology;
import base.operators.operator.Annotations;
import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDTransformationRule;


/**
 * @author Marius Helf
 *
 */
public class AnnotationsToData extends Operator {

	private static final String ANNOTATION_ATTRIBUTE = "annotation";
	private static final String VALUE_ATTRIBUTE = "value";

	private InputPort inputPort = getInputPorts().createPort("object", IOObject.class);
	private OutputPort annotationsOutputPort = getOutputPorts().createPort("annotations");
	private OutputPort outputPort = getOutputPorts().createPort("object through");

	/**
	 * @param description
	 */
	public AnnotationsToData(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(inputPort, outputPort);
		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				ExampleSetMetaData metaData = new ExampleSetMetaData();
				metaData.addAttribute(new AttributeMetaData(ANNOTATION_ATTRIBUTE, Ontology.POLYNOMINAL, Attributes.ID_NAME));
				metaData.addAttribute(new AttributeMetaData(VALUE_ATTRIBUTE, Ontology.POLYNOMINAL));
				annotationsOutputPort.deliverMD(metaData);
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		IOObject data = inputPort.getData(IOObject.class);
		Annotations annotations = data.getAnnotations();
		Attribute annotationAttr = AttributeFactory.createAttribute(ANNOTATION_ATTRIBUTE, Ontology.POLYNOMINAL);
		Attribute valueAttr = AttributeFactory.createAttribute(VALUE_ATTRIBUTE, Ontology.POLYNOMINAL);

		ExampleSetBuilder builder = ExampleSets.from(annotationAttr, valueAttr).withExpectedSize(annotations.size());

		for (String annotation : annotations.getDefinedAnnotationNames()) {
			double[] rowData = new double[2];
			rowData[0] = annotationAttr.getMapping().mapString(annotation);
			rowData[1] = valueAttr.getMapping().mapString(annotations.getAnnotation(annotation));
			builder.addRow(rowData);
		}

		ExampleSet exampleSet = builder.build();
		exampleSet.getAttributes().setSpecialAttribute(annotationAttr, Attributes.ID_NAME);
		outputPort.deliver(data);
		annotationsOutputPort.deliver(exampleSet);
	}
}
