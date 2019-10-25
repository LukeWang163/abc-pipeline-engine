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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * A {@link MemoryLicenseLocation} can be used be other products that include RapidMiner to ship
 * hardcoded licenses. Furthermore it can be used for unit tests.
 *
 * @author Nils Woehler
 *
 */
public class MemoryLicenseLocation implements LicenseLocation {

	private Map<String, List<String>> productToLicenseStrings;

	/** Creates a {@link MemoryLicenseLocation} with an empty storage. */
	public MemoryLicenseLocation() {
		this(null);
	}

	/**
	 * Creates a {@link MemoryLicenseLocation} with a prefilled storage.
	 *
	 * @param productIdToLicenseString
	 *            the map containing product IDs as keys and license text lists as values
	 */
	public MemoryLicenseLocation(Map<String, List<String>> productIdToLicenseString) {
		if (productIdToLicenseString != null) {
			this.productToLicenseStrings = productIdToLicenseString;
		} else {
			this.productToLicenseStrings = new HashMap<>();
		}
	}

	@Override
	public List<String> loadLicenses(String productId) throws LicenseLoadingException {
		List<String> licenseList = productToLicenseStrings.get(productId);
		if (licenseList == null) {
			return new LinkedList<>();
		}
		return licenseList;
	}

	@Override
	public void storeLicense(String productId, String productVersion, String edition, LocalDate start, LocalDate end,
							 String licenseString) throws LicenseStoringException {
		List<String> list = productToLicenseStrings.computeIfAbsent(productId, (key) -> new LinkedList<>());
		list.add(licenseString);
	}

}
