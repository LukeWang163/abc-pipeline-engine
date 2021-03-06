/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package base.operators.core.license;

import base.operators.operator.Operator;
import base.operators.license.violation.LicenseConstraintViolation;


/**
 * The exception that is thrown in case the database license constraint is violated.
 *
 * @author Nils Woehler
 *
 */
public class DatabaseConstraintViolationException extends LicenseViolationException {

	private static final long serialVersionUID = 1L;

	private final String databaseURL;

	/**
	 * @param op
	 *            the operator which causes the constraint violation exception
	 * @param violation
	 *            the database constraint violation
	 */
	public DatabaseConstraintViolationException(Operator op, String databaseURL,
                                                @SuppressWarnings("rawtypes") LicenseConstraintViolation violation) {
		super(op, violation);
		this.databaseURL = databaseURL;
	}

	/**
	 * @return the database URL
	 */
	public String getDatabaseURL() {
		return databaseURL;
	}

}
