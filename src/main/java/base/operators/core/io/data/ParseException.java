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

/**
 * An {@link Exception} that is thrown by methods of {@link DataSetRow} in case the queried data
 * item cannot be parsed.
 *
 * @author Nils Woehler
 * @since 0.2.0
 *
 */
public class ParseException extends Exception {

	private static final long serialVersionUID = 1L;

	private final Integer columnIndex;

	/**
	 * Constructs a new {@link ParseException} instance.
	 *
	 * @param message
	 *            the error message
	 */
	public ParseException(String message) {
		super(message);
		columnIndex = null;
	}

	/**
	 * Constructs a new {@link ParseException} instance.
	 *
	 * @param message
	 *            the error message
	 * @param columnIndex
	 *            the affected columnIndex
	 */
	public ParseException(String message, Integer columnIndex) {
		super(message);
		this.columnIndex = columnIndex;
	}

	/**
	 * Constructs a new {@link ParseException} instance.
	 *
	 * @param message
	 *            the error message
	 * @param cause
	 *            the cause
	 */
	public ParseException(String message, Throwable cause) {
		super(message, cause);
		columnIndex = null;
	}

	/**
	 * Constructs a new {@link ParseException} instance.
	 *
	 * @param message
	 *            the error message
	 * @param cause
	 *            the cause
	 * @param columnIndex
	 *            the affected columnIndex
	 */
	public ParseException(String message, Throwable cause, Integer columnIndex) {
		super(message, cause);
		this.columnIndex = columnIndex;
	}

	/**
	 * @return The column index or {@code null}
	 */
	public Integer getColumnIndex() {
		return columnIndex;
	}
}
