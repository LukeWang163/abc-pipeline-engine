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

import java.text.DateFormat;
import java.util.Date;


/**
 * A {@link DataSetRow} represents one row of data items fetched by the {@link DataSet} when calling
 * {@link DataSet#nextRow()}. The data items within the row can be accessed via the column index.
 * Use {@link DataSetMetaData#getTypes()} to decide which method to use to fetch the data.
 *
 * @author Nils Woehler
 * @since 0.2.0
 */
public interface DataSetRow {

	/**
	 * Returns the data item at the specified column index as a {@link Date}. Uses the
	 * {@link DateFormat} provided by the {@link DataSetMetaData#getDateFormat()} to parse the date.
	 *
	 * @param columnIndex
	 *            the 0-based column index for the data item
	 * @return {@code null} for missing values, the parsed {@link Date} otherwise
	 * @throws ParseException
	 *             in case the data item could be parsed to date with the current configured
	 *             {@link DateFormat}
	 * @throws IndexOutOfBoundsException
	 *             in case the specified column does not exist
	 */
	Date getDate(int columnIndex) throws ParseException, IndexOutOfBoundsException;

	/**
	 * Returns the data item at the specified column index as a {@link String}.
	 *
	 * @param columnIndex
	 *            the 0-based column index for the data point in the current row
	 * @return {@code null} for missing values, the parsed {@link String} otherwise
	 * @throws ParseException
	 *             in case the data item could not be parsed
	 * @throws IndexOutOfBoundsException
	 *             in case the specified column does not exist
	 */
	String getString(int columnIndex) throws ParseException, IndexOutOfBoundsException;

	/**
	 * Returns the data item at the specified column index as a {@link Double}.
	 *
	 * @param columnIndex
	 *            the 0-based column index for the data point in the current row
	 * @return {@link Double#NaN} for missing values, the parsed {@link Double} otherwise
	 * @throws ParseException
	 *             in case the data item at the provided column index could be parsed to date with
	 *             the current configured {@link DateFormat}
	 * @throws IndexOutOfBoundsException
	 *             in case the specified column does not exist
	 */
	double getDouble(int columnIndex) throws ParseException, IndexOutOfBoundsException;

	/**
	 * Checks whether the data item at the provided column index is missing.
	 *
	 * @param columnIndex
	 *            the column index for the data point in the current row
	 * @return {@code true} in case the data point is missing, {@code false} otherwise.
	 * @throws IndexOutOfBoundsException
	 *             in case the specified column does not exist
	 */
	boolean isMissing(int columnIndex) throws IndexOutOfBoundsException;

}
