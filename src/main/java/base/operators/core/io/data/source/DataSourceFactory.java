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
package base.operators.core.io.data.source;

import base.operators.core.io.gui.ImportWizard;
import base.operators.core.io.gui.WizardStep;

import java.util.List;


/**
 * A factory that knows how to create {@link DataSource} instances and the data source location step
 * and other custom steps. Furthermore it holds information about the I18N key used to look-up data
 * source related messages.
 *
 * @param <D>
 *            the type of {@link DataSource} that is created by this factory
 * @author Nils Woehler
 * @since 0.2.0
 *
 */
public interface DataSourceFactory<D extends DataSource> {

	/**
	 * Creates a new unconfigured {@link DataSource} instance.
	 *
	 * @return the new {@link DataSource} instance
	 */
	D createNew();

	/**
	 * Returns the I18N key for the data source created by this factory. The key is used to look-up
	 * the data source's name, icon, description, etc.
	 *
	 * Following I18N messages need to be added to the GUI.properties file:
	 * <ul>
	 * <li>gui.io.dataimport.source.[I18NKey].label</li>
	 * <li>gui.io.dataimport.source.[I18NKey].description</li>
	 * <li>gui.io.dataimport.source.[I18NKey].icon</li>
	 * </ul>
	 *
	 * @return the I18N key used to lookup data source label, icon and description
	 */
	String getI18NKey();

	/**
	 * @return the data source class for this factory. It is used to lookup the correct factory for
	 *         an instance for a {@link DataSource}
	 */
	Class<D> getDataSourceClass();

	/**
	 * Creates and initializes a new {@link WizardStep} that allows to configure the data location
	 * (e.g. file path, database connection, etc.) for the selected {@link DataSource}. It is called
	 * right after the user has selected a data source in the first step of the {@link ImportWizard}
	 * dialog.
	 * <p>
	 * The method is called from the <b>EDT</b>. Therefore it should not take too long to return the
	 * location step.
	 *
	 * @param wizard
	 *            the {@link ImportWizard} the view is created for
	 * @return the new location wizard step
	 */
	WizardStep createLocationStep(ImportWizard wizard);

	/**
	 * Creates and initializes a list of custom steps needed to configure the data import for the
	 * selected {@link DataSource}. The method is called right after the user has selected a data
	 * source in the first step of the {@link ImportWizard} dialog. A step returned by this method
	 * can opened by referencing its step ID within the {@link WizardStep#getNextStepID()} method of
	 * another step.
	 * <p>
	 * The method is called from the <b>EDT</b>. Therefore it should not take too long to return the
	 * list of custom steps.
	 *
	 * @param wizard
	 *            the {@link ImportWizard} the steps are created for
	 * @param dataSource
	 *            the {@link DataSource} the steps are created for
	 *
	 * @return a list of custom {@link WizardStep} for the provided data source instance
	 */
	List<WizardStep> createCustomSteps(ImportWizard wizard, D dataSource);

}
