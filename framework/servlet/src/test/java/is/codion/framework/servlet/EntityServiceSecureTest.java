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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.servlet;

import is.codion.common.db.database.Database;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Base64;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that enabling {@link EntityService#SECURE} leaves no cleartext connector listening.
 */
public class EntityServiceSecureTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));
	private static final int OK = 200;

	private static EntityServer server;

	@BeforeAll
	public static void setUp() throws Exception {
		Clients.SERVER_HOSTNAME.set("localhost");
		System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
		EntityService.SERIALIZATION.set(false);
		EntityService.SECURE.set(true);
		EntityService.KEYSTORE_PATH.set(new File("../server/src/main/config/keystore.jks").getAbsolutePath());
		EntityService.KEYSTORE_PASSWORD.set("crappypass");

		server = EntityServer.startServer(EntityServerConfiguration.builder()
						.port(3423)
						.registryPort(3421)
						.adminPort(3423)
						.adminUser(User.parse("scott:tiger"))
						.domainClasses(singletonList(TestDomain.class.getName()))
						.database(Database.instance())
						.sslEnabled(false)
						.auxiliaryServerFactory(singletonList(EntityServiceFactory.class.getName()))
						.objectInputFilterFactoryRequired(false)
						.build());
	}

	@AfterAll
	public static void tearDown() {
		if (server != null) {
			server.shutdown();
		}
		System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.FALSE.toString());
		Clients.SERVER_HOSTNAME.set(null);
		ServerConfiguration.AUXILIARY_SERVER_FACTORIES.set(null);
		EntityService.SERIALIZATION.set(false);
		EntityService.SECURE.set(true);
		EntityService.KEYSTORE_PATH.set(null);
		EntityService.KEYSTORE_PASSWORD.set(null);
	}

	@Test
	void securePortServes() throws Exception {
		HttpResponse<byte[]> response = createHttpClient()
						.send(createRequest("https://localhost:" + EntityService.SECURE_PORT.get()), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
	}

	@Test
	void insecurePortIsNotListening() {
		//the whole api, basic authentication credentials included, was served in cleartext alongside the secure connector
		assertThrows(ConnectException.class, () -> createHttpClient()
						.send(createRequest("http://localhost:" + EntityService.PORT.get()), BodyHandlers.ofByteArray()));
	}

	/**
	 * The trust store is loaded into an {@link SSLContext} of our own rather than set as a system property,
	 * the default context being cached from the first use anywhere in the JVM, which another test class may
	 * well have reached first.
	 */
	private static HttpClient createHttpClient() throws Exception {
		KeyStore trustStore = KeyStore.getInstance("pkcs12");
		try (InputStream inputStream = Files.newInputStream(Paths.get("../server/src/main/config/truststore.jks"))) {
			trustStore.load(inputStream, "changeit".toCharArray());
		}
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

		return HttpClient.newBuilder()
						.sslContext(sslContext)
						.build();
	}

	private static HttpRequest createRequest(String baseUrl) {
		return HttpRequest.newBuilder()
						.uri(URI.create(baseUrl + "/entities/json/isTransactionOpen"))
						.POST(BodyPublishers.noBody())
						.headers(new String[] {
										EntityService.DOMAIN_TYPE, TestDomain.DOMAIN.name(),
										EntityService.CLIENT_TYPE, "EntityServiceSecureTest",
										EntityService.CLIENT_ID, UUID.randomUUID().toString(),
										EntityService.CLIENT_VERSION, Version.version().toString(),
										"Authorization", "Basic " + Base64.getEncoder().encodeToString((UNIT_TEST_USER.username()
														+ ":" + String.valueOf(UNIT_TEST_USER.password())).getBytes())
						})
						.build();
	}
}
