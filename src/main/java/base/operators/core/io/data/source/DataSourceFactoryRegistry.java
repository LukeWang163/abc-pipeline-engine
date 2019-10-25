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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * The {@link DataSourceFactoryRegistry} is used to register new {@link DataSourceFactory}s and to
 * retrieve all current available {@link DataSourceFactory}s.
 *
 * @author Nils Woehler
 * @since 0.2.0
 *
 */
public enum DataSourceFactoryRegistry {

	/**
	 * The factory registry instance.
	 *
	 */
	INSTANCE;

	/**
	 * Stores the instances of all registered {@link DataSourceFactory}s.
	 */
	private final List<DataSourceFactory<?>> factories = new LinkedList<>();

	/**
	 * Stores the instances of registered {@link FileDataSourceFactory}s.
	 */
	private final List<FileDataSourceFactory<?>> fileFactories = new LinkedList<>();

	/**
	 * Registers a new {@link DataSourceFactory} instance. The factory will be part of the list
	 * returned by {@link #getFactories()} afterwards.
	 *
	 * @param newFactory
	 *            the new factory to be registered
	 * @throws IllegalArgumentException
	 *             in case a {@code null} factory is provided or a factory with the same I18N key or
	 *             the same {@link DataSource} class as the provided factory has been registered
	 *             already.
	 */
	public void register(DataSourceFactory<?> newFactory) throws IllegalArgumentException {
		synchronized (factories) {
			if (newFactory == null) {
				throw new IllegalArgumentException("null factory is not allowed");
			}

			// do not allow duplicate registration for same data source classes or I18N keys
			for (DataSourceFactory<?> fact : getFactories()) {
				if (fact.getDataSourceClass().equals(newFactory.getDataSourceClass())) {
					throw new IllegalArgumentException(String.format(
							"Cannot register factory. A factory for the DataSource class %s has been registered already.",
							fact.getDataSourceClass().getName()));
				}
				if (fact.getI18NKey().equals(newFactory.getI18NKey())) {
					throw new IllegalArgumentException(String.format(
							"Cannot register factory. A factory with the I18N key %s has been registered already.",
							fact.getI18NKey()));
				}
			}
			this.factories.add(newFactory);
		}
	}

	/**
	 * Registers a new {@link FileDataSourceFactory} instance. Afterwards the registered
	 * {@link FileDataSourceFactory} will be part of the list returned by
	 * {@link #getFileFactories()}.
	 *
	 * @param newFactory
	 *            the new file data source factory
	 * @throws IllegalArgumentException
	 *             in case a {@code null} factory is provided or a factory with the same I18N key or
	 *             the same {@link DataSource} class as the provided factory has been registered
	 *             already. Furthermore this exception is thrown in case a
	 *             {@link FileDataSourceFactory} has been registered already that is associated with
	 *             the the same file extension or MIME type as the provided factory.
	 */
	public void register(FileDataSourceFactory<?> newFactory) throws IllegalArgumentException {
		synchronized (fileFactories) {
			if (newFactory == null) {
				throw new IllegalArgumentException("null factory is not allowed");
			}

			// do not allow duplicate registration for same file extensions or mime types
			for (FileDataSourceFactory<?> fact : fileFactories) {
				if (fact.getDataSourceClass().equals(newFactory.getDataSourceClass())) {
					throw new IllegalArgumentException(String.format(
							"Cannot register factory. A factory for the FileDataSource class %s has been registered already.",
							fact.getDataSourceClass().getName()));
				}
				if (fact.getI18NKey().equals(newFactory.getI18NKey())) {
					throw new IllegalArgumentException(String.format(
							"Cannot register factory. A factory with the I18N key %s has been registered already.",
							fact.getI18NKey()));
				}
				for (String fileExtension : fact.getFileExtensions()) {
					if (newFactory.getFileExtensions().contains(fileExtension)) {
						throw new IllegalArgumentException(String.format(
								"Cannot register factory. A factory for the file extension %s has been registered already.",
								fileExtension));
					}
				}
				for (String mimeType : fact.getMimeTypes()) {
					if (newFactory.getMimeTypes().contains(mimeType)) {
						throw new IllegalArgumentException(String.format(
								"Cannot register factory. A factory for the MIME type %s has been registered already.",
								mimeType));
					}
				}
			}

			// add file data source to file data source list
			fileFactories.add(newFactory);
		}
	}

