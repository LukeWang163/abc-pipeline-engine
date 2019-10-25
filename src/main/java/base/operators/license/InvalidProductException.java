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
 * Thrown if a product is registered at the {@link LicenseManager} which has a signature which does
 * not match the product characteristics. This means the product was changed after it has been
 * approved by RapidMiner.
 *
 * @author Marco Boeck
 *
 */
public class InvalidProductException extends Exception {

	private static final long serialVersionUID = 1L;

	private final String productId;

	/**
	 * Constructor for a {@link InvalidProductException} without a cause.
	 *
	 * @param message
	 *            the message
	 * @param productId
	 *            the product ID
	 */
	public InvalidProductException(String message, String productId) {
		super(message);
		this.productId = productId;
	}

	/**
	 * Constructor for a {@link InvalidProductException} with a cause.
	 *
	 * @param message
	 *            the message
	 * @param productId
	 *            the product ID
	 * @param cause
	 *            the cause
	 */
	public InvalidProductException(String message, String productId, Throwable cause) {
		super(message, cause);
		this.productId = productId;
	}

	/**
	 * @return the productId
	 */
	public String getProductId() {
		return this.productId;
	}

}
