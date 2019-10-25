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
import base.operators.license.annotation.LicenseLevel;


/**
 * A license violation that is emitted if the license level of a {@link LicenseLevel} annotation is
 * not fulfilled.
 *
 * @since 2.0.0
 *
 * @author Nils Woehler
 *
 */
public class LicenseLevelViolation implements LicenseViolation {

	private final LicenseLevel licenseAnnotation;
	private final License license;
	private final Object violatingObject;

	/**
	 * @param obj
	 *            the violating object
	 * @param license
	 *            the violated license
	 * @param licenseAnnotation
	 *            the violated {@link LicenseLevel} annotation
	 */
	public LicenseLevelViolation(Object obj, License license, LicenseLevel licenseAnnotation) {
		this.violatingObject = obj;
		this.license = license;
		this.licenseAnnotation = licenseAnnotation;
	}

	/**
	 * @return the license annotation which caused the constraint
	 */
	public LicenseLevel getLicenseAnnotation() {
		return licenseAnnotation;
	}

	@Override
	public ViolationType getViolationType() {
		return ViolationType.LICENSE_LEVEL_VIOLATED;
	}

	@Override
	public License getLicense() {
		return license;
	}

	/**
	 * @return the violating object
	 */
	public Object getViolatingObject() {
		return violatingObject;
	}

	@Override
	public String getI18nKey() {
		return licenseAnnotation.i18nKey();
	}

}
