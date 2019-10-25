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

import base.operators.core.io.data.source.DataSource;

import javax.swing.*;
import javax.swing.event.ChangeListener;


/**
 * An {@link WizardStep} is one logical step in the {@link ImportWizard} dialog like e.g. a the data
 * source type selection or the data source location selection.
 * <p>
 * Apart from having a view which is shown to the user to configure the current step the
 * {@link WizardStep} also controls the dialog buttons (previous, next) and offers method to notify
 * of UI changes via a {@link ChangeListener}.
 *
 * @author Nils Woehler
 * @since 0.2.0
 *
 */
public interface WizardStep {

	/**
	 * A button state defines the current display state of the previous and next buttons of the
	 * {@link ImportWizard}.
	 */
	public enum ButtonState {

		/**
		 * The button is enabled and can be used.
		 */
		ENABLED,

		/**
		 * The button is disabled.
		 */
		DISABLED,

		/**
		 * The button is hidden.
		 */
		HIDDEN
	}

	/**
	 * The I18N key that is used by the {@link ImportWizard} to look-up the current step's title,
	 * description, etc. It is also used as step ID and has to be unique across all
	 * {@link WizardStep}s. <br/>
	 * Following I18N messages need to be added to the GUI.properties file:
	 * <ul>
	 * <li>gui.dialog.io.dataimport.step.[I18NKey].title</li>
	 * <li>gui.dialog.io.dataimport.step.[I18NKey].description</li>
	 * </ul>
	 *
	 * @return the I18N key for this step. It is also used as step ID and has to be unique across
	 *         all {@link WizardStep}s.
	 */
	String getI18NKey();

	/**
	 * Called to retrieve the {@link ButtonState} for the next button every time the registered
	 * {@link ChangeListener} are notified about a {@link WizardStep} change.
	 *
	 * @return the current {@link ButtonState} for the next button
	 */
	ButtonState getNextButtonState();

	/**
	 * Called to retrieve the {@link ButtonState} for the previous button every time the registered
	 * {@link ChangeListener} are notified about a {@link WizardStep} change.
	 *
	 * @return the current {@link ButtonState} for the previous button
	 */
	ButtonState getPreviousButtonState();

	/**
	 * Returns the actual view instance for this {@link ValidatableStep}. The method is called right
	 * after adding the step to the {@link ImportWizard}. Thus some resources (e.g. the wizard's
	 * {@link DataSource}) might not be available yet. Therefore content of the view should be
	 * updated within the {@link #viewWillBecomeVisible(ImportWizard, WizardDirection)} and
	 * {@link #viewWillBecomeInvisible(ImportWizard, WizardDirection)} methods.
	 *
	 * @return the actual view instance
	 */
	JComponent getView();

	/**
	 * Validates whether the view has a valid configuration. It is called each time the list of
	 * {@link ChangeListener}s is informed about UI changes. Therefore must return quickly and
	 * should not block in any case.
	 *
	 * @throws InvalidConfigurationException
	 *             in case the current view does not have a valid configuration yet. The error
	 *             message is not shown automatically within the {@link ImportWizard} dialog.
	 */
	void validate() throws InvalidConfigurationException;

	/**
	 * Called just before this view is about to be shown. It should ensure that the view is in the
	 * correct state and aligned with the current import wizard configuration.
	 * <p>
	 * Furthermore it should be used to update the overall import progress by calling
	 * {@link ImportWizard#setProgress(int)}.
	 *
	 * @param direction
	 *            the direction the step is entered from
	 * @throws InvalidConfigurationException
	 *             in case the view detects a blocking issue that needs to be address before the
	 *             user is allowed to leave the current step. The error message is not shown
	 *             automatically within the {@link ImportWizard} dialog and must be displayed by the
	 *             by the implementer.
	 */
	void viewWillBecomeVisible(WizardDirection direction) throws InvalidConfigurationException;

	/**
	 * Called just before the current view is about to be hidden. It is used to cleanup the used
	 * resources and publish common data to the {@link WizardState} for other steps to follow. This
	 * method is called in a progress thread and thus can contain lengthier checks than
	 * {@link #validate()}.
	 *
	 * @param direction
	 *            the direction the step is left to
	 * @throws InvalidConfigurationException
	 *             in case the view detects a blocking issue that needs to be addressed before the
	 *             user is allowed to leave the current step. The error message is not shown
	 *             automatically within the {@link ImportWizard} dialog and must be displayed by the
	 *             by the implementer.
	 */
	void viewWillBecomeInvisible(WizardDirection direction) throws InvalidConfigurationException;

	/**
	 * Returns the ID of the next {@link WizardStep} returned by the next steps
	 * {@link WizardStep#getI18NKey()} method.
	 *
	 * @return the ID of the next {@link WizardStep} returned by the next steps
	 *         {@link WizardStep#getI18NKey()} method. In case {@code null} is returned the
	 *         {@link ImportWizard} assumes the current step to be the last step of the wizard.
	 *         {@link ImportWizard#CONFIGURE_DATA_STEP_ID} is the ID of the step which allows to
	 *         configure the imported data. It should be returned by the last custom step of a
	 *         {@link DataSource}.
	 */
	String getNextStepID();

	/**
	 * Registers a {@link ChangeListener} for this {@code ChangeableWizardComponent}. The
	 * {@code ChangeListener} will be notified about every configuration change to the
	 * {@code ChangeableWizardComponent}.
	 *
	 * @param listener
	 *            the listener to register
	 */
	void addChangeListener(ChangeListener listener);

	/**
	 * Removes a registered {@link ChangeListener} from this {@code ChangeableWizardComponent}.
	 *
	 * @param listener
	 *            the listener to remove
	 */
	void removeChangeListener(ChangeListener listener);

}
