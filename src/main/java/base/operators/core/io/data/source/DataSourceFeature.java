/**
 * Copyright (c) 2014-2018, RapidMiner GmbH, All rights reserved.
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
package base.operators.core.io.data.source;

/**
 * DataSource Features
 *
 * @author Jonas Wilms-Pfau
 * @since 0.2.2
 * @see DataSource#supportsFeature(DataSourceFeature)
 */
public enum DataSourceFeature {

	/**
	 * {@link DataSource#getMetadata() Datasource.getMetadata()} contains {@link base.operators.core.io.data.ColumnMetaData.ColumnType#DATETIME
	 * DATETIME, DATE and TIME} information
	 *
	 * @since 0.2.2
	 */
	DATETIME_METADATA;
}
