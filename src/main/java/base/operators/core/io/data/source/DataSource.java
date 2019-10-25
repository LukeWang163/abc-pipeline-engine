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

import base.operators.core.io.data.DataSet;
import base.operators.core.io.data.DataSetException;
import base.operators.core.io.data.DataSetMetaData;
import base.operators.core.io.gui.ImportWizard;


/**
 * A {@link DataSource} is a representation for any kind of {@link DataSet} data provider for the
 * {@link ImportWizard}.
 * <p>
 * A data source instance is created by a {@link DataSourceFactory}. Use the
 * {@link DataSourceFactoryRegistry} to register the {@link DataSourceFactory}.
 * <p>
 * In case the data is loaded from a local file extend {@link FileDataSource} and
 * {@link FileDataSourceFactory} instead.
 *
 * @author Nils Woehler
 * @since 0.2.0
 */
public interface DataSource extends AutoCloseable {

	/**
	 * Returns the configuration of the data source as a {@link DataSourceConfiguration}. The
	 * representation can be used to reconfigure a new {@link DataSource} instance by calling
	 * {@link #configure(DataSourceConfiguration)} with a configuration that was extracted
	 * beforehand.</br>
	 *
	 * @return the configuration of the data source as {@link DataSourceConfiguration}
	 */
	DataSourceConfiguration getConfiguration();

	/**
	 * Accepts a {@link DataSourceConfiguration} of the data source as returned by
	 * {@link #getConfiguration()} and configures the data source accordingly.</br>
	 * <b>Caution:</b> Each data source implementation has to ensure <b>backwards compatibility</b>
	 * with a configuration that was created with an older version of the data source instance.
	 *
	 * @param configuration
	 *            a {@link DataSourceConfiguration} of the data source returned by
	 *            {@link #getConfiguration()}
	 * @throws DataSetException
	 *             in case the configuration could not be read
	 */
	void configure(DataSourceConfiguration configuration) throws DataSetException;

	/**
	 * Uses the current data source configuration to create a new {@link DataSet} instance which
	 * contains the whole data referenced by the current configuration. If the configuration does
	 * not change the same {@link DataSet} instance will be returned on subsequent calls. Otherwise
	 * a new instance will be created.In any case it is a good idea to call {@link DataSet#reset()}
	 * after retrieving the data set to make sure the data set starts with the first data row.
	 * <p>
	 * This method is used by the {@link ImportWizard#STORE_DATA_STEP_ID} step to store the a static
	 * data set to the data store. It is also used by dynamic data sources to retrieve the data
	 * which should be processed.
	 *
	 * @return a new {@link DataSet} instance for this {@link DataSource} if called for the first
	 *         time, a cached {@link DataSet} instance in case it is called another time and the
	 *         configuration has not changed
	 * @throws DataSetException
	 *             in case the data set could not be created (e.g. because of an invalid
	 *             configuration)
	 */
	DataSet getData() throws DataSetException;

	/**
	 * Uses the current data source configuration to create a new {@link DataSet} instance which
	 * contains a preview of the data that would be returned by {@link #getData()}. If the
	 * configuration of the data source does not change the same {@link DataSet} instance has to be
	 * returned in subsequent calls. Otherwise a new instance has to be created. In any case
	 * {@link DataSet#reset()} should be called after retrieving the preview to make sure the data
	 * set starts with the first data row.
	 * <p>
	 * This method should only be used for data preview in the UI. Do not call it for actual data
	 * processing! It is for example used by the {@link ImportWizard#CONFIGURE_DATA_STEP_ID} for
	 * previewing the data.
	 *
	 * @param maxPreviewSize
	 *            the maximum number of rows for the preview data set
	 * @return a new preview {@link DataSet} instance for this {@link DataSource} if called for the
	 *         first time, a cached {@link DataSet} instance in case it is called another time and
	 *         the configuration has not changed
	 * @throws DataSetException
	 *             in case the preview data set could not be created (e.g. because of an invalid
	 *             configuration)
	 */
	DataSet getPreview(int maxPreviewSize) throws DataSetException;

	/**
	 * Returns the meta data for the {@link DataSet}s produced by this data source. The meta data
	 * contains information about the column names, column types and column roles.
	 *
	 * @return the {@link DataSetMetaData} for {@link DataSet}s produced by this data source. Must
	 *         not return {@code null}.
	 * @throws DataSetException
	 *             in case the meta data could not be created
	 */
	DataSetMetaData getMetadata() throws DataSetException;

	/**
	 * Notifies the {@link DataSource} to free all allocated resources (e.g. to close opened
	 * streams, remove temporary files, etc.) as the {@link ImportWizard} is about be closed.
	 *
	 * @throws DataSetException
	 *             in case there was an error while freeing the allocated resources
	 */
	@Override
	void close() throws DataSetException;

	/**
	 * Checks if this {@link DataSource} supports the given {@link DataSourceFeature}
	 *
	 * @param feature the {@link DataSourceFeature} that should be supported
	 * @return {@code true} if the given feature is supported
	 * @since 0.2.2
	 */
	default boolean supportsFeature(DataSourceFeature feature){
		return false;
	}
}
