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

import base.operators.license.product.Constraint;


/**
 * Container class that holds {@link Constraint} object and some other constants for licensing
 * mechanism of RapidMiner Scoring Agent.
 *
 * @author Marcel Michel
 *
 */
public final class ScoringAgentLicenseConstants {

	/**
	 * Product ID for RM Scoring Agent
	 */
	public static final String PRODUCT_ID = "operators-scoring-agent";

	/**
	 * The product version of Scoring Agent
	 */
	public static final String VERSION = "8.2+";

	// --------------------------------------------------------------------------
	// UNLIMITED EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a unlimited license.
	 */
	public static final int UNLIMITED_LICENSE_PRECEDENCE = 70;

	/**
	 * The name of a unlimited license product edition
	 */
	public static final String UNLIMITED_EDITION = "unlimited";

	// --------------------------------------------------------------------------
	// DEVELOPER EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a professional license.
	 */
	public static final int DEVELOPER_LICENSE_PRECEDENCE = 100;

	/**
	 * Edition name of developer edition license
	 */
	public static final String DEVELOPER_EDITION = "developer";

	private ScoringAgentLicenseConstants() throws IllegalAccessException {
		throw new IllegalAccessException("Utility class");
	}
}
