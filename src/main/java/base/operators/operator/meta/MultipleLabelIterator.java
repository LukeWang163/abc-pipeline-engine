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
package base.operators.operator.meta;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.CollectingPortPairExtender;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.parameter.UndefinedParameterError;


/**
 * Performs the inner operator for all label attributes, i.e. special attributes whose role name
 * starts with &quot;label&quot;. In each iteration one of the multiple labels is used as label. The
 * results of the inner operators are collected and returned. The example set will be consumed
 * during the iteration.
 *
 * @author Ingo Mierswa
 */
public class MultipleLabelIterator extends OperatorChain {

	private final InputPort exampleSetInput = getInputPorts().createPort("example set");
	private final OutputPort exampleInnerSource = getSubprocess(0).getInnerSources().createPort("example set");
	CollectingPortPairExtender outExtender = new CollectingPortPairExtender("out", getSubprocess(0).getInnerSinks(),
			getOutputPorts());

	public MultipleLabelIterator(OperatorDescription description) {
		super(description, "Iteration");
		outExtender.start();
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleInnerSource, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				AttributeMetaData myLabel = metaData.getLabelMetaData();
				if (myLabel != null) {
					metaData.removeAttribute(myLabel);
				}
				for (AttributeMetaData amd : metaData.getAllAttributes()) {
					if (amd.getName().startsWith(Attributes.LABEL_NAME)) {
						amd.setRole(Attributes.LABEL_NAME);
						break;
					}
				}
				return metaData;
			}
		});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(outExtender.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		Attribute[] labels = getLabels(exampleSet);
		if (labels.length == 0) {
			throw new UserError(this, 105);
		}

		// initProgressListener
		getProgress().setTotal(labels.length);
		getProgress().setCheckForStop(false);

		outExtender.reset();
		for (int i = 0; i < labels.length; i++) {
			ExampleSet cloneSet = (ExampleSet) exampleSet.clone();
			cloneSet.getAttributes().setLabel(labels[i]);
			exampleInnerSource.deliver(cloneSet);

			getSubprocess(0).execute();
			outExtender.collect();
			inApplyLoop();
			getProgress().step();
		}
		getProgress().complete();
	}

	private Attribute[] getLabels(ExampleSet exampleSet) {
		List<Attribute> attributes = new LinkedList<Attribute>();
		Iterator<AttributeRole> i = exampleSet.getAttributes().specialAttributes();
		while (i.hasNext()) {
			AttributeRole role = i.next();
			String name = role.getSpecialName();
			if (name.startsWith(Attributes.LABEL_NAME)) {
				attributes.add(role.getAttribute());
			}
		}
		Attribute[] result = new Attribute[attributes.size()];
		attributes.toArray(result);
		return result;
	}
}
