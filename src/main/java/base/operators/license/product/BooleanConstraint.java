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
 * A boolean constraint checks whether the constraint is violated by checking if a value is
 * {@code true}.
 *
 * @author Nils Woehler
 */
public class BooleanConstraint extends AbstractConstraint<Boolean, Boolean> {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a {@link BooleanConstraint} which is forbidden by default
	 *
	 * @param constraintId
	 *            the constraint ID
	 */
	public BooleanConstraint(String constraintId) {
		super(constraintId, Boolean.FALSE);
	}

	/**
	 * @param constraintId
	 *            the constraint ID
	 * @param defaultValue
	 *            the default value, {@code Boolean.TRUE} allows usage without a license whereas
	 *            {@code Boolean.FALSE} requires a license which permits this constraint
	 */
	public BooleanConstraint(String constraintId, Boolean defaultValue) {
		super(constraintId, Boolean.FALSE);
	}

	@Override
	public Boolean transformFromString(String stringConstraint) {
		return Boolean.parseBoolean(stringConstraint);
	}

	@Override
	public String transformToString(Boolean constraint) {
		return String.valueOf(constraint);
	}

	@Override
	protected boolean checkConstraint(Boolean constraintValue, Boolean checkedValue) {
		return constraintValue.equals(checkedValue);
	}

	@Override
	public Boolean transformValueFromString(String checkedValue) {
		return transformFromString(checkedValue);
	}

}
