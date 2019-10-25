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
package base.operators.parameter;

import base.operators.operator.Operator;

/**
 * A parameter type for parameters which stands for attribute description files (RapidMiner XML
 * descriptions). Operators ask for the selected file with
 * {@link Operator#getParameterAsFile(String)}.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class ParameterTypeAttributeFile extends ParameterTypeFile {

	private static final long serialVersionUID = 4929969388911989038L;

	public ParameterTypeAttributeFile(String key, String description, boolean optional) {
		super(key, description, "aml", optional);
	}

	public ParameterTypeAttributeFile(String key, String description, String defaultFileName) {
		super(key, description, "aml", defaultFileName);
	}

	@Override
	public String getRange() {
		return "attribute filename";
	}
}
