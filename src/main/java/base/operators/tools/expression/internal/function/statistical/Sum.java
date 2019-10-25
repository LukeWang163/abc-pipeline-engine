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
package base.operators.tools.expression.internal.function.statistical;

import base.operators.tools.Ontology;
import base.operators.tools.expression.FunctionDescription;
import base.operators.tools.expression.internal.function.AbstractArbitraryDoubleInputFunction;
import org.apache.commons.math3.stat.StatUtils;


/**
 *
 * A {@link Function} for sum.
 *
 * @author David Arnu
 *
 */
public class Sum extends AbstractArbitraryDoubleInputFunction {

	/**
	 * Constructs an sum function.
	 */
	public Sum() {
		super("statistical.sum", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, Ontology.NUMERICAL);

	}

	@Override
	public double compute(double... values) {
		return StatUtils.sum(values);
	}
}
