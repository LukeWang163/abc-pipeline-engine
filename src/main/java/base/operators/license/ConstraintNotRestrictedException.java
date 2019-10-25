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
 * An exception that is thrown if the constraint is not restricted by the current license.
 *
 * @author Nils Woehler
 */
public class ConstraintNotRestrictedException extends Exception {

	private final Constraint<?, ?> constraint;

	private static final long serialVersionUID = 1L;

	/**
	 * Thrown by the {@link LicenseManager} if a constraint cannot be found in a product license.
	 *
	 * @param constraint
	 *            the {@link Constraint} that is not restricted
	 */
	public ConstraintNotRestrictedException(Constraint<?, ?> constraint) {
		this.constraint = constraint;
	}

	/**
	 * @return the constraint
	 */
	public Constraint<?, ?> getConstraint() {
		return this.constraint;
	}

}
