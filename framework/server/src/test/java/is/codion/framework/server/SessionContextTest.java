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

import is.codion.common.db.database.Database;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.ConnectionHolder;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.server.TestDomain.Employee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class SessionContextTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@AfterEach
	void tearDown() {
		TestSessionContext.disarm();
		TestClientTypeSessionContext.disarm();
	}

	@Test
	void appliedAndRemovedPerCheckOut() throws Exception {
		TestSessionContext.arm();
		EntityServer server = EntityServer.startServer(configure());
		try {
			EntityConnection connection = connection();
			//entities() is served from the domain, no connection checked out, so nothing to prepare
			assertTrue(TestSessionContext.CALLS.isEmpty());

			connection.select(all(Employee.TYPE));
			assertEquals(asList("prepare:scott@SessionContextTest", "release:scott@SessionContextTest"),
							TestSessionContext.CALLS);

			//pooled, so the pairing repeats for every invocation
			connection.select(all(Employee.TYPE));
			assertEquals(4, TestSessionContext.CALLS.size());

			connection.close();
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void cleanReleaseKeepsTheConnection() throws Exception {
		TestSessionContext.arm();
		EntityServer server = EntityServer.startServer(configure());
		try {
			EntityConnection connection = connection();
			connection.select(all(Employee.TYPE));
			connection.select(all(Employee.TYPE));

			//the control for failedReleaseDoesNotBreakTheNextOperation: nothing went wrong, so the pool
			//hands the same connection back out rather than building a new one
			assertEquals(2, TestSessionContext.CONNECTIONS.size());
			assertSame(TestSessionContext.CONNECTIONS.get(0), TestSessionContext.CONNECTIONS.get(1));

			connection.close();
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void failedReleaseDoesNotBreakTheNextOperation() throws Exception {
		TestSessionContext.armFailing(false);
		EntityServer server = EntityServer.startServer(configure());
		try {
			EntityConnection connection = connection();
			//the failure happens on the way back, after the client's operation has succeeded
			assertFalse(connection.select(all(Employee.TYPE)).isEmpty());
			//and the discarded connection does not take the next one with it
			assertFalse(connection.select(all(Employee.TYPE)).isEmpty());
			assertEquals(4, TestSessionContext.CALLS.size());

			//a release which failed leaves state nobody can describe, so the pool must not hand the same
			//physical connection to the next client - identity is the only way to see that from here
			assertEquals(2, TestSessionContext.CONNECTIONS.size());
			assertNotSame(TestSessionContext.CONNECTIONS.get(0), TestSessionContext.CONNECTIONS.get(1));

			connection.close();
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void failedPrepareFailsTheOperation() throws Exception {
		TestSessionContext.armFailing(true);
		EntityServer server = EntityServer.startServer(configure());
		try {
			EntityConnection connection = connection();
			//a connection whose session state could not be applied must not be queried, the missing state
			//being exactly what a row level security policy would have relied on
			assertThrows(Exception.class, () -> connection.select(all(Employee.TYPE)));
			//the failing context is unwound, so nothing is left applied
			assertEquals(1, TestSessionContext.CALLS.size());
			assertEquals("prepare:scott@SessionContextTest", TestSessionContext.CALLS.get(0));

			connection.close();
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void sharedAppliedBeforeClientTypeSpecific() throws Exception {
		TestSessionContext.arm();
		TestClientTypeSessionContext.arm();
		EntityServer server = EntityServer.startServer(configure());
		try {
			EntityConnection connection = connection(TestClientTypeSessionContext.CLIENT_TYPE);
			connection.select(all(TestDomain.Employee.TYPE));

			//the specific context sits on top of the general setup and comes off again before it
			assertEquals(asList(
							"prepare:scott@" + TestClientTypeSessionContext.CLIENT_TYPE,
							"prepare:specific",
							"release:specific",
							"release:scott@" + TestClientTypeSessionContext.CLIENT_TYPE),
							TestSessionContext.CALLS);

			connection.close();
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void clientTypeSpecificSkippedForOtherClients() throws Exception {
		TestSessionContext.arm();
		TestClientTypeSessionContext.arm();
		EntityServer server = EntityServer.startServer(configure());
		try {
			EntityConnection connection = connection();
			connection.select(all(TestDomain.Employee.TYPE));

			//a different client type, so only the shared context applies
			assertEquals(asList("prepare:scott@SessionContextTest", "release:scott@SessionContextTest"),
							TestSessionContext.CALLS);

			connection.close();
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void dedicatedConnectionPreparedOnceAndReleasedOnClose() throws Exception {
		TestSessionContext.arm();
		EntityServer server = EntityServer.startServer(configure(false));
		try {
			EntityConnection connection = connection();
			assertTrue(TestSessionContext.CALLS.isEmpty());

			//no pool, so the connection is this client's alone: prepared on first use and not released
			//per invocation, there being no check in to release it on
			connection.select(all(Employee.TYPE));
			connection.select(all(Employee.TYPE));
			assertEquals(singletonList("prepare:scott@SessionContextTest"), TestSessionContext.CALLS);

			connection.close();
			assertEquals(asList("prepare:scott@SessionContextTest", "release:scott@SessionContextTest"),
							TestSessionContext.CALLS);
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void dedicatedConnectionReplacedStartsOver() throws Exception {
		TestSessionContext.arm();
		EntityServer server = EntityServer.startServer(configure(false));
		try {
			EntityConnection connection = connection();
			connection.select(all(Employee.TYPE));
			assertEquals(1, TestSessionContext.CALLS.size());

			//the connection goes bad, its state dies with it, the replacement is prepared anew
			kill(server, connection);
			connection.select(all(Employee.TYPE));
			assertEquals(asList("prepare:scott@SessionContextTest", "prepare:scott@SessionContextTest"),
							TestSessionContext.CALLS);

			//and the count of what was applied does not carry over, the close releasing exactly once
			connection.close();
			assertEquals(asList("prepare:scott@SessionContextTest", "prepare:scott@SessionContextTest",
							"release:scott@SessionContextTest"), TestSessionContext.CALLS);
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void dedicatedConnectionFailedPrepareRetried() throws Exception {
		TestSessionContext.armFailing(true);
		EntityServer server = EntityServer.startServer(configure(false));
		try {
			EntityConnection connection = connection();
			assertThrows(Exception.class, () -> connection.select(all(Employee.TYPE)));
			//the connection is still good, so the next operation must try again rather than run unprepared
			assertThrows(Exception.class, () -> connection.select(all(Employee.TYPE)));
			assertEquals(2, TestSessionContext.CALLS.size());

			TestSessionContext.arm();
			assertFalse(connection.select(all(Employee.TYPE)).isEmpty());
			assertEquals(singletonList("prepare:scott@SessionContextTest"), TestSessionContext.CALLS);

			connection.close();
		}
		finally {
			server.shutdown();
		}
	}

	/**
	 * Closes the JDBC connection under the server side handler, so that the next invocation finds
	 * it gone and replaces it.
	 */
	static void kill(EntityServer server, EntityConnection connection) throws Exception {
		ConnectionHolder holder = server.connection(connection.id()).connectionHandler().connectionHolder();
		Connection jdbcConnection = holder.detach();
		jdbcConnection.close();
		holder.attach(jdbcConnection);
	}

	private static EntityConnection connection() {
		return connection("SessionContextTest");
	}

	private static EntityConnection connection(String clientType) {
		return RemoteEntityConnection.builder()
						.hostname("localhost")
						.port(3623)
						.registryPort(3621)
						.domain(TestDomain.DOMAIN)
						.clientType(clientType)
						.user(UNIT_TEST_USER)
						.build();
	}

	private static EntityServerConfiguration configure() {
		return configure(true);
	}

	private static EntityServerConfiguration configure(boolean pooled) {
		Clients.SERVER_HOSTNAME.set("localhost");
		Clients.TRUSTSTORE.set("src/main/config/truststore.jks");
		Clients.resolveTrustStore();
		ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
		ServerConfiguration.KEYSTORE.set("src/main/config/keystore.jks");
		ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");

		return EntityServerConfiguration.builder()
						.port(3623)
						.registryPort(3621)
						.database(Database.instance())
						.connectionPoolUsers(pooled ? singletonList(UNIT_TEST_USER) : emptyList())
						.domainClasses(singletonList("is.codion.framework.server.TestDomain"))
						//the serial filter is JVM wide and set once, another test class in this JVM owns it
						.objectInputFilterFactoryRequired(false)
						.build();
	}
}
