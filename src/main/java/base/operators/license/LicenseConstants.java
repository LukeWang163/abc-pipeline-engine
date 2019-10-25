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

import base.operators.license.product.BooleanConstraint;
import base.operators.license.product.Constraint;
import base.operators.license.product.NumericalConstraint;


/**
 * Container class that holds {@link Constraint} object and some other constants for licensing
 * mechanism of RapidMiner.
 *
 * @author Nils Woehler, Marcel Michel
 */
public final class LicenseConstants {

	/**
	 * Default product ID for RM Studio
	 *
	 * @deprecated Use {@link StudioLicenseConstants#PRODUCT_ID} instead
	 */
	@Deprecated
	public static final String DEFAULT_PRODUCT_ID = StudioLicenseConstants.PRODUCT_ID;

	/**
	 * Server product ID
	 *
	 * @deprecated Use {@link ServerLicenseConstants#PRODUCT_ID} instead
	 */
	@Deprecated
	public static final String SERVER_PRODUCT_ID = ServerLicenseConstants.PRODUCT_ID;

	// --------------------------------------------------------------------------
	// STARTER licenses
	// --------------------------------------------------------------------------

	/**
	 * The precedence of a starter license.
	 */
	public static final int STARTER_LICENSE_PRECEDENCE = 10;

	/**
	 * The name of a starter license product edition
	 */
	public static final String STARTER_EDITION = "starter";

	/**
	 * The data row limit of a starter license.
	 */
	public static final Integer STARTER_DATA_ROWS = 0;

	/**
	 * The logical processor limit of a starter license.
	 */
	public static final Integer STARTER_LOGICAL_PROCESSORS = 0;

	/**
	 * The memory limit in MB of a starter license for server.
	 */
	public static final Integer STARTER_MEMORY_LIMIT = 0;

	/**
	 * The limit of daily web service calls of a starter license for server.
	 */
	public static final Integer STARTER_WEB_SERVICE_LIMIT = 0;

	/**
	 * Defines the number of nodes for Radoop and Streams without a license.
	 */
	public static final Integer STARTER_NODES = 0;

	// --------------------------------------------------------------------------
	// Studio & Server constraints
	// --------------------------------------------------------------------------

	/** Constraint ID of the data row constraint */
	public static final String DATA_ROW_CONSTRAINT_ID = "data-rows";

	/** The data row constraint instance. */
	public final static NumericalConstraint DATA_ROW_CONSTRAINT = new NumericalConstraint(DATA_ROW_CONSTRAINT_ID, 0);

	/** Constraint ID of the logical processor constraint */
	public static final String LOGICAL_PROCESSORS_CONSTRAINT_ID = "logical-processors";

	/** The logical processor constraint instance. */
	public final static NumericalConstraint LOGICAL_PROCESSOR_CONSTRAINT = new NumericalConstraint(
			LOGICAL_PROCESSORS_CONSTRAINT_ID, 0);

	/** Constraint ID of the memory limit constraint */
	public static final String MEMORY_LIMIT_CONSTRAINT_ID = "memory-limit";

	/** The memory limit constraint instance */
	public final static NumericalConstraint MEMORY_LIMIT_CONSTRAINT = new NumericalConstraint(MEMORY_LIMIT_CONSTRAINT_ID, 0);

	/** Constraint ID of the memory limit constraint */
	public static final String WEB_SERVICE_LIMIT_CONSTRAINT_ID = "web-service-limit";

	/** The web service limit constraint instance */
	public final static NumericalConstraint WEB_SERVICE_LIMIT_CONSTRAINT = new NumericalConstraint(
			WEB_SERVICE_LIMIT_CONSTRAINT_ID, 0);

	// --------------------------------------------------------------------------
	// Radoop & Streams constraints
	// --------------------------------------------------------------------------

	/** Constraint ID of the node constraint */
	public static final String NODES_CONSTRAINT_ID = "nodes";

	/** The node constraint instance. */
	public final static NumericalConstraint NODES_CONSTRAINT = new NumericalConstraint(NODES_CONSTRAINT_ID, 0);

	/** Constraint ID for in-Hadoop usage of RM operators constraint */
	public static final String RM_IN_HADOOP_CONSTRAINT_ID = "rm-in-hadoop";

	/** In-Hadoop usage of RM operators constraint instance. */
	public final static BooleanConstraint RM_IN_HADOOP_CONSTRAINT = new BooleanConstraint(RM_IN_HADOOP_CONSTRAINT_ID, false);

	/**
	 * @return the default constraint for RapidMiner Studio and Server products
	 */
	public static final Constraint<?, ?>[] getDefaultConstraints() {
		return new Constraint[] { DATA_ROW_CONSTRAINT, LOGICAL_PROCESSOR_CONSTRAINT, MEMORY_LIMIT_CONSTRAINT,
				WEB_SERVICE_LIMIT_CONSTRAINT };
	}

	/**
	 * Utility class constructor.
	 */
	private LicenseConstants() throws IllegalAccessException {
		throw new IllegalAccessException("Utility class");
	}

}
