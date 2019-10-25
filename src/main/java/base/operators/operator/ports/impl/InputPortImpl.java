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
package base.operators.operator.ports.impl;

import base.operators.Process;
import base.operators.operator.DebugMode;
import base.operators.operator.IOObject;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.InputPorts;
import base.operators.operator.ports.Port;
import base.operators.operator.ports.Ports;
import base.operators.operator.ports.metadata.MetaData;


/**
 * The default implementation of an {@link InputPort}
 * 
 * @author Simon Fischer, Sebastian Land
 * 
 */
public class InputPortImpl extends AbstractInputPort {

	/** Use the factory method {@link InputPorts#createPort()} to create InputPorts. */
	protected InputPortImpl(Ports<? extends Port> owner, String name, boolean simulatesStack) {
		super(owner, name, simulatesStack);
	}

	@Override
	public void receive(IOObject object) {
		setData(object);

		Process process = getPorts().getOwner().getOperator().getProcess();
		if ((process != null) && (process.getDebugMode() == DebugMode.COLLECT_METADATA_AFTER_EXECUTION)) {
			if (object == null) {
				setRealMetaData(null);
			} else {
				MetaData forIOObject = MetaData.forIOObject(object);
				setRealMetaData(forIOObject);
			}
		} else {
			setRealMetaData(null);
		}
	}

}
