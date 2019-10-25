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

/**
 * An exception which is thrown if the loading of licenses does not work.
 *
 * @author Nils Woehler
 *
 */
public class LicenseLoadingException extends Exception {

	private static final long serialVersionUID = -6398408026434038563L;

	/**
	 * Creates a new {@link LicenseLoadingException}.
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public LicenseLoadingException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a new {@link LicenseLoadingException}.
	 *
	 * @param message
	 *            the message
	 */
	public LicenseLoadingException(String message) {
		super(message);
	}

}
