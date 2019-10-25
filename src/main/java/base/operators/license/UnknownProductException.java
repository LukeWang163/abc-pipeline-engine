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
 * Thrown if a license loaded by the {@link LicenseManager} has an unknown productId. This means the
 * product this license belongs to was not yet registered to the {@link LicenseManager}.
 *
 * @author Nils Woehler
 *
 */
public class UnknownProductException extends Exception {

	private static final long serialVersionUID = 1L;

	private final String productId;

	/**
	 * Creates a new {@link UnknownProductException}.
	 *
	 * @param productId
	 *            the product ID for which no product could be found
	 */
	public UnknownProductException(String productId) {
		this.productId = productId;
	}

	/**
	 * @return the productId
	 */
	public String getProductId() {
		return this.productId;
	}

}
