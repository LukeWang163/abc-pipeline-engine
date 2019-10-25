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
package base.operators.license.location;

import java.time.LocalDate;
import java.util.List;


/**
 * Specifies a location to store licenses to and retrieve licenses from.
 *
 * @author Nils Woehler
 *
 */
public interface LicenseLocation {

	/**
	 * Loads licenses for the provided product Id.
	 *
	 * @param productId
	 *            loads licenses by the provided product id
	 * @return the list of licenses found. If no licenses are found, an empty list is returned.
	 * @throws LicenseLoadingException
	 *             thrown if loading the licenses fails
	 */
	List<String> loadLicenses(String productId) throws LicenseLoadingException;

	/**
	 * Stores a license text for the provided parameters. Only the product Id will be used as a
	 * reference to the stored license text.
	 *
	 * @param productId
	 *            the product id of the current license
	 * @param productVersion
	 *            the product version, might be {@code null}
	 * @param edition
	 *            the product edition name
	 * @param start
	 *            the start date
	 * @param end
	 *            the end date
	 * @param licenseString
	 *            the actual license string
	 * @throws LicenseStoringException
	 *             thrown if storing the license fails
	 */
	void storeLicense(String productId, String productVersion, String edition, LocalDate start, LocalDate end, String licenseString)
			throws LicenseStoringException;
}
