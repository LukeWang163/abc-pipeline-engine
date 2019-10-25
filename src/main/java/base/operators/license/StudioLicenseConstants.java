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
 * mechanism of RapidMiner Studio.
 *
 * @author Nils Woehler, Marcel Michel
 *
 */
public final class StudioLicenseConstants {

	/**
	 * Product ID for RM Studio
	 */
	public static final String PRODUCT_ID = "operators-studio";

	/**
	 * The product version of Studio
	 */
	public static final String VERSION = "7.2+";

	// --------------------------------------------------------------------------
	// FREE EDITION (10.000 rows)
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
	 * The data row limit of a free license.
	 */
	public static final Integer FREE_DATA_ROWS = 10_000;

	/**
	 * The logical processor limit of a free license.
	 */
	public static final Integer FREE_LOGICAL_PROCESSORS = 1;

	// --------------------------------------------------------------------------
	// FREE EDITION (20.000 rows)
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a free license.
	 */
	public static final int FREE_20K_LICENSE_PRECEDENCE = 21;

	/**
	 * The name of a free license product edition
	 */
	public static final String FREE_20K_EDITION = "free_20k";

	/**
	 * The data row limit of a free license.
	 */
	public static final Integer FREE_20K_DATA_ROWS = 20_000;

	// --------------------------------------------------------------------------
	// FREE EDITION (30.000 rows)
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a free license.
	 */
	public static final int FREE_30K_LICENSE_PRECEDENCE = 22;

	/**
	 * The name of a free license product edition
	 */
	public static final String FREE_30K_EDITION = "free_30k";

	/**
	 * The data row limit of a free license.
	 */
	public static final Integer FREE_30K_DATA_ROWS = 30_000;

	// --------------------------------------------------------------------------
	// FREE EDITION (40.000 rows)
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a free license.
	 */
	public static final int FREE_40K_LICENSE_PRECEDENCE = 23;

	/**
	 * The name of a free license product edition
	 */
	public static final String FREE_40K_EDITION = "free_40k";

	/**
	 * The data row limit of a free license.
	 */
	public static final Integer FREE_40K_DATA_ROWS = 40_000;

	// --------------------------------------------------------------------------
	// FREE EDITION (50.000 rows)
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a free license.
	 */
	public static final int FREE_50K_LICENSE_PRECEDENCE = 24;

	/**
	 * The name of a free license product edition
	 */
	public static final String FREE_50K_EDITION = "free_50k";

	/**
	 * The data row limit of a free license.
	 */
	public static final Integer FREE_50K_DATA_ROWS = 50_000;

	// --------------------------------------------------------------------------
	// NON COMMERCIAL EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a non commercial license.
	 */
	public static final int NON_COMMERCIAL_LICENSE_PRECEDENCE = 30;

	/**
	 * Edition name of small edition license
	 */
	public static final String NON_COMMERCIAL_EDITION = "non-commercial";

	/**
	 * The logical processor limit of a non commercial license.
	 */
	public static final Integer NON_COMMERCIAL_LOGICAL_PROCESSORS = 1;

	// --------------------------------------------------------------------------
	// SMALL EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a small license.
	 */
	public static final int SMALL_LICENSE_PRECEDENCE = 40;

	/**
	 * Edition name of small edition license
	 */
	public static final String SMALL_EDITION = "small";

	/**
	 * The data row limit of a small license.
	 */
	public static final Integer SMALL_DATA_ROWS = 100_000;

	/**
	 * The logical processor limit of a free license.
	 */
	public static final Integer SMALL_LOGICAL_PROCESSORS = 2;

	// --------------------------------------------------------------------------
	// MEDIUM EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a medium license.
	 */
	public static final int MEDIUM_LICENSE_PRECEDENCE = 50;

	/**
	 * Edition name of small edition license
	 */
	public static final String MEDIUM_EDITION = "medium";

	/**
	 * The data row limit of a medium license.
	 */
	public static final Integer MEDIUM_DATA_ROWS = 1_000_000;

	/**
	 * The logical processor limit of a medium license.
	 */
	public static final Integer MEDIUM_LOGICAL_PROCESSORS = 4;

	// --------------------------------------------------------------------------
	// TRIAL EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a trial license.
	 */
	public static final int TRIAL_LICENSE_PRECEDENCE = 60;

	/**
	 * Edition name of trial edition license
	 */
	public static final String TRIAL_EDITION = "trial";

	// --------------------------------------------------------------------------
	// UNLIMITED EDITION
	// --------------------------------------------------------------------------

	/**
	 * The precedence of an unlimited license.
	 */
	public static final int UNLIMITED_LICENSE_PRECEDENCE = 70;

	/**
	 * Edition name of unlimited edition license
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
	private StudioLicenseConstants() throws IllegalAccessException {
		throw new IllegalAccessException("Utility class");
	}

}
