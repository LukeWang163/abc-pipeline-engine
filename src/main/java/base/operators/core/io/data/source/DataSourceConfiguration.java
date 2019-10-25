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

import java.util.Map;


/**
 * A configuration for a {@link DataSource} which allows to store data source parameter and the data
 * source version to disk. The configuration is used by
 * {@link DataSource#configure(DataSourceConfiguration)} to recreated a data source setup which was
 * configured by the user beforehand.
 *
 * @author Nils Woehler
 * @since 0.2.0
 */
public interface DataSourceConfiguration {

	/**
	 * @return the current version. Can be used by
	 *         {@link BaseDataSource#configure(DataSourceConfiguration)} to check which import logic
	 *         to use
	 */
	String getVersion();

	/**
	 * @return the actual configuration of the data source
	 */
	Map<String, String> getParameters();

}
