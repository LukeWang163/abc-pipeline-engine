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
package base.operators.tools.expression.internal;

import java.util.LinkedList;
import java.util.List;

import base.operators.tools.expression.Constant;
import base.operators.tools.expression.ExpressionParserModule;
import base.operators.tools.expression.Function;
import base.operators.tools.expression.internal.function.basic.Divide;
import base.operators.tools.expression.internal.function.basic.Minus;
import base.operators.tools.expression.internal.function.basic.Modulus;
import base.operators.tools.expression.internal.function.basic.Multiply;
import base.operators.tools.expression.internal.function.basic.Plus;
import base.operators.tools.expression.internal.function.basic.Power;
import base.operators.tools.expression.internal.function.comparison.Equals;
import base.operators.tools.expression.internal.function.comparison.GreaterEqualThan;
import base.operators.tools.expression.internal.function.comparison.GreaterThan;
import base.operators.tools.expression.internal.function.comparison.LessEqualThan;
import base.operators.tools.expression.internal.function.comparison.LessThan;
import base.operators.tools.expression.internal.function.comparison.NotEquals;
import base.operators.tools.expression.internal.function.logical.And;
import base.operators.tools.expression.internal.function.logical.Not;
import base.operators.tools.expression.internal.function.logical.Or;


/**
 * Singleton that holds the standard operations (+,-,*,...).
 *
 * @author Gisa Schaefer
 *
 */
public enum StandardOperations implements ExpressionParserModule {

	INSTANCE;

	private List<Function> standardOperations = new LinkedList<>();

	private StandardOperations() {

		// logical operations
		standardOperations.add(new Not());
		standardOperations.add(new And());
		standardOperations.add(new Or());

		// comparison operations
		standardOperations.add(new Equals());
		standardOperations.add(new NotEquals());
		standardOperations.add(new LessThan());
		standardOperations.add(new GreaterThan());
		standardOperations.add(new LessEqualThan());
		standardOperations.add(new GreaterEqualThan());

		// basic operations
		standardOperations.add(new Plus());
		standardOperations.add(new Minus());
		standardOperations.add(new Multiply());
		standardOperations.add(new Divide());
		standardOperations.add(new Power());
		standardOperations.add(new Modulus());
	}

	@Override
	public String getKey() {
		return "";
	}

	@Override
	public List<Constant> getConstants() {
		return null;
	}

	@Override
	public List<Function> getFunctions() {
		return standardOperations;
	}

}
