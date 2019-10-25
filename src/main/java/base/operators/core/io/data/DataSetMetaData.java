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
package base.operators.core.io.data;

import base.operators.core.io.data.source.DataSource;

import java.text.DateFormat;
import java.util.List;


/**
 * The {@link DataSetMetaData} describes the content of {@link DataSet}s returned by
 * {@link DataSource}s. It contains information about column meta data as well as information about
 * the date format to parse date columns.
 *
 * @author Nils Woehler
 * @since 0.2.0
 *
 */
public interface DataSetMetaData {

	/**
	 * Returns the stored list of {@link ColumnMetaData}. The list can be edited to change the state
	 * of the meta data object (e.g. by adding a new {@link ColumnMetaData} or removing an existing
	 * one).
	 *
	 * @return a list of {@link ColumnMetaData} containing the meta data for each column
	 */
	List<ColumnMetaData> getColumnMetaData();

	/**
	 * @param columnIndex
	 *            the index of the column of interest
	 * @return the {@link ColumnMetaData} for the column with index columnIndex
	 */
	ColumnMetaData getColumnMetaData(int columnIndex);

	/**
	 * @return the configured date format
	 */
	DateFormat getDateFormat();

	/**
	 * Updates the configured date format.
	 *
	 * @param dateFormat
	 *            the new date format
	 */
	void setDateFormat(DateFormat dateFormat);

	/**
	 * Returns whether the data set import should be done fault tolerant.
	 *
	 * @return {@code true} if {@link ParsingError} values should result in missing values,
	 *         {@code false} if the {@link DataSet} import should be aborted on {@link ParsingError}
	 */
	boolean isFaultTolerant();

	/**
	 * Sets whether the data set import should be done fault tolerant.
	 *
	 * @param faultTolerant
	 *            {@code true} if {@link ParsingError} values should result in missing values,
	 *            {@code false} if the {@link DataSet} import should be aborted on
	 *            {@link ParsingError}
	 */
	void setFaultTolerant(boolean faultTolerant);

	/**
	 * Creates a deep copy of the meta data instance.
	 *
	 * @return a deep copy of the instance with exactly the same settings as this instance
	 */
	DataSetMetaData copy();

	/**
	 * Adapt the settings of this meta data according to the provided {@link DataSetMetaData}
	 * instance.
	 *
	 * @param other
	 *            the instance that should be used to configure the current instance
	 */
	void configure(DataSetMetaData other);

}
