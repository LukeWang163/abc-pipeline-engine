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

import java.nio.file.Path;


/**
 * A {@link FileDataSource} is a {@link DataSource} that loads data from local files.
 *
 * @author Nils Woehler
 * @since 0.2.0
 */
public abstract class FileDataSource implements DataSource {

	private Path location;

	/**
	 * Updates the location of the {@link FileDataSource}.
	 *
	 * @param fileLocation
	 *            the new file location
	 */
	public void setLocation(Path fileLocation) {
		this.location = fileLocation;
	};

	/**
	 * @return the current selected location. Will return {@code null} in case no location has been
	 *         specified yet.
	 */
	public Path getLocation() {
		return location;
	};

}
