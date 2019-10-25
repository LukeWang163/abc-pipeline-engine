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

import base.operators.core.io.data.DataSet;
import base.operators.core.io.data.source.DataSource;
import base.operators.core.io.data.source.DataSourceFactory;

import javax.swing.*;
import javax.swing.event.ChangeListener;


/**
 * The {@link ImportWizard} is a dialog which guides the user through data import into RapidMiner.
 * It consists of various {@link WizardStep}s (from data source selection to data storage) which
 * allows to configure the data import parameters. Besides the different steps it also holds the
 * current {@link DataSource} instance which is responsible for storing the import configuration and
 * accessing the data.
 *
 * @author Nils Woehler
 * @since 0.2.0
 */
public interface ImportWizard {

	/**
	 * The step ID of the {@link ImportWizard} step which allows to configure imported data sets
	 * (e.g. the column names, column roles, column types, etc.).
	 * <p>
	 * The step requires the current ImportWizard {@link DataSource#getPreview()} method to return a
	 * valid {@link DataSet} instance.
	 */
	public static final String CONFIGURE_DATA_STEP_ID = "data_column_configuration";

	/**
	 * The step ID of the {@link ImportWizard} step which allows the user to select a location that
	 * should be used to store either a static data snapshot or a dynamic data reference.
	 * <p>
	 * The step requires the current ImportWizard {@link DataSource#getData()} method to return a
	 * valid {@link DataSet} instance.
	 */
	public static final String STORE_DATA_STEP_ID = "store_data_to_repository";

	/**
	 * Switches to the next {@link WizardStep}. Before switching the current step is informed that
	 * it will be left with the {@link WizardDirection#NEXT} and the next shown step is informed
	 * that is about to be shown with the {@link WizardDirection#NEXT}.
	 */
	void nextStep();

	/**
	 * Switches to the next {@link WizardStep} identified by the provided step ID.
	 *
	 * @param stepId
	 *            the step ID used to lookup the next {@link WizardStep}
	 */
	void nextStep(String stepId);

	/**
	 * Switches to the previous {@link WizardStep}. Before switching the current step is informed
	 * that it will be left with the {@link WizardDirection#PREVIOUS} and the next shown step is
	 * informed that is about to be shown with the {@link WizardDirection#PREVIOUS}.
	 */
	void previousStep();

	/**
	 * Adds a new {@link WizardStep} to this dialog. The step is added to the list of current steps,
	 * the steps view is added to the card panel and a {@link ChangeListener} is registered to the
	 * provided step.
	 *
	 * @param newStep
	 *            the new step to add
	 */
	void addStep(WizardStep newStep);

	/**
	 * Retrieves the data source for this {@link ImportWizard}.
	 *
	 * @param <D>
	 *            the class of the data source
	 * @param dsClass
	 *            the class of the queried data source
	 * @return the {@link DataSource} for this wizard. Might be {@code null} in case no data source
	 *         has been chosen yet by the user.
	 * @throws InvalidConfigurationException
	 *             in case the provided class does not match the actual data source class
	 */
	<D> D getDataSource(Class<? extends D> dsClass) throws InvalidConfigurationException;

	/**
	 * Updates the data source used by the {@link ImportWizard} and calls
	 * {@link DataSourceFactory#createCustomSteps(ImportWizard, DataSource)} on the EDT. If another
	 * data source was specified before, the data source is closed.
	 *
	 * @param <D>
	 *            the class of the data source
	 * @param dataSource
	 *            the new {@link DataSource} instance
	 * @param factory
	 *            the factory that created the data source instance
	 */
	<D extends DataSource> void setDataSource(D dataSource, DataSourceFactory<D> factory);

	/**
	 * Updates the progress bar of the {@link ImportWizard}. The total amount of progress is set to
	 * 100. This method should be called within the
	 * {@link WizardStep#viewWillBecomeVisible(ImportWizard, WizardDirection)} method.
	 *
	 * @param progress
	 *            the new amount of progress (min: 0, max: 100)
	 */
	void setProgress(int progress);

	/**
	 * Returns the actual instance of the {@link ImportWizard} dialog
	 *
	 * @return the dialog instance
	 */
	JDialog getDialog();

}
