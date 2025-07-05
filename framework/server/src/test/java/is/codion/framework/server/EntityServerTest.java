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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.logging.MethodTrace;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.SerializationFilterFactory;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.server.ConfigureDb.Configured;
import is.codion.framework.server.TestDomain.Employee;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.util.Collection;
import java.util.UUID;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServerTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final User ADMIN_USER = User.parse("scott:tiger");
	private static Server<RemoteEntityConnection, EntityServerAdmin> server;
	private static EntityServerAdmin admin;

	private static final EntityServerConfiguration CONFIGURATION = configure();

	@BeforeAll
	public static synchronized void setUp() throws Exception {
		String serverName = CONFIGURATION.serverName();
		EntityServer.startServer(CONFIGURATION).addAuthenticator(new TestAuthenticator());
		server = (Server<RemoteEntityConnection, EntityServerAdmin>)
						LocateRegistry.getRegistry(Clients.SERVER_HOSTNAME.get(), CONFIGURATION.registryPort()).lookup(serverName);
		admin = server.admin(ADMIN_USER);
	}

	@AfterAll
	public static synchronized void tearDown() throws Exception {
		admin.shutdown();
		server = null;
	}

	@Test
	void customCondition() throws Exception {
		//Fix side effect from remoteEntityConnectionProvider() test,
		//which registers the entities received from the server
		//thus overwriting the entities containing the custom conditions
		new TestDomain();
		ConnectionRequest connectionRequestOne = ConnectionRequest.builder()
						.user(UNIT_TEST_USER)
						.clientType("ClientType")
						.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();
		RemoteEntityConnection connection = server.connect(connectionRequestOne);

		Condition condition = Employee.MGR_CONDITION_TYPE.get(Employee.MGR, 4);

		connection.select(condition);

		connection.close();
	}

	@Test
	void testWrongPassword() {
		assertThrows(ServerAuthenticationException.class, () -> server.connect(ConnectionRequest.builder()
						.user(User.user(UNIT_TEST_USER.username(), "foobar".toCharArray()))
						.clientType(getClass().getSimpleName())
						.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build()));
	}

	@Test
	void serverAdminEmptyPassword() {
		assertThrows(ServerAuthenticationException.class, () -> server.admin(User.user("test", "".toCharArray())));
	}

	@Test
	void serverAdminNullPassword() {
		assertThrows(ServerAuthenticationException.class, () -> server.admin(User.user("test")));
	}

	@Test
	void serverAdminWrongPassword() {
		assertThrows(ServerAuthenticationException.class, () -> server.admin(User.user("test", "test".toCharArray())));
	}

	@Test
	void serverAdminWrongUsername() {
		assertThrows(ServerAuthenticationException.class, () -> server.admin(User.user("test", "test".toCharArray())));
	}

	@Test
	void configureConnection() throws Exception {
		ConnectionRequest connectionRequestTwo = ConnectionRequest.builder()
						.user(UNIT_TEST_USER)
						.clientType("ClientType")
						.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, ConfigureDb.class.getSimpleName()).build();
		try (RemoteEntityConnection connection = server.connect(connectionRequestTwo)) {
			//throws exception if table does not exist, which is created during connection configuration
			connection.select(all(Configured.TYPE));
		}
	}

	@Test
	void test() throws Exception {
		ConnectionRequest connectionRequestOne = ConnectionRequest.builder()
						.user(UNIT_TEST_USER)
						.clientType("ClientType").parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();

		RemoteEntityConnection remoteConnectionOne = server.connect(connectionRequestOne);
		assertTrue(remoteConnectionOne.connected());
		assertEquals(1, admin.connectionCount());
		admin.setPooledConnectionIdleTimeout(UNIT_TEST_USER.username(), 60005);
		assertEquals(60005, admin.getPooledConnectionIdleTimeout(UNIT_TEST_USER.username()));
		admin.setMaximumPoolCheckOutTime(UNIT_TEST_USER.username(), 2005);
		assertEquals(2005, admin.getMaximumPoolCheckOutTime(UNIT_TEST_USER.username()));

		try {
			server.connect(ConnectionRequest.builder().user(UNIT_TEST_USER).clientType("ClientType").build());
			fail();
		}
		catch (LoginException ignored) {}

		try {
			server.connect(ConnectionRequest.builder()
							.user(UNIT_TEST_USER)
							.clientType("ClientType")
							.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE,
											new EmptyDomain().type().name()).build());
			fail();
		}
		catch (LoginException ignored) {}

		ConnectionRequest connectionRequestTwo = ConnectionRequest.builder()
						.user(UNIT_TEST_USER)
						.clientType("ClientType")
						.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();
		RemoteEntityConnection remoteConnectionTwo = server.connect(connectionRequestTwo);
		admin.setLoggingEnabled(connectionRequestTwo.clientId(), true);
		assertTrue(admin.isLoggingEnabled(connectionRequestOne.clientId()));
		assertThrows(IllegalArgumentException.class, () -> admin.isLoggingEnabled(UUID.randomUUID()));
		assertThrows(IllegalArgumentException.class, () -> admin.setLoggingEnabled(UUID.randomUUID(), true));
		assertTrue(remoteConnectionTwo.connected());
		assertEquals(2, admin.connectionCount());
		assertEquals(2, admin.clients().size());

		Collection<User> users = admin.users();
		assertEquals(1, users.size());
		assertEquals(UNIT_TEST_USER, users.iterator().next());

		Select select = Select.all(Employee.TYPE)
						.orderBy(OrderBy.ascending(Employee.NAME))
						.build();
		remoteConnectionTwo.select(select);

		admin.databaseStatistics();

		ClientLog log = admin.clientLog(connectionRequestTwo.clientId());

		MethodTrace entry = log.entries().get(0);
		assertEquals("select", entry.method());
		assertTrue(entry.duration() >= 0);

		admin.disconnectTimedOutClients();

		server.disconnect(connectionRequestOne.clientId());
		assertEquals(1, admin.connectionCount());

		server.disconnect(connectionRequestTwo.clientId());
		assertEquals(0, admin.connectionCount());

		admin.setConnectionLimit(1);
		server.connect(connectionRequestOne);
		try {
			server.connect(connectionRequestTwo);
			fail("Server should be full");
		}
		catch (ConnectionNotAvailableException ignored) {/*ignored*/}

		assertEquals(1, admin.connectionCount());
		admin.setConnectionLimit(2);
		server.connect(connectionRequestTwo);
		assertEquals(2, admin.connectionCount());

		server.disconnect(connectionRequestOne.clientId());
		assertEquals(1, admin.connectionCount());
		server.disconnect(connectionRequestTwo.clientId());
		assertEquals(0, admin.connectionCount());

		//testing with the TestAuthenticator
		admin.setConnectionLimit(3);
		assertEquals(3, admin.getConnectionLimit());
		String testClientType = "TestAuthenticator";
		User john = User.parse("john:hello");
		ConnectionRequest connectionRequestJohn = ConnectionRequest.builder()
						.user(john)
						.clientType(testClientType)
						.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();
		ConnectionRequest connectionRequestHelen = ConnectionRequest.builder()
						.user(User.parse("helen:juno"))
						.clientType(testClientType)
						.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();
		ConnectionRequest connectionRequestInvalid = ConnectionRequest.builder()
						.user(User.parse("foo:bar"))
						.clientType(testClientType)
						.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();
		server.connect(connectionRequestJohn);
		server.connect(connectionRequestHelen);
		try {
			server.connect(connectionRequestInvalid);
			fail("Should not be able to connect with an invalid user");
		}
		catch (LoginException ignored) {/*ignored*/}
		Collection<RemoteClient> employeesClients = admin.clients();
		assertEquals(2, employeesClients.size());
		for (RemoteClient employeesClient : employeesClients) {
			assertEquals(UNIT_TEST_USER, employeesClient.databaseUser());
		}
		server.disconnect(connectionRequestJohn.clientId());
		assertEquals(1, admin.connectionCount());
		server.disconnect(connectionRequestHelen.clientId());
		assertEquals(0, admin.connectionCount());
	}

	@Test
	void remoteEntityConnectionProvider() throws Exception {
		RemoteEntityConnectionProvider provider =
						RemoteEntityConnectionProvider.builder()
										.hostName("localhost")
										.port(CONFIGURATION.port())
										.registryPort(CONFIGURATION.registryPort())
										.domain(TestDomain.DOMAIN)
										.clientType("TestClient")
										.user(UNIT_TEST_USER)
										.build();

		assertEquals(EntityConnectionProvider.CONNECTION_TYPE_REMOTE, provider.connectionType());

		EntityConnection db = provider.connection();
		assertNotNull(db);
		assertTrue(db.connected());
		provider.close();

		//not available until a connection has been requested
		assertEquals(Clients.SERVER_HOSTNAME.get(), provider.hostName());

		EntityConnection db2 = provider.connection();
		assertNotNull(db2);
		assertNotSame(db, db2);
		assertTrue(db2.connected());
		provider.close();

		EntityConnection db3 = provider.connection();
		assertTrue(db3.connected());
		admin.disconnect(provider.clientId());
		assertFalse(db3.connected());

		db3 = provider.connection();
		assertTrue(db3.connected());
		db3.close();

		provider.close();
		assertFalse(provider.connectionValid());
		db3 = provider.connection();
		assertTrue(provider.connectionValid());
		db3.close();
	}

	@Test
	void clientLogNotConnected() {
		assertThrows(IllegalArgumentException.class, () -> admin.clientLog(UUID.randomUUID()));
	}

	private static EntityServerConfiguration configure() {
		Clients.SERVER_HOSTNAME.set("localhost");
		Clients.TRUSTSTORE.set("src/main/config/truststore.jks");
		Clients.resolveTrustStore();
		ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
		ServerConfiguration.KEYSTORE.set("src/main/config/keystore.jks");
		ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");
		SerializationFilterFactory.SERIALIZATION_FILTER_PATTERN_FILE.set("classpath:serialization-whitelist-test.txt");

		return EntityServerConfiguration.builder(3223, 3221)
						.adminPort(3223)
						.adminUser(User.parse("scott:tiger"))
						.database(Database.instance())
						.connectionPoolUsers(singletonList(UNIT_TEST_USER))
						.clientTypeIdleConnectionTimeouts(singletonMap("ClientType", 10000))
						.domainClassNames(asList("is.codion.framework.server.TestDomain", "is.codion.framework.server.ConfigureDb"))
						.clientLogging(true)
						.sslEnabled(true)
						.objectInputFilterFactoryClassName(SerializationFilterFactory.class.getName())
						.build();
	}

	public static class EmptyDomain extends DomainModel {
		private EmptyDomain() {
			super(DomainType.domainType(EmptyDomain.class));
		}
	}
}
