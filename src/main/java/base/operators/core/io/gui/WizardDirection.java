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
 * The direction a {@link WizardStep} is entered from or left to.
 *
 * @author Nils Woehler
 * @since 0.2.0
 */
public enum WizardDirection {

	/**
	 * First direction when creating the wizard.
	 */
	STARTING,

	/**
	 * The user switches to the next wizard step.
	 */
	NEXT,

	/**
	 * The user switches back to the previous wizard step.
	 */
	PREVIOUS
}
