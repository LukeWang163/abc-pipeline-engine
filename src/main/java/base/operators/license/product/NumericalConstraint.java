/**
 * Copyright (c) 2013-2018, RapidMiner GmbH, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library.
 */
package base.operators.license.product;

/**
 * A numerical constraint checks whether the constraint is violated by checking if a value is less
 * or equal (<=) to the allowed value. If it is greater than the allowed value, the constraint is
 * violated.
 *
 * @author Nils Woehler
 */
public class NumericalConstraint extends AbstractConstraint<Integer, Integer> {

	private static final long serialVersionUID = 1L;

	/**
	 * @param constraintId
	 *            the constraint ID
	 * @param defaultValue
	 *            The value that will be used by the default license. Can be <code>null</code> if
	 *            the checked feature should be forbidden by the default license.
	 */
	public NumericalConstraint(String constraintId, Integer defaultValue) {
		super(constraintId, defaultValue);
	}

	@Override
	public Integer transformFromString(String stringConstraint) {
		return Integer.parseInt(stringConstraint);
	}

	@Override
	public String transformToString(Integer constraints) {
		return String.valueOf(constraints);
	}

	@Override
	protected boolean checkConstraint(Integer constraintValue, Integer checkedValue) {
		return checkedValue <= constraintValue;
	}

	@Override
	public Integer transformValueFromString(String checkedValue) {
		return Integer.valueOf(checkedValue);
	}

}
