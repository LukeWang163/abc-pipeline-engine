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

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * Utility class to encrypt and decrypt licenses. We do not use static variables here as decompiling
 */
final class LicenseLocationUtils {

	private static final int TAG_LENGTH_BIT = 128;
	private static final int IV_LENGTH_BYTE = 16;

	/**
	 * The prefix defines the cipher algorithm we use to encrypt/decrypt the licenses.
	 *
	 * {1} => AES/GCM/NoPadding
	 */
	private static final String PREFIX = "{1}";

	private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
	private static final String DIGEST_ALGORITHM = "SHA-256";
	private static final String KEY_ALGORITHM = "AES";

	private LicenseLocationUtils() {
		throw new IllegalStateException("Static class");
	}

	/**
	 * Uses the {@value CIPHER_ALGORITHM} to encrypt the provided value. The encrypted value will be prefixed by
	 * {@value PREFIX} so that the encrypted value can be distinguished easily. If the value is already encrypted
	 * the encrypted value will be returned.
	 *
	 * @param value
	 * 		the value to be encrypted
	 * @param encryptionKey
	 * 		the key that will be used to encrypt the value
	 * @return the encrypted value or {@code null} in case an empty value was provided
	 */
	static String encrypt(String value, String encryptionKey) {
		String encryptedVal;
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		// do not encrypt already encrypted values
		if (value.startsWith(PREFIX)) {
			return value;
		}
		try {
			// Generating the initialization vector needed by the GCM mode
			byte[] iv = new byte[IV_LENGTH_BYTE];
			SecureRandom random = new SecureRandom();
			random.nextBytes(iv);

			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

			// Encrypt value
			final Key key = generateKeyFromString(encryptionKey);
			final Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
			c.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
			final byte[] encrypted = c.doFinal(value.getBytes());

			// Combine IV and encrypted part.
			byte[] encryptedIVAndText = new byte[IV_LENGTH_BYTE + encrypted.length];
			System.arraycopy(iv, 0, encryptedIVAndText, 0, IV_LENGTH_BYTE);
			System.arraycopy(encrypted, 0, encryptedIVAndText, IV_LENGTH_BYTE, encrypted.length);

			// Encode to Base64 and attach prefix
			encryptedVal = PREFIX + Base64.getEncoder().encodeToString(encryptedIVAndText);
		} catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException
				| NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | RuntimeException e) {
			throw new IllegalArgumentException("Could not encrypt provided value. Reason: " + e.getMessage(), e);
		}

		return encryptedVal;
	}

	/**
	 * Uses the {@value CIPHER_ALGORITHM} to decrypt the provided value. If the value is not prefixed with
	 * {@value PREFIX} the provided value will be returned.
	 *
	 * @param value
	 * 		the value to decrypt
	 * @param decryptionKey
	 * 		the key that will be used to decrypt the value
	 * @return the decrypted value or {@code null} in case an empty value was provided.
	 */
	static String decrypt(String value, String decryptionKey) {
		String decryptedValue;
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		// do not decrypt the value if it does not start with the encryption prefix
		if (!value.startsWith(PREFIX)) {
			return value;
		}
		try {
			// Strip prefix from value
			String valueWithoutPrefix = value.substring(PREFIX.length());
			final byte[] valueWithoutPrefixBytes = valueWithoutPrefix.getBytes(StandardCharsets.UTF_8);
			final byte[] encryptedIvTextBytes = Base64.getDecoder().decode(valueWithoutPrefixBytes);

			// Extract IV
			byte[] iv = new byte[IV_LENGTH_BYTE];
			System.arraycopy(encryptedIvTextBytes, 0, iv, 0, iv.length);
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

			// Extract encrypted part.
			int encryptedSize = encryptedIvTextBytes.length - IV_LENGTH_BYTE;
			byte[] encryptedBytes = new byte[encryptedSize];
			System.arraycopy(encryptedIvTextBytes, IV_LENGTH_BYTE, encryptedBytes, 0, encryptedSize);

			// Decrypt
			final Key key = generateKeyFromString(decryptionKey);
			final Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
			c.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
			final byte[] decValue = c.doFinal(encryptedBytes);
			decryptedValue = new String(decValue, StandardCharsets.UTF_8);
		} catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException
				| NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | RuntimeException e) {
			throw new IllegalArgumentException("Could not decrypt provided value. Reason: " + e.getMessage(), e);
		}

		return decryptedValue;
	}

	private static Key generateKeyFromString(String key) throws NoSuchAlgorithmException {
		if (key == null || key.trim().isEmpty()) {
			throw new IllegalArgumentException("No encryption key provided");
		}

		byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
		MessageDigest sha = MessageDigest.getInstance(DIGEST_ALGORITHM);
		byte[] digestedKeyBytes = sha.digest(keyBytes);

		// Use only first 128 bit. A stronger key would require
		// Java cryptography extension (JCE) unlimited strength jurisdiction policy files
		// to be installed.
		byte[] finalKey = Arrays.copyOf(digestedKeyBytes, IV_LENGTH_BYTE);
		return new SecretKeySpec(finalKey, KEY_ALGORITHM);
	}

	/**
	 * Encrypts the provided value(s) with the provided key and prints the result on command line
	 *
	 * @param args
	 * 		the program arguments
	 */
	public static void main(String... args) {
		if (args.length < 2) {
			throw new IllegalStateException("Could not encrypt provided values as no values were provided. "
					+ "Expected following arguments: ENCRYPTION_KEY, LICENSE_1, LICENSE_2, ...");
		}
		String encryptionKey = args[0];
		Arrays.asList(args).subList(1, args.length).forEach(arg -> System.out.println(encrypt(arg, encryptionKey)));
	}
}
