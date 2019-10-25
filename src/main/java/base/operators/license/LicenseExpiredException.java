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
 * Is thrown if the current active license expires. Contains a copy of the expired license and a
 * copy of the new license which can be <code>null</code> if no new license is installed.
 *
 * @author Nils Woehler
 *
 */
public class LicenseExpiredException extends Exception {

	private static final long serialVersionUID = 1L;

	private final License oldLicense;
	private final License newLicense;

	/**
	 * Creates a license expired exception
	 *
	 * @param oldLicense
	 *            the old license
	 * @param newLicense
	 *            the new license
	 */
	public LicenseExpiredException(License oldLicense, License newLicense) {
		this.oldLicense = oldLicense;
		this.newLicense = newLicense;
	}

	/**
	 * @return the oldLicense
	 */
	public License getOldLicense() {
		return this.oldLicense;
	}

	/**
	 * @return the newLicense
	 */
	public License getNewLicense() {
		return this.newLicense;
	}

}
