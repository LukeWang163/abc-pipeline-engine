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
package base.operators.tools.expression.internal.function.conversion;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Callable;

import base.operators.tools.Ontology;
import base.operators.tools.expression.*;
import base.operators.tools.expression.internal.SimpleExpressionEvaluator;
import base.operators.tools.expression.internal.function.AbstractFunction;
import base.operators.tools.expression.DoubleCallable;
import base.operators.tools.expression.ExpressionEvaluator;
import base.operators.tools.expression.ExpressionParsingException;
import base.operators.tools.expression.ExpressionType;
import base.operators.tools.expression.FunctionDescription;
import base.operators.tools.expression.FunctionInputException;


/**
 *
 * A {@link Function} parsing a string or a number to a date.
 *
 * @author Marcel Seifert
 *
 */
public class DateParse extends AbstractFunction {

	/**
	 * Constructs an AbstractFunction with {@link FunctionDescription} generated from the arguments
	 * and the function name generated from the description.
	 *
	 */
	public DateParse() {
		super("conversion.date_parse", 1, Ontology.DATE);
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 1) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), 1,
					inputEvaluators.length);
		}
		ExpressionType type = getResultType(inputEvaluators);

		ExpressionEvaluator input = inputEvaluators[0];

		return new SimpleExpressionEvaluator(type, makeDateCallable(input), isResultConstant(inputEvaluators));
	}

	/**
	 * Builds a Date Callable from one String or one double input argument
	 *
	 * @param inputEvaluator
	 *            the input
	 * @return the resulting callable<String>
	 */
	protected Callable<Date> makeDateCallable(final ExpressionEvaluator inputEvaluator) {

		final Callable<String> funcString = inputEvaluator.getType() == ExpressionType.STRING ? inputEvaluator
				.getStringFunction() : null;

		final DoubleCallable funcDouble = inputEvaluator.getType() != ExpressionType.STRING ? inputEvaluator
						.getDoubleFunction() : null;

		try {
			if (funcString != null) {
				if (inputEvaluator.isConstant()) {
					final Date result = compute(funcString.call());
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return result;
						}
					};
				} else {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(funcString.call());
						}
					};
				}
			} else {
				if (inputEvaluator.isConstant()) {
					final Date result = compute(funcDouble.call());
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return result;
						}
					};
				} else {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(funcDouble.call());
						}
					};
				}

			}
		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}
	}

	/**
	 * Computes the result for one input double value.
	 *
	 * @param value
	 *            the input timestamp
	 *
	 * @return the result of the computation.
	 */
	protected Date compute(double value) {

		if (Double.isNaN(value)) {
			return null;
		}

		long dateLong = (long) value;
		Date date = new Date(dateLong);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.getTime();
	}

	/**
	 * Computes the result for one input string value.
	 *
	 * @param value
	 *            the input date string
	 *
	 * @return the result of the computation
	 */
	protected Date compute(String dateString) {
		if (dateString == null) {
			return null;
		}
		Date date;
		try {
			// clone because getDateInstance uses an internal pool which can return the
			// same instance for multiple threads
			date = ((DateFormat) DateFormat.getDateInstance(DateFormat.SHORT).clone()).parse(dateString);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			return date;
		} catch (ParseException e) {
			throw new FunctionInputException("expression_parser.invalid_argument.date", getFunctionName(), dateString);
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType input = inputTypes[0];
		if (input == ExpressionType.STRING || input == ExpressionType.DOUBLE || input == ExpressionType.INTEGER) {
			return ExpressionType.DATE;
		} else {
			throw new FunctionInputException("expression_parser.function_wrong_type_two", getFunctionName(), "nominal",
					"numerical");
		}
	}

}
