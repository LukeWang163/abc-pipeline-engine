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

import java.time.LocalDate;
import java.util.Set;

import base.operators.license.product.Product;


/**
 * The interface represents a license for a {@link Product} that was loaded by the
 * {@link LicenseManager}. It can be queried for license details like product ID, product edition
 * and license constraints. Furthermore it provides methods to check the status of the license.
 *
 * @author Nils Woehler
 *
 */
public interface License extends Comparable<License> {

	/**
	 * @return license annotations (if any)
	 * @since 3.1.0
	 */
	String getAnnotations();

	/**
	 * @return the precedence of the license
	 */
	int getPrecedence();

	/**
	 * @return the product id this license is for
	 */
	String getProductId();

	/**
	 * @return the product edition
	 */
	String getProductEdition();

	/**
	 * @return a set of product versions the license is compatible with
	 *
	 * @since 3.1.0
	 */
	Set<String> getVersions();

	/**
	 * @return a {@link LicenseUser} object which contains details about the user of this license
	 */
	LicenseUser getLicenseUser();

	/**
	 * @return constraints the {@link Constraints} container which stores a map from
	 *         constraint ID to constraint value
	 */
	Constraints getConstraints();

	/**
	 * @return the start data of the license. Can be <code>null</code> if license does not expire.
	 */
	LocalDate getStartDate();

	/**
	 * @return the expiration date of the license. Can be <code>null</code> if license does not
	 *         expire.
	 */
	LocalDate getExpirationDate();

	/**
	 * @return the current status of the license without validating it beforehand
	 */
	LicenseStatus getStatus();

	/**
	 * Checks if start date is equal or before and expiration date is equal or after the provided date.
	 * If the status has changed the changed status will stored within the {@link License} and returned by
	 * {@link #getStatus()} afterwards.
	 *
	 * @param today
	 *            the date to check against
	 *
	 * @return the current {@link LicenseStatus}
	 */
	LicenseStatus validate(LocalDate today);

	/**
	 * @return <code>true</code> if the license is a starter license
	 */
	boolean isStarterLicense();

	/**
	 * @return the unique license ID
	 */
	String getLicenseID();

	/**
	 * @return a deep copy of the license
	 */
	License copy();

}
