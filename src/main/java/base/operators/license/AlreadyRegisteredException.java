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
 * The exception that is thrown if another product with the same productId is already registered to
 * the {@link LicenseManager}.
 *
 * @author Nils Woehler
 *
 */
public class AlreadyRegisteredException extends Exception {

	private static final long serialVersionUID = 1L;

	private final String productId;

	/**
	 * @param productId
	 *            the product ID
	 */
	public AlreadyRegisteredException(final String productId) {
		this.productId = productId;
	}

	/**
	 * @return the productId
	 */
	public String getProductId() {
		return this.productId;
	}

}
