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


/**
 * The {@link ColumnMetaData} describes the content of a column of a {@link DataSet}s returned by
 * {@link DataSource}s. It contains information about the name, role and type of the column as well
 * as if this column should be removed.
 *
 * @author Nils Woehler, Gisa Schaefer
 * @since 0.2.0
 */
public interface ColumnMetaData {

	/**
	 * A column type defines the type of a column which specifies on how to import the data.
	 */
	public static enum ColumnType {

		/**
		 * A column that consists of multiple categorical values. Use {@link DataSetRow#getString(int)}
		 * to parse a categorical data item.
		 */
		CATEGORICAL,

		/**
		 * A column that consists of two categorical values. Use {@link DataSetRow#getString(int)} to
		 * parse a binary data item.
		 */
		BINARY,

		/**
		 * A column that consists of numerical values. Use {@link DataSetRow#getDouble(int)} to parse a
		 * data item of type real.
		 */
		REAL,

		/**
		 * A column that consists of integral values. Use {@link DataSetRow#getDouble(int)} to parse a
		 * data item of type integer.
		 */
		INTEGER,

		/**
		 * A column that consists of datetime values. Use {@link DataSetRow#getDate(int)} to parse a
		 * datetime data item
		 */
		DATETIME,

		/**
		 * A column that consists of date values. Use {@link DataSetRow#getDate(int)} to parse a date
		 * data item
		 */
		DATE,

		/**
		 * A column that consists of time values. Use {@link DataSetRow#getDate(int)} to parse a time
		 * data item
		 */
		TIME
	}

	/**
	 * @return the name of the column
	 */
	String getName();

	/**
	 * @return the {@link ColumnType} of the column
	 */
	ColumnType getType();

	/**
	 * @return the role of the column
	 */
	String getRole();

	/**
	 * @return whether this column was removed and thus should not be part of the resulting
	 *         {@link DataSet}
	 */
	boolean isRemoved();

	/**
	 * Sets the column name.
	 *
	 * @param columnName
	 *            the new column name
	 */
	void setName(String columnName);

	/**
	 * Sets the column type.
	 *
	 * @param type
	 *            the new type of the column
	 */
	void setType(ColumnType type);

	/**
	 * Sets the column role.
	 *
	 * @param role
	 *            the new role of the column
	 */
	void setRole(String role);

	/**
	 * Sets whether this column should be removed.
	 *
	 * @param removed
	 *            whether the column should be removed
	 */
	void setRemoved(boolean removed);
}