	/**
	 * @return an unmodifiable list of all registered {@link DataSourceFactory}s </br>
	 *         (except subclasses of {@link FileDataSourceFactory} which are returned by
	 *         {@link #getFileFactories()})
	 */
	public List<DataSourceFactory<?>> getFactories() {
		synchronized (factories) {
			return Collections.unmodifiableList(factories);
		}

	}

	/**
	 * @return an unmodifiable list of all registered instances of {@link FileDataSourceFactory}s
	 */
	public List<FileDataSourceFactory<? extends FileDataSource>> getFileFactories() {
		synchronized (fileFactories) {
			return Collections.unmodifiableList(fileFactories);
		}
	}

	/**
	 * Looks up the {@link DataSourceFactory} for the provided {@link DataSource} class.
	 *
	 * @param dataSourceClass
	 *            the class that should be used to lookup the according {@link DataSourceFactory}
	 * @return the matching {@link DataSourceFactory} or {@code null} in case no match could be
	 *         found
	 */
	public DataSourceFactory<?> lookUp(Class<?> dataSourceClass) {
		synchronized (factories) {
			for (DataSourceFactory<?> factory : getFactories()) {
				if (factory.getDataSourceClass().equals(dataSourceClass)) {
					return factory;
				}
			}
		}
		synchronized (fileFactories) {
			for (DataSourceFactory<?> factory : getFileFactories()) {
				if (factory.getDataSourceClass().equals(dataSourceClass)) {
					return factory;
				}
			}
		}
		return null;
	}

	/**
	 * Looks up the {@link DataSourceFactory} for the provided I18N key.
	 *
	 * @param i18nKey
	 *            the I18N key that should be used to lookup the according {@link DataSourceFactory}
	 * @return the matching {@link DataSourceFactory} or {@code null} in case no match could be
	 *         found
	 */
	public DataSourceFactory<?> lookUp(String i18nKey) {
		synchronized (factories) {
			for (DataSourceFactory<?> factory : getFactories()) {
				if (factory.getI18NKey().equals(i18nKey)) {
					return factory;
				}
			}
		}
		synchronized (fileFactories) {
			for (DataSourceFactory<?> factory : getFileFactories()) {
				if (factory.getI18NKey().equals(i18nKey)) {
					return factory;
				}
			}
		}
		return null;
	}

	/**
	 * As described in the {@link FileDataSourceFactory#getMimeTypes()} and
	 * {@link FileDataSourceFactory#getFileExtensions()} this method looks up the responsible
	 * {@link FileDataSourceFactory} for the provided file.
	 * <p>
	 * It first uses the provided MIME type to check whether a {@link FileDataSource} for the
	 * provided MIME type is available. If no {@link FileDataSource} for the provided MIME type is
	 * registered it checks whether a {@link FileDataSource} is responsible for the file extension.
	 * If still no match could be found {@code null} is returned.
	 *
	 * @param filePath
	 *            the path to the file which should be imported
	 * @param mimeType
	 *            the MIME type of the file which should be imported
	 * @return the responsible {@link FileDataSourceFactory} or {@code null} if none could be found
	 */
	public FileDataSourceFactory<?> lookUp(Path filePath, String mimeType) {

		// go through file data sources and check for file MIME types first
		for (FileDataSourceFactory<?> factory : fileFactories) {
			if (factory.getMimeTypes().contains(mimeType)) {
				return factory;
			}
		}

		// In case the MIME type is unknown go through file data sources again and check for file
		// ending first
		for (FileDataSourceFactory<?> factory : fileFactories) {
			for (String fileExtension : factory.getFileExtensions()) {
				String glob = String.format("glob:**.%s", fileExtension);
				PathMatcher matcher = FileSystems.getDefault().getPathMatcher(glob);
				if (matcher.matches(filePath)) {
					return factory;
				}
			}
		}

		return null;
	}
}
