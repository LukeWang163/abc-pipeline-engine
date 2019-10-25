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
package base.operators.operator.io;

import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.tools.io.Encoding;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.PassThroughRule;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.parameter.ParameterType;

import java.util.LinkedList;
import java.util.List;


/**
 * Superclass of all operators that take a single object as input, save it to disk and return the
 * same object as output. This class is mainly a tribute to the e-LICO DMO.
 * 
 * It defines precondition and a pass through rule for its output port.
 * 
 * @author Simon Fischer
 */
public abstract class AbstractWriter<T extends IOObject> extends Operator {

	private InputPort inputPort = getInputPorts().createPort("input");
	private OutputPort outputPort = getOutputPorts().createPort("through");
	private Class<T> savedClass;

	public AbstractWriter(OperatorDescription description, Class<T> savedClass) {
		super(description);
		this.savedClass = savedClass;
		inputPort.addPrecondition(new SimplePrecondition(inputPort, new MetaData(savedClass)));
		getTransformer().addRule(new PassThroughRule(inputPort, outputPort, false));
	}

	/**
	 * Creates (or reads) the ExampleSet that will be returned by {@link #apply()}.
	 * 
	 * @return the written IOObject itself
	 */
	public abstract T write(T ioobject) throws OperatorException;

	@Override
	public final void doWork() throws OperatorException {
		T ioobject = inputPort.getData(savedClass);
		ioobject = write(ioobject);
		outputPort.deliver(ioobject);
	}

	protected boolean supportsEncoding() {
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.addAll(super.getParameterTypes());
		if (supportsEncoding()) {
			types.addAll(Encoding.getParameterTypes(this));
		}
		return types;
	}
}
