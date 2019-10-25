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

import java.util.Arrays;
import java.util.List;

import base.operators.license.violation.LicenseLevelViolation;
import base.operators.license.violation.LicenseViolation;


/**
 * A license event is used by the {@link LicenseManagerListener} to notify of events happened within
 * the {@link LicenseManager}.
 *
 * @param <S>
 *            the class a constraint is stored with
 *
 * @param <C>
 *            the class of the checked constraint value
 *
 * @author Nils Woehler
 *
 */
public class LicenseEvent<S, C> {

	/**
	 * The available license event types.
	 */
	public enum LicenseEventType {
		/**
		 * In case a license has been violated
		 */
		LICENSE_VIOLATED,
		/**
		 * In case a license has expired
		 */
		LICENSE_EXPIRED,
		/**
		 * In case the active license has changed
		 */
		ACTIVE_LICENSE_CHANGED,
		/**
		 * In case a new license was stored
		 */
		LICENSE_STORED;
	}

	private final LicenseEventType type;

	// License expired event
	private License expiredLicense;
	private License newLicense;

	// License violations
	private List<LicenseViolation> licenseViolations;

	/**
	 * Creates a {@link LicenseEvent} of type {@link LicenseEventType#LICENSE_EXPIRED}.
	 *
	 * @param expiredLicense
	 *            the expired license
	 * @param newLicense
	 *            the new license
	 * */
	public LicenseEvent(final License expiredLicense, final License newLicense) {
		this.type = LicenseEventType.LICENSE_EXPIRED;
		this.expiredLicense = expiredLicense;
		this.newLicense = newLicense;
	}

	/**
	 * Creates a {@link LicenseEvent} of type {@link LicenseEventType#LICENSE_VIOLATED}.
	 *
	 * @param licenseViolation
	 *            the license violation
	 */
	public LicenseEvent(final LicenseViolation licenseViolation) {
		this.licenseViolations = Arrays.asList(licenseViolation);
		this.type = LicenseEventType.LICENSE_VIOLATED;
	}

	/**
	 * Creates a {@link LicenseEvent} of type {@link LicenseEventType#LICENSE_VIOLATED}.
	 *
	 * @param causes
	 *            the {@link LicenseLevelViolation}s
	 */
	public LicenseEvent(final List<LicenseViolation> causes) {
		this.licenseViolations = causes;
		this.type = LicenseEventType.LICENSE_VIOLATED;
	}

	/**
	 * Creates a {@link LicenseEvent} of type {@link LicenseEventType#ACTIVE_LICENSE_CHANGED} if
	 * activated is <code>true</code>. If activated is <code>false</code> the {@link LicenseEvent}
	 * is of type {@link LicenseEventType#LICENSE_STORED}.
	 *
	 * @param newActiveLicense
	 *            the new license
	 * @param activated
	 *            whether a new license was actived (<code>true</code>) or stored (
	 *            <code>false</code>)
	 */
	public LicenseEvent(final License newActiveLicense, boolean activated) {
		if (activated) {
			this.type = LicenseEventType.ACTIVE_LICENSE_CHANGED;
		} else {
			this.type = LicenseEventType.LICENSE_STORED;
		}
		this.newLicense = newActiveLicense;
	}

	/**
	 * @return the type
	 */
	public LicenseEventType getType() {
		return this.type;
	}

	/**
	 * @return the expired license
	 */
	public License getExpiredLicense() {
		return this.expiredLicense;
	}

	/**
	 * @return the newLicense
	 */
	public License getNewLicense() {
		return this.newLicense;
	}

	/**
	 * @return the list of {@link LicenseViolation}s
	 */
	public List<LicenseViolation> getLicenseViolations() {
		return this.licenseViolations;
	}

}
