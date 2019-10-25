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


/**
 * A listener that gets informed by the {@link LicenseManager} if a {@link LicenseEvent} (e.g.
 * constraint violated) occurs.
 *
 * @author Nils Woehler
 *
 */
public interface LicenseManagerListener {

	/**
	 * Called whenever a license event has happened. The implementation must not throw an exception
	 * and must not cause any delay!
	 *
	 * @param <S>
	 *            the class a constraint is stored with
	 *
	 * @param <C>
	 *            the class of the checked constraint value
	 *
	 * @param event
	 *            the event that has happened.
	 */
	<S, C> void handleLicenseEvent(LicenseEvent<S, C> event);

}
