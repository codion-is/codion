/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2015 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.client;

import is.codion.common.property.PropertyValue;

import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.KeyStoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.KeyStore;

import static is.codion.common.Configuration.stringValue;

/**
 * Utility methods for remote clients
 */
public final class Clients {

	private static final Logger LOG = LoggerFactory.getLogger(Clients.class);

	/**
	 * The system property key for specifying a ssl truststore
	 */
	public static final String JAVAX_NET_TRUSTSTORE = "javax.net.ssl.trustStore";

	/**
	 * The system property key for specifying a ssl truststore password
	 */
	public static final String JAVAX_NET_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

	/**
	 * The rmi ssl truststore to use
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 */
	public static final PropertyValue<String> TRUSTSTORE = stringValue("codion.client.trustStore");

	/**
	 * The rmi ssl truststore password to use
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 */
	public static final PropertyValue<String> TRUSTSTORE_PASSWORD = stringValue("codion.client.trustStorePassword");

	/**
	 * The host on which to locate the server
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: localhost
	 * </ul>
	 */
	public static final PropertyValue<String> SERVER_HOSTNAME = stringValue("codion.server.hostname", "localhost");

	private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";

	private Clients() {}

	/**
	 * Reads the trust store and password specified by the 'codion.client.trustStore' and 'codion.client.trustStorePassword'
	 * system properties and if a truststore is specified, either in the filesystem or on the classpath, combines it with the default
	 * system truststore, writes the combined truststore to a temporary file and sets 'javax.net.ssl.trustStore'
	 * so that it points to that file and 'javax.net.ssl.trustStorePassword' to the given password.
	 * If no password is provided, the default 'changeit' password is used.
	 * If no truststore is specified or the file is not found, this method has no effect.
	 * @throws IllegalArgumentException in case a truststore is specified but no password
	 * @see Clients#TRUSTSTORE
	 * @see Clients#TRUSTSTORE_PASSWORD
	 */
	public static void resolveTrustStore() {
		String trustStorePath = TRUSTSTORE.get();
		if (trustStorePath == null || trustStorePath.isEmpty()) {
			LOG.warn("No truststore specified via {}", TRUSTSTORE.propertyName());
			return;
		}
		String password = TRUSTSTORE_PASSWORD.optional().orElse(DEFAULT_TRUSTSTORE_PASSWORD);
		SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder()
						.withDefaultTrustMaterial();
		File trustStore = new File(trustStorePath);
		if (trustStore.exists()) {
			sslFactoryBuilder.withTrustMaterial(trustStore.toPath(), password.toCharArray());
		}
		else {
			sslFactoryBuilder.withTrustMaterial(trustStorePath, password.toCharArray());
		}

		X509TrustManager trustManager = sslFactoryBuilder.build()
						.getTrustManager()
						.orElseThrow(() -> new RuntimeException("No TrustManager available after combining truststores"));
		KeyStore store = KeyStoreUtils.createTrustStore(trustManager);
		try {
			File file = File.createTempFile("combinedTrustStore", "tmp");
			file.deleteOnExit();
			try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
				store.store(outputStream, password.toCharArray());
			}
			LOG.debug("Combined trust store written to file: {} -> {}", JAVAX_NET_TRUSTSTORE, file);

			System.setProperty(JAVAX_NET_TRUSTSTORE, file.getPath());
			System.setProperty(JAVAX_NET_TRUSTSTORE_PASSWORD, password);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
