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

import java.util.Map;


/**
 * A license user is a person or company to which the license is registered to.
 *
 * @author Nils Woehler
 * @since 3.0.0
 *
 */
public interface LicenseUser {

	/**
	 * @return the name of the license user. Might be <code>null</code> in case for starter
	 *         licenses.
	 */
	String getName();

	/**
	 * @return the contect email adress of the license user. Might be <code>null</code> in case for
	 *         starter licenses
	 */
	String getEmail();

	/**
	 * @param key
	 *            the property key
	 * @return the property specified by the key or <code>null</code> if no property is specified
	 *         for the key value
	 */
	String getProperty(String key);

	/**
	 * @return an unmodifiable copy of all license user properties
	 */
	Map<String, String> getProperties();

	/**
	 * Adds a new license user property in case it is not present yet. If the property already
	 * exists, it will be overwritten.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the property value
	 * @return the {@link LicenseUser} object to be able to chain calls
	 */
	LicenseUser putProperty(String key, String value);

	/**
	 * @return a deep copy of the {@link LicenseUser} object
	 */
	LicenseUser copy();
}
