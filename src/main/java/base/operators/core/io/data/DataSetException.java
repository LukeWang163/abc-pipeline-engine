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
 * An {@link Exception} that is thrown by various methods of the {@link DataSource} and the
 * {@link DataSet} in case something goes wrong.
 *
 * @author Nils Woehler
 * @since 0.2.0
 *
 */
public class DataSetException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new {@link DataSetException} with an error message.
	 *
	 * @param message
	 *            the error message
	 */
	public DataSetException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link DataSetException} with an error message and a cause.
	 *
	 * @param message
	 *            the error message
	 * @param cause
	 *            the cause
	 */
	public DataSetException(String message, Throwable cause) {
		super(message, cause);
	}

}
