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
package base.operators.core.concurrency;

/**
 * Unchecked exception indicating that a parallel computation was stopped, e.g., since the
 * surrounding process was stopped or an uncaught exception occurred in one of the concurrent tasks.
 *
 * @author Gisa Schaefer, Michael Knopf
 * @since 0.1
 */
public class ExecutionStoppedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an {@code ExecutionStoppedException} with no detail message. The cause is not
	 * initialized, and may subsequently be initialized by a call to {@link #initCause(Throwable)
	 * initCause}.
	 *
	 * @since 0.1
	 */
	public ExecutionStoppedException() {
		super();
	}

	/**
	 * Constructs an {@code ExecutionStoppedException} with the specified detail message. The cause
	 * is not initialized, and may subsequently be initialized by a call to
	 * {@link #initCause(Throwable) initCause}.
	 *
	 * @param message
	 *            the detail message
	 * @since 0.1
	 */
	protected ExecutionStoppedException(String message) {
		super(message);
	}

	/**
	 * Constructs an {@code ExecutionStoppedException} with the specified detail message and cause.
	 *
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the cause (which is saved for later retrieval by the {@link #getCause()} method)
	 * @since 0.1
	 */
	public ExecutionStoppedException(String message, Throwable cause) {
		super(message, cause);
	}
}
