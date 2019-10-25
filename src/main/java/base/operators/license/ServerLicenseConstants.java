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
 * mechanism of RapidMiner Server.
 *
 * @author Nils Woehler, Marcel Michel
 *
 */
public final class ServerLicenseConstants {

	/**
	 * Product ID for RM Server
	 */
	public static final String PRODUCT_ID = "operators-server";

	/**
	 * The product version of Server
	 */
	public static final String VERSION = "7.2+";

	// --------------------------------------------------------------------------
	// FREE EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a free license.
	 */
	public static final int FREE_LICENSE_PRECEDENCE = 20;

	/**
	 * The name of a free license product edition
	 */
	public static final String FREE_EDITION = "free";

	/**
	 * The logical processor limit of a free license.
	 */
	public static final Integer FREE_LOGICAL_PROCESSORS = 1;

	/**
	 * The memory limit in MB of a free license.
	 */
	public static final Integer FREE_MEMORY_LIMIT = 2048;

	/**
	 * The limit of daily web service calls of a free license.
	 */
	public static final Integer FREE_WEB_SERVICE_LIMIT = 1000;

	// --------------------------------------------------------------------------
	// NON COMMERCIAL EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a non commercial license.
	 */
	public static final int NON_COMMERCIAL_LICENSE_PRECEDENCE = 30;

	/**
	 * The name of a non commercial license product edition
	 */
	public static final String NON_COMMERCIAL_EDITION = "non-commercial";

	/**
	 * The logical processor limit of a non commercial license.
	 */
	public static final Integer NON_COMMERCIAL_LOGICAL_PROCESSORS = 1;

	/**
	 * The limit of daily web service calls of a non commercial license.
	 */
	public static final Integer NON_COMMERCIAL_WEB_SERVICE_LIMIT = 1000;

	// --------------------------------------------------------------------------
	// Pay-as-you-go UNLIMITED EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a PAYG unlimited license.
	 */
	public static final int PAYG_UNLIMITED_LICENSE_PRECEDENCE = 35;

	/**
	 * The name of a PAYG unlimited license product edition
	 */
	public static final String PAYG_UNLIMITED_EDITION = "payg_unlimited";

	// --------------------------------------------------------------------------
	// SMALL EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a small license.
	 */
	public static final int SMALL_LICENSE_PRECEDENCE = 40;

	/**
	 * The name of a small license product edition
	 */
	public static final String SMALL_EDITION = "small";

	/**
	 * The logical processor limit of a small license.
	 */
	public static final Integer SMALL_LOGICAL_PROCESSORS = 4;

	/**
	 * The memory limit in MB of a small license.
	 */
	public static final Integer SMALL_MEMORY_LIMIT = 16_384;

	// --------------------------------------------------------------------------
	// MEDIUM EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a medium license.
	 */
	public static final int MEDIUM_LICENSE_PRECEDENCE = 50;

	/**
	 * The name of a medium license product edition
	 */
	public static final String MEDIUM_EDITION = "medium";

	/**
	 * The logical processor limit of a medium license.
	 */
	public static final Integer MEDIUM_LOGICAL_PROCESSORS = 8;

	/**
	 * The memory limit in MB of a medium license.
	 */
	public static final Integer MEDIUM_MEMORY_LIMIT = 65_536;

	// --------------------------------------------------------------------------
	// TRIAL EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a trial license.
	 */
	public static final int TRIAL_LICENSE_PRECEDENCE = 60;

	/**
	 * The name of a trial license product edition
	 */
	public static final String TRIAL_EDITION = "trial";

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
	 * Edition name of professional edition license
	 */
	public static final String DEVELOPER_EDITION = "developer";

	/**
	 * Utility class constructor.
	 */
	private ServerLicenseConstants() throws IllegalAccessException {
		throw new IllegalAccessException("Utility class");
	}

}
