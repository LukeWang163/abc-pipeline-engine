/**
 * Copyright (c) 2013-2018, RapidMiner GmbH, All rights reserved.
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
package base.operators.license;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * The registry holds the current active {@link LicenseManager}.
 *
 * @author Nils Woehler
 * @since 3.1.0
 *
 */
public enum LicenseManagerRegistry {

	/**
	 * The registry instance.
	 */
	INSTANCE;

	private LicenseManager licenseManager = null;

	/**
	 * {@link ReadWriteLock} implementation according to JD of {@link ReentrantReadWriteLock}.
	 */
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Lock readLock = readWriteLock.readLock();
	private final Lock writeLock = readWriteLock.writeLock();

	/**
	 * @return the current active {@link LicenseManager}. Might return <code>null</code> in case the
	 *         {@link LicenseManager} has not been set yet.
	 */
	public LicenseManager get() {
		readLock.lock();
		try {
			return licenseManager;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Sets the {@link LicenseManager} which will be returned by this registry. The license manager
	 * can only be set once. If the method is called if a {@link LicenseManager} is already
	 * registered an exception is thrown.
	 *
	 * @param lm
	 *            the {@link LicenseManager} to register
	 * @throws IllegalStateException
	 *             thrown in case the license manager has already been set
	 */
	public void set(LicenseManager lm) throws IllegalStateException {
		writeLock.lock();
		try {
			if (licenseManager != null) {
				throw new IllegalStateException("License manager already registered.");
			}
			this.licenseManager = lm;
		} finally {
			writeLock.unlock();
		}
	}

}
