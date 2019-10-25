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

import java.util.NoSuchElementException;


/**
 * A representation of data loaded by a {@link DataSource}. The {@link DataSet} allows to iterate
 * over the data set's {@link DataSetRow}s sequentially by calling {@link #hasNext()} and
 * {@link #nextRow()}. Each time {@link #nextRow()} is called the data row pointer will be moved to
 * the next row. Use {@link #reset()} to reset the pointer to iterate the rows from the beginning.
 *
 * @author Nils Woehler
 * @since 0.2.0
 */
public interface DataSet extends AutoCloseable {

	/**
	 * Returns {@code true} if the {@link DataSet} has more rows available. (In other words, returns
	 * {@code true} if {@link #nextRow} would return an {@link DataSetRow} rather than throwing an
	 * exception.)
	 *
	 * @return {@code true} if the iteration has more elements
	 */
	boolean hasNext();

	/**
	 * Moves the data pointer to the next row and returns the next {@link DataSetRow}.
	 *
	 * @return the next {@link DataSetRow} loaded by data set
	 * @throws NoSuchElementException
	 *             if the iteration has no more elements
	 * @throws DataSetException
	 *             in case loading the next row fails (e.g. because of a broken network connection)
	 */
	DataSetRow nextRow() throws DataSetException, NoSuchElementException;

	/**
	 * @return the 0-based index of the current row. Returns {@code -1} in case {@link #nextRow()}
	 *         has not been called yet.
	 */
	int getCurrentRowIndex();

	/**
	 * Resets the data row pointer to the beginning (i.e. {@link #getCurrentRowIndex()} will return
	 * {@code -1}) and allows to re-read the data.
	 *
	 * @throws DataSetException
	 *             in case the data row pointer reset fails
	 */
	void reset() throws DataSetException;

	/**
	 * @return the number of columns for this {@link DataSet}.
	 */
	int getNumberOfColumns();

	/**
	 * @return the number of rows for this {@link DataSet}. In case the number is not known
	 *         {@code -1} will be returned.
	 */
	int getNumberOfRows();

	@Override
	void close() throws DataSetException;

}
