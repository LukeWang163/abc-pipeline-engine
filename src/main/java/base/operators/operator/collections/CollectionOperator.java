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
package base.operators.operator.collections;

import base.operators.operator.Operator;
import base.operators.operator.IOObject;
import base.operators.operator.IOObjectCollection;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.InputPortExtender;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.CollectionMetaData;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;

import java.util.List;


/**
 * Takes several objects as an input at converts them into a collection.
 * 
 * @author Simon Fischer
 */
public class CollectionOperator extends Operator {

	public static final String PARAMETER_UNFOLD = "unfold";

	private final InputPortExtender inExtender = new InputPortExtender("input", getInputPorts());
	private final OutputPort collectionOutput = getOutputPorts().createPort("collection");

	public InputPortExtender getInExtender() {
		return inExtender;
	}

	public CollectionOperator(OperatorDescription description) {
		super(description);
		inExtender.start();
		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				boolean unfold = getParameterAsBoolean(PARAMETER_UNFOLD);
				MetaData commonSupertype = null;
				for (InputPort in : inExtender.getManagedPorts()) {
					MetaData md = in.getMetaData();
					if (unfold && (md instanceof CollectionMetaData)) {
						md = ((CollectionMetaData) md).getElementMetaDataRecursive();
					}
					if (md == null) {
						continue;
					} else if (commonSupertype == null) {
						commonSupertype = md;
						continue;
					} else {
						if (commonSupertype.getObjectClass().equals(md.getObjectClass())) {
							continue;
						} else if (commonSupertype.getObjectClass().isAssignableFrom(md.getObjectClass())) {
							commonSupertype = md;
						} else if (md.getObjectClass().isAssignableFrom(commonSupertype.getObjectClass())) {
							// noop, old value was ok
							// commonSupertype = commonSupertype;
						} else {
							in.addError(new SimpleMetaDataError(Severity.WARNING, in, "incompatible_ioobjects",
									commonSupertype.getObjectClass().getSimpleName(), md.getObjectClass().getSimpleName()));
							collectionOutput.deliverMD(new CollectionMetaData(new MetaData()));
							return;
						}
					}
				}
				collectionOutput.deliverMD(new CollectionMetaData(commonSupertype));
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		List<IOObject> list = inExtender.getData(IOObject.class, getParameterAsBoolean(PARAMETER_UNFOLD));
		collectionOutput.deliver(new IOObjectCollection<IOObject>(list));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_UNFOLD,
				"Determines whether collections received at the input ports are unfolded.", false));
		return types;
	}
}
