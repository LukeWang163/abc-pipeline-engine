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

import java.nio.file.Path;
import java.util.Set;


/**
 * A {@link FileDataSourceFactory} creates {@link FileDataSource}s that read data from local files.
 * It holds information about file extensions and MIME types the data source is associated with and
 * has a method to create a {@link DataSource} instance for a predefined file location.
 *
 * @param <D>
 *            the actual {@link FileDataSource} class
 * @author Nils Woehler
 * @since 0.2.0
 */
public abstract class FileDataSourceFactory<D extends FileDataSource> implements DataSourceFactory<D> {

	private final String i18nKey;
	private final Set<String> fileExtensions;
	private final Set<String> mimeTypes;
	private final String firstStepId;

	/**
	 * Creates a new instance.
	 *
	 * @param i18nKey
	 *            the I18N key for this data source. See {@link DataSourceFactory#getI18NKey()} for
	 *            a more detailed description.
	 * @param mimeTypes
	 *            the MIME types the file data source is associated with
	 * @param fileExtensions
	 *            the file extensions the data source is associated with
	 * @param firstStepId
	 *            the ID of the first step that is shown after the file location has been chosen
	 */
	public FileDataSourceFactory(String i18nKey, Set<String> mimeTypes, Set<String> fileExtensions, String firstStepId) {
		this.i18nKey = i18nKey;
		this.mimeTypes = mimeTypes;
		this.fileExtensions = fileExtensions;
		this.firstStepId = firstStepId;
	}

	@Override
	public final String getI18NKey() {
		return i18nKey;
	}

	@Override
	public final WizardStep createLocationStep(ImportWizard wizard) { // NOPMD
		/*
		 * No need for the FileDataSourceFactory as a common location step is used to select the
		 * file location and file type.
		 */
		return null;
	}

	/**
	 * Creates a new {@link FileDataSource} instance with a preselected file location.
	 *
	 * @param preselectedLocation
	 *            the preselected file location
	 * @return the new {@link FileDataSource} instance
	 */
	public D createNew(Path preselectedLocation) {
		D newDS = createNew();
		newDS.setLocation(preselectedLocation);
		return newDS;
	}

	/**
	 * A set of file extensions this data source is responsible for. When importing files and no
	 * factory with a matching MIME type is found, the file extensions are checked as well. </br>
	 *
	 * @return a set of file extensions the data source is responsible to import. Must not return
	 *         {@code null}.
	 */
	public final Set<String> getFileExtensions() {
		return fileExtensions;
	}

	/**
	 * A set of MIME types this data source is responsible for. When importing files the responsible
	 * {@link FileDataSource} is selected by looking at the file's MIME type first. Only in case no
	 * factory with a matching MIME type was found the file extensions are checked as well.
	 *
	 * @return a set of MIME types the data source is responsible to import. Must not return
	 *         {@code null}.
	 */
	public final Set<String> getMimeTypes() {
		return mimeTypes;
	}

	/**
	 * Returns the ID of the first {@link WizardStep} that is shown after the file location has been
	 * selected.
	 *
	 * @return the ID of the {@link WizardStep} that is shown after the file location has been
	 *         selected
	 */
	public final String getFirstStepId() {
		return firstStepId;
	}

}
