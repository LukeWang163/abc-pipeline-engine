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
package base.operators.core.io.gui;

/**
 * An exception that indicated that the current configuration a {@link WizardStep} is invalid. It is
 * used as a marker exception and does therefore not display an error in the UI. The
 * {@link WizardStep} has to ensure the error is shown to the user.
 *
 * @author Nils Woehler, Gisa Schaefer
 * @since 0.2.0
 */
public class InvalidConfigurationException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link InvalidConfigurationException} instance.
	 */
	public InvalidConfigurationException() {
		super();
	}

}
