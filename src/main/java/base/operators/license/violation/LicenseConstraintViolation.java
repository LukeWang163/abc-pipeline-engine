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
package base.operators.license.violation;

import base.operators.license.License;
import base.operators.license.product.Constraint;


/**
 * The constraint violation is returned by the license manager if the constraint value stored in a
 * license is violated.
 *
 * @param <S>
 *            the class a constraint is stored with
 *
 * @param <C>
 *            the class of the checked value
 *
 * @since 2.0.0
 *
 * @author Nils Woehler
 *
 */
public class LicenseConstraintViolation<S, C> implements LicenseViolation {

	private final Constraint<S, C> constraint;
	private final S constraintValue;
	private final C violatingValue;
	private final License license;
	private final String i18nKey;

	/**
	 * Creates a new {@link LicenseConstraintViolation}.
	 *
	 * @param license
	 *            the license which is violated
	 * @param constraint
	 *            the constraint which is violated
	 * @param constraintValue
	 *            the constraint value which is violated
	 * @param violatingValue
	 *            the violating constraint value
	 */
	public LicenseConstraintViolation(License license, Constraint<S, C> constraint, S constraintValue, C violatingValue) {
		this(license, constraint, constraintValue, violatingValue, null);
	}

	/**
	 * Creates a new {@link LicenseConstraintViolation}.
	 *
	 * @param license
	 *            the license which is violated
	 * @param constraint
	 *            the constraint which is violated
	 * @param constraintValue
	 *            the constraint value which is violated
	 * @param violatingValue
	 *            the violating constraint value
	 * @param i18nKey
	 *            the specified I18N keys
	 */
	public LicenseConstraintViolation(License license, Constraint<S, C> constraint, S constraintValue, C violatingValue,
			String i18nKey) {
		this.license = license;
		this.constraint = constraint;
		this.constraintValue = constraintValue;
		this.violatingValue = violatingValue;
		this.i18nKey = i18nKey;
	}

	/**
	 * @return the constraint
	 */
	public Constraint<S, C> getConstraint() {
		return this.constraint;
	}

	/**
	 * @return the constraint value defined by the current license
	 */
	public S getConstraintValue() {
		return this.constraintValue;
	}

	/**
	 * @return the violatingValue
	 */
	public C getViolatingValue() {
		return this.violatingValue;
	}

	@Override
	public ViolationType getViolationType() {
		return ViolationType.LICENSE_CONSTRAINT_VIOLATED;
	}

	@Override
	public License getLicense() {
		return license;
	}

	@Override
	public String getI18nKey() {
		return i18nKey;
	}

}
