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
package base.operators.license.violation;

import base.operators.license.License;
import base.operators.license.annotation.LicenseConstraint;
import base.operators.license.annotation.LicenseLevel;


/**
 * Interface for all possible license violations.
 *
 * @author Nils Woehler
 *
 */
public interface LicenseViolation {

	/**
	 * All possible license violation types.
	 */
	public enum ViolationType {
		/**
		 * In case a {@link LicenseLevel} annotation is violated
		 */
		LICENSE_LEVEL_VIOLATED,
		/**
		 * In case a {@link LicenseConstraint} annotation is violated
		 */
		LICENSE_CONSTRAINT_VIOLATED
	}

	/**
	 * @return the violation type to be able to switch for class casting.
	 */
	ViolationType getViolationType();

	/**
	 * @return the license that has been violated. Can be <code>null</code> in case no license is
	 *         present.
	 */
	License getLicense();

	/**
	 * @return the I18N key specified by the license annotation. Might by <code>null</code> or an
	 *         empty String in case none has been specified.
	 *
	 * @since 3.0.0
	 */
	String getI18nKey();
}
