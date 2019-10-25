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
package base.operators.license.location;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A file license location allows to store and read files to disk.
 *
 * @author Nils Woehler
 *
 */
public final class FileLicenseLocation implements LicenseLocation {

	/**
	 * Environment variable for the encryption/decryption key. If set the provided key will be used
	 * to decrypt loaded licenses and encrypt stored licenses.
	 */
	private static final String ENVIRONMENT_VARIABLE = "HEIMDALL";

	private Path userLicenseFolder;
	private final Path[] readOnlyLicenseFolders;
	private final String encryptionKey;

	/**
	 * @param userLicenseFolder
	 *            the folder where the licenses are loaded from and new licenses are stored to
	 * @param readOnlyLicenseFolders
	 *            folders that are only used for reading licenses from
	 */
	public FileLicenseLocation(Path userLicenseFolder, Path... readOnlyLicenseFolders) {
		this.userLicenseFolder = userLicenseFolder;
		this.readOnlyLicenseFolders = readOnlyLicenseFolders;

		// retrieve optional decryption key from the system environment variables
		encryptionKey = System.getenv(ENVIRONMENT_VARIABLE);
	}

	@Override
	public List<String> loadLicenses(String productId) throws LicenseLoadingException {
		final List<String> licenseStrings = new LinkedList<>();
		licenseStrings.addAll(readLicenseFolders(productId, getUserLicenseFolder()));
		for (Path licenseFolder : readOnlyLicenseFolders) {
			licenseStrings.addAll(readLicenseFolders(productId, licenseFolder));
		}
		return licenseStrings;
	}

	/**
	 * @param userLicenseFolder
	 *            sets a new user license folder
	 */
	public void setUserLicenseFolder(Path userLicenseFolder) {
		this.userLicenseFolder = userLicenseFolder;
	}

	private List<String> readLicenseFolders(final String productId, Path licenseFolder) throws LicenseLoadingException {
		final List<String> licenseStrings = new LinkedList<>();

		if (Files.isDirectory(licenseFolder)) {
			// no subfolder for product exists yet -> no licenses to return
			if (!Files.isDirectory(licenseFolder.resolve(productId))) {
				return licenseStrings;
			}
			try {
				Files.walkFileTree(licenseFolder.resolve(productId), new SimpleFileVisitor<Path>() {

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						String fileName = file.getFileName().toString();
						if (fileName.endsWith(".lic")) {
							try {
								licenseStrings.add(readFile(file.toFile()));
							} catch (IOException e) {
								Logger.getLogger(FileLicenseLocation.class.getSimpleName()).log(Level.WARNING,
										"Could not read license file '" + file.toString() + "'");
							}
						}
						return FileVisitResult.CONTINUE;
					}

				});
			} catch (IOException e) {
				// cannot happen
			}
		} else {
			throw new LicenseLoadingException("License folder is not a folder!");
		}

		return licenseStrings;
	}

	private Path getUserLicenseFolder() throws LicenseLoadingException {
		if (!Files.exists(userLicenseFolder)) {
			try {
				Files.createDirectory(userLicenseFolder);
			} catch (IOException e) {
				throw new LicenseLoadingException("Cannot create licenses folder.", e);
			}
		}
		return userLicenseFolder;
	}

	private String readFile(File file) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				line = br.readLine();
			}
			String licenseContent = sb.toString();
			if (encryptionKey != null) {
				licenseContent = LicenseLocationUtils.decrypt(licenseContent, encryptionKey);
			}
			return licenseContent;
		}
	}

	@Override
	public void storeLicense(String productId, String productVersion, String edition, LocalDate start, LocalDate end,
							 String licenseString) throws LicenseStoringException {
		String finalLicenseString = licenseString;
		if (encryptionKey != null) {
			finalLicenseString = LicenseLocationUtils.encrypt(licenseString, encryptionKey);
		}

		try {
			StringBuilder fileNameBuilder = new StringBuilder(20);
			fileNameBuilder.append(productId);
			if (productVersion != null) {
				fileNameBuilder.append(" ");
				fileNameBuilder.append(productVersion);
			}
			fileNameBuilder.append(" [");
			fileNameBuilder.append(edition);
			fileNameBuilder.append("] ");
			if (start != null) {
				fileNameBuilder.append(DateTimeFormatter.BASIC_ISO_DATE.format(start));
			} else {
				fileNameBuilder.append("now");
			}
			fileNameBuilder.append("-");
			if (end != null) {
				fileNameBuilder.append(DateTimeFormatter.BASIC_ISO_DATE.format(end));
			} else {
				fileNameBuilder.append("forever");
			}
			fileNameBuilder.append(".lic");

			if (!Files.exists(getUserLicenseFolder().resolve(productId))) {
				try {
					Files.createDirectory(getUserLicenseFolder().resolve(productId));
				} catch (IOException e) {
					throw new LicenseLoadingException("Cannot create license product folder.", e);
				}
			}
			Path licenseFile = getUserLicenseFolder().resolve(productId).resolve(fileNameBuilder.toString());
			Files.write(licenseFile, finalLicenseString.getBytes(Charset.forName("UTF-8")));
		} catch (IOException | LicenseLoadingException e) {
			throw new LicenseStoringException("Could not store license.", e);
		}
	}
}
