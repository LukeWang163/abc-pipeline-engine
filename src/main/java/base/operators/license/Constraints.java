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
package base.operators.license;

import base.operators.license.product.Constraint;


/**
 * An interface for a container that stores constraint values for {@link Constraint}s.
 *
 * @author Nils Woehler
 *
 */
public interface Constraints {

	/**
	 * @param constraint
	 *            the constraint which should be used to fetch the constraint value
	 *
	 * @return the constraint value as string. Can be <code>null</code> which means the feature is
	 *         restricted without a license.
	 *
	 * @throws ConstraintNotRestrictedException
	 *             thrown if no value is stored for the specified constraint
	 */
	String getConstraintValue(Constraint<?, ?> constraint) throws ConstraintNotRestrictedException;

	/**
	 * @return a deep copy of the {@link Constraints} object
	 */
	Constraints copy();

}
