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
package base.operators.license.product;

import java.util.List;
import java.util.UUID;

import base.operators.license.LicenseManager;


/**
 * Instances of this class represent a {@link Product} like e.g. RapidMiner or RapidMiner
 * Extensions. Extensions should register an instance of this class to the {@link LicenseManager} if
 * they want to use the license mechanism provided by RapidMiner. After registering the same
 * instance can be used to retrieve the installed licenses for the product.
 *
 * @author Nils Woehler
 *
 */
public interface Product {

	/**
	 * Regular expression for RapidMiner core product IDs (e.g. operators-studio,
	 * operators-server, etc.)
	 */
	public static final String RM_REGEX = "operators-.+";

	/**
	 * Searches the list of constraints for a constraint with the specified constraintId.
	 *
	 * @param constraintId
	 *            the ID that should be used to look up the constraint
	 *
	 * @return <code>null</code> if no constraint is found.
	 *
	 */
	Constraint<?, ?> findConstraint(String constraintId);

	/**
	 * @return a list of all constraints
	 */
	List<Constraint<?, ?>> getConstraints();

	/**
	 * @return the products UUID
	 */
	UUID getProdictUUID();

	/**
	 * @return the ID of the product
	 */
	String getProductId();

	/**
	 * @return the current version of the product
	 */
	String getProductVersion();

	/**
	 * @return a list of further supported license versions
	 * @since 3.1.0
	 */
	List<String> getSupportedVersions();

	/**
	 *
	 * @return the base64 encoded signature to check the integrity of the configuration of this
	 *         product
	 */
	String getSignature();

	/**
	 * Creates a Base64 encoded JSON representation of this product.
	 *
	 * @return base64 encoded JSON or <code>null</code>
	 */
	String createBase64Representation();

	/**
	 *
	 * @return if this product is a RapidMiner Studio extension
	 */
	boolean isExtension();

}
