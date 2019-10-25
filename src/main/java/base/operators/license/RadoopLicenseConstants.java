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
 * Container class that holds constants for licensing mechanism of RapidMiner Radoop.
 *
 * @author Mate Stogica
 *
 */
public final class RadoopLicenseConstants {

	/**
	 * Product ID for RM Radoop
	 */
	public static final String PRODUCT_ID = "radoop";

	/**
	 * The product version of Radoop
	 */
	public static final String VERSION = "7.2+";

	/**
	 * Step size between precedences.
	 */
	public static final int STEP = 1000000;

	// --------------------------------------------------------------------------
	// General node limit constant.
	// --------------------------------------------------------------------------

	/**
	 * "In-Hadoop usage of RM operators" limit.
	 */
	public static final Integer ALLOWED_NODES = -1;

	/**
	 * The allowed number of nodes for *small* enterprise deals.
	 */
	public static final Integer SMALL_ENTERPRISE_NODES = 30;

	/**
	 * The allowed number of nodes for *medium* enterprise deals.
	 */
	public static final Integer MEDIUM_ENTERPRISE_NODES = 100;

	/**
	 * The allowed number of nodes for *large* enterprise deals.
	 */
	public static final Integer LARGE_ENTERPRISE_NODES = -1;

	// --------------------------------------------------------------------------
	// FREE EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a free license.
	 */
	public static final int FREE_LICENSE_PRECEDENCE = 2 * STEP;

	/**
	 * The name of a free license product edition
	 */
	public static final String FREE_EDITION = "free";

	/**
	 * The in-Hadoop usage of RM operators limit of a free license.
	 */
	public static final Boolean FREE_RM_IN_HADOOP = false;

	// --------------------------------------------------------------------------
	// NON COMMERCIAL EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a non commercial license.
	 */
	public static final int NON_COMMERCIAL_LICENSE_PRECEDENCE = 3 * STEP;

	/**
	 * The name of a non commercial license product edition
	 */
	public static final String NON_COMMERCIAL_EDITION = "non-commercial";

	// --------------------------------------------------------------------------
	// TRIAL EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a trial license.
	 */
	public static final int TRIAL_LICENSE_PRECEDENCE = 6 * STEP;

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
	public static final int UNLIMITED_LICENSE_PRECEDENCE = 7 * STEP;

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
	public static final int DEVELOPER_LICENSE_PRECEDENCE = 10 * STEP;

	/**
	 * Edition name of professional edition license
	 */
	public static final String DEVELOPER_EDITION = "developer";

	/**
	 * Utility class constructor.
	 */
	private RadoopLicenseConstants() throws IllegalAccessException {
		throw new IllegalAccessException("Utility class");
	}

}
