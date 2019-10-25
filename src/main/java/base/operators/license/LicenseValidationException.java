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
 * The exception is thrown if the validation of a license fails. It contains the cause as well as an
 * message which explains what has gone wrong.
 *
 * @author Nils Woehler
 *
 */
public class LicenseValidationException extends Exception {

	private static final long serialVersionUID = 6516933615068529332L;

	/**
	 * Creates a new {@link LicenseValidationException}.
	 * 
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public LicenseValidationException(String message, Exception cause) {
		super(message, cause);
	}
}
