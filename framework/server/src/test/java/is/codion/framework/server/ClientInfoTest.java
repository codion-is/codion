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
package is.codion.framework.server;

import is.codion.common.db.database.ClientInfo;
import is.codion.common.db.database.Database;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnection;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ClientInfoTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void clientInfo() throws Exception {
		RecordingDatabase database = new RecordingDatabase();
		EntityServer server = EntityServer.startServer(configure(database.database()));
		try {
			EntityConnection connection = RemoteEntityConnection.builder()
							.hostname("localhost")
							.port(3523)
							.registryPort(3521)
							.domain(TestDomain.DOMAIN)
							.clientType("ClientInfoTest")
							.user(UNIT_TEST_USER)
							.build();
			//nothing borrowed yet, entities() is served from the domain without a connection
			assertTrue(database.applied.isEmpty());

			connection.select(all(TestDomain.Employee.TYPE));

			//the pooled connection is stamped with the identity of the client, not with the pool user's
			assertEquals(1, database.applied.size());
			ClientInfo clientInfo = database.applied.get(0);
			assertEquals(UNIT_TEST_USER.username(), clientInfo.user());
			assertEquals("ClientInfoTest", clientInfo.clientType());
			assertTrue(clientInfo.host().isPresent());
			//pooled, so stamped once per invocation, the next borrower overwriting rather than
			//the connection being cleared on its way back
			connection.select(all(TestDomain.Employee.TYPE));
			assertEquals(2, database.applied.size());

			connection.close();
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void clientInfoDedicatedConnection() throws Exception {
		RecordingDatabase database = new RecordingDatabase();
		EntityServer server = EntityServer.startServer(configure(database.database(), false));
		try {
			EntityConnection connection = RemoteEntityConnection.builder()
							.hostname("localhost")
							.port(3523)
							.registryPort(3521)
							.domain(TestDomain.DOMAIN)
							.clientType("ClientInfoTest")
							.user(UNIT_TEST_USER)
							.build();
			assertTrue(database.applied.isEmpty());

			//no pool, so the connection is this client's alone and is stamped once, on first use
			connection.select(all(TestDomain.Employee.TYPE));
			connection.select(all(TestDomain.Employee.TYPE));
			assertEquals(1, database.applied.size());
			assertEquals(UNIT_TEST_USER.username(), database.applied.get(0).user());

			//and again when it has been replaced
			SessionContextTest.kill(server, connection);
			connection.select(all(TestDomain.Employee.TYPE));
			assertEquals(2, database.applied.size());

			connection.close();
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void clientInfoDisabled() throws Exception {
		Database.CLIENT_INFO.set(false);
		RecordingDatabase database = new RecordingDatabase();
		try {
			EntityServer server = EntityServer.startServer(configure(database.database()));
			try {
				EntityConnection connection = RemoteEntityConnection.builder()
								.hostname("localhost")
								.port(3523)
								.registryPort(3521)
								.domain(TestDomain.DOMAIN)
								.clientType("ClientInfoTest")
								.user(UNIT_TEST_USER)
								.build();
				connection.select(all(TestDomain.Employee.TYPE));
				assertTrue(database.applied.isEmpty());
				connection.close();
			}
			finally {
				server.shutdown();
			}
		}
		finally {
			Database.CLIENT_INFO.remove();
		}
	}

	/**
	 * A {@link Database} recording the client info applied to its connections, the dialects which
	 * implement it needing the databases they speak to.
	 */
	private static final class RecordingDatabase implements InvocationHandler {

		private final Database delegate = Database.instance();
		private final List<ClientInfo> applied = synchronizedList(new ArrayList<>());

		private Database database() {
			return (Database) Proxy.newProxyInstance(Database.class.getClassLoader(),
							new Class[] {Database.class}, this);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("clientInfo") && args.length == 2) {
				applied.add((ClientInfo) args[1]);

				return null;
			}
			try {
				return method.invoke(delegate, args);
			}
			catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
	}

	private static EntityServerConfiguration configure(Database database) {
		return configure(database, true);
	}

	private static EntityServerConfiguration configure(Database database, boolean pooled) {
		Clients.SERVER_HOSTNAME.set("localhost");
		Clients.TRUSTSTORE.set("src/main/config/truststore.jks");
		Clients.resolveTrustStore();
		ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
		ServerConfiguration.KEYSTORE.set("src/main/config/keystore.jks");
		ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");

		return EntityServerConfiguration.builder()
						.port(3523)
						.registryPort(3521)
						.database(database)
						.connectionPoolUsers(pooled ? singletonList(UNIT_TEST_USER) : emptyList())
						.domainClasses(singletonList("is.codion.framework.server.TestDomain"))
						//the serial filter is JVM wide and set once, another test class in this JVM owns it
						.objectInputFilterFactoryRequired(false)
						.build();
	}
}
