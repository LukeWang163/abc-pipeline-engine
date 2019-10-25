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

import java.io.Serializable;


/**
 * Interface for a product constraint that should restrict access to a product feature. </br></br>
 *
 * The constraint contains a default value that will be used as constraint value for default
 * licenses. The default value can be <code>null</code> if using the feature with the default
 * license should not be restricted at all. </br></br>
 *
 * It is recommended to extend {@link AbstractConstraint} to create a new {@link Constraint} for a
 * {@link Product}.
 *
 * @param <S>
 *            the class a constraint is stored with
 * @param <C>
 *            the class of the checked value
 *
 * @author Nils Woehler
 *
 */
public interface Constraint<S, C> extends Serializable {

	/**
	 * Checks whether the checked value is allowed by the constraint.
	 *
	 * @param constraint
	 *            the current constraint. If it is <code>null</code>, <code>false</code> will be
	 *            returned.
	 * @param checkedValue
	 *            the value that is being checked. If it is <code>null</code>, <code>false</code>
	 *            will be returned.
	 * @return <code>true</code> if checked value is allowed, <code>false</code> if value violates
	 *         the constraint.
	 */
	boolean isAllowed(S constraint, C checkedValue);

	/**
	 * @return the constraint value the should be used by the default license. Can be
	 *         <code>null</code> if using the feature without a valid license is forbidden.
	 */
	S getDefaultValue();

	/**
	 * @return the constraint key which is used to get the constraint value from the currently
	 *         installed license
	 */
	String getKey();

	/**
	 * Used to transform the string representation of a constraint stored in a license file to the
	 * actual class.
	 *
	 * @return the constraint transformed from its string representation
	 *
	 * @param stringConstraint
	 *            the constraint in string representation loaded from the license file
	 */
	S transformFromString(String stringConstraint);

	/**
	 * Transforms the constraint to its {@link String} representation which will be stored in the
	 * license file.
	 *
	 * @return the string representation of the constraint value
	 *
	 * @param constraint
	 *            the constraint to be transformed
	 */
	String transformToString(S constraint);

	/**
	 * Is called before calling {@link #isAllowed(Object, Object)} to transform a checked value from
	 * its {@link String} representation to the actual checked value class. Mainly used for newly
	 * introduced license annotations which are only capable of storing checked values as
	 * {@link String}.
	 *
	 * @since 2.0.0
	 *
	 * @param checkedValue
	 *            the checked constraint value as String
	 * @return the checked constraint value as actual class
	 */
	C transformValueFromString(String checkedValue);

}
