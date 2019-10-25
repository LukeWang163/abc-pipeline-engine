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

/**
 * The status of a validated license.
 *
 * @author Nils Woehler
 *
 */
public enum LicenseStatus {
	// Disable checkstyle comment checks
	// CHECKSTYLE:OFF

	VALID, SIGNATURE_INVALID, PRODUCT_VERSION_INVALID, WRONG_PRODUCT_ID,

	// license status regarding license key
	KEY_INVALID, KEY_BLACKLISTED, KEY_PHONY,

	// license status regarding license date
	EXPIRED, STARTS_IN_FUTURE,
}
