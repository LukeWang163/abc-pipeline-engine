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
 * An abstract constraint which is handling the case of missing licenses by comparing the checked
 * value with the value returned by {@link #getDefaultValue()}.
 *
 * @param <C>
 *            the class a constraint is stored with
 *
 * @param <L>
 *            the class of the checked value
 *
 * @author Nils Woehler
 *
 */
public abstract class AbstractConstraint<C, L> implements Constraint<C, L> {

	private static final long serialVersionUID = 1L;

	private final C defaultConstraintValue;
	private final String featureId;

	protected AbstractConstraint(String constraintId, C defaultConstraintValue) {
		this.featureId = constraintId;
		this.defaultConstraintValue = defaultConstraintValue;
	}

	@Override
	public C getDefaultValue() {
		return defaultConstraintValue;
	}

	@Override
	public String getKey() {
		return featureId;
	}

	@Override
	public final boolean isAllowed(C constraintValue, L checkedValue) {

		// if checked value is null, the constraint is violated
		if (checkedValue == null) {
			return false;
		}

		// in case it is null the feature is not permitted
		if (constraintValue == null) {
			return false;
		} else {
			// check if checked value violates the constraint value
			return checkConstraint(constraintValue, checkedValue);
		}
	}

	/**
	 * Checks if checked value is allowed (<code>true</code>) or if it violates the constraint (
	 * <code>false</code>). Both input values cannot be <code>null</code>.
	 */
	protected abstract boolean checkConstraint(C constraintValue, L checkedValue);

}
