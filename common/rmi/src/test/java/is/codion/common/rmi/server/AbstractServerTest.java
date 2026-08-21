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
 * Copyright (c) 2011 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.ServerAdmin.ServerStatistics;
import is.codion.common.rmi.server.ServerAdmin.ThreadStatistics;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.rmi.server.exception.ServerException;
import is.codion.common.utilities.user.User;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractServerTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));
	private static final int PORT = 1234;
	private static final String CLIENT_TYPE = "AbstractServerTest";

	private static TestServer server;

	@BeforeAll
	static void startServer() throws RemoteException {
		server = new TestServer();
	}

	@AfterAll
	static void stopServer() {
		server.shutdown();
		assertEquals(1, TestAuthenticator.CLOSE_COUNTER.get());
	}

	@Test
	void testConnectionCount() throws RemoteException, ServerException {
		ConnectionRequest connectionRequest = ConnectionRequest.builder().user(UNIT_TEST_USER).clientType(CLIENT_TYPE).build();
		ConnectionRequest connectionRequest2 = ConnectionRequest.builder().user(UNIT_TEST_USER).clientType(CLIENT_TYPE).build();
		ConnectionRequest connectionRequest3 = ConnectionRequest.builder().user(UNIT_TEST_USER).clientType(CLIENT_TYPE).build();
		server.connect(connectionRequest);
		assertEquals(1, server.connectionCount());
		server.connect(connectionRequest2);
		assertEquals(2, server.connectionCount());
		server.disconnect(connectionRequest.connectionId());
		assertEquals(1, server.connectionCount());
		server.connect(connectionRequest);
		assertEquals(2, server.connectionCount());
		server.connect(connectionRequest3);
		assertEquals(3, server.connectionCount());
		server.disconnect(connectionRequest3.connectionId());
		assertEquals(2, server.connectionCount());
		server.disconnect(connectionRequest2.connectionId());
		assertEquals(1, server.connectionCount());
		server.disconnect(connectionRequest.connectionId());
		assertEquals(0, server.connectionCount());
	}

	@Test
	void testConnectionLimitReached() throws RemoteException, ServerException {
		ConnectionRequest connectionRequest = ConnectionRequest.builder().user(UNIT_TEST_USER).clientType(CLIENT_TYPE).build();
		ConnectionRequest connectionRequest2 = ConnectionRequest.builder().user(UNIT_TEST_USER).clientType(CLIENT_TYPE).build();
		server.connectionLimit(1);
		assertEquals(1, server.connectionLimit());
		server.connect(connectionRequest);
		assertThrows(ConnectionNotAvailableException.class, () -> server.connect(connectionRequest2));
		server.disconnect(connectionRequest.connectionId());
		server.connectionLimit(-1);
	}

	@Test
	void testConnect() throws RemoteException, ServerException {
		ConnectionRequest connectionRequest = ConnectionRequest.builder().user(UNIT_TEST_USER).clientType(CLIENT_TYPE).build();
		ServerTest connection = server.connect(connectionRequest);
		assertNotNull(connection);
		ServerTest connection2 = server.connect(connectionRequest);
		assertSame(connection, connection2);
		Map<RemoteSession, ServerTest> connections = server.connections();
		assertEquals(1, connections.size());
		assertSame(connection, server.connection(connectionRequest.connectionId()));

		ServerAdmin admin = server.getAdmin();
		Collection<RemoteSession> sessions = admin.sessions();
		assertFalse(sessions.isEmpty());
		sessions.forEach(session -> assertEquals(0, session.request().user().password().length));
		sessions.forEach(session -> assertEquals(0, session.databaseUser().password().length));
		admin.users().forEach(user -> assertEquals(0, user.password().length));

		RemoteSession session = server.sessions().iterator().next();
		session.request();
		session.clientHost();
		session.request().version();
		session.request().frameworkVersion();
		session.databaseUser();
		session.toString();
		server.disconnect(connectionRequest.connectionId());
		assertThrows(IllegalArgumentException.class, () -> server.connection(connectionRequest.connectionId()));
		ServerTest connection3 = server.connect(connectionRequest);
		assertNotSame(connection, connection3);
		assertNotNull(server.information());
		admin.disconnect(connection3.session().id());
		assertThrows(IllegalArgumentException.class, () -> server.connection(connection3.session().id()));
		assertThrows(NullPointerException.class, () -> server.connect((ConnectionRequest) null));
	}

	@Test
	void testAuthenticator() throws RemoteException, ServerException {
		TestAuthenticator.LOGIN_COUNTER.set(0);
		TestAuthenticator.LOGOUT_COUNTER.set(0);
		TestAuthenticator.CLOSE_COUNTER.set(0);

		ConnectionRequest connectionRequest = ConnectionRequest.builder().user(UNIT_TEST_USER).clientType(CLIENT_TYPE).build();
		ServerTest connection = server.connect(connectionRequest);
		assertNotNull(connection);
		assertEquals(connectionRequest.connectionId(), connection.session().id());

		server.disconnect(connectionRequest.connectionId());

		connection = server.connect(connectionRequest);
		assertEquals(2, TestAuthenticator.LOGIN_COUNTER.get());
		assertNotNull(connection);
		assertEquals(connectionRequest.connectionId(), connection.session().id());

		server.disconnect(connectionRequest.connectionId());
		assertEquals(2, TestAuthenticator.LOGOUT_COUNTER.get());

		connection = server.connect(connectionRequest);
		assertEquals(3, TestAuthenticator.LOGIN_COUNTER.get());
		assertNotNull(connection);
		assertEquals(connectionRequest.connectionId(), connection.session().id());

		server.disconnect(connectionRequest.connectionId());
	}

	@Test
	void authenticatorReturningForeignSession() throws RemoteException, ServerException {
		//the server keys the connection by the id the client knows, so a session for some other
		//connection, however it came about, is a login failure rather than an unreachable connection
		ConnectionRequest connectionRequest = ConnectionRequest.builder().user(UNIT_TEST_USER).clientType(CLIENT_TYPE).build();
		TestAuthenticator.FOREIGN_SESSION = true;
		try {
			assertThrows(LoginException.class, () -> server.connect(connectionRequest));
			assertEquals(0, server.connectionCount());
		}
		finally {
			TestAuthenticator.FOREIGN_SESSION = false;
		}
		server.connect(connectionRequest);
		assertEquals(1, server.connectionCount());
		server.disconnect(connectionRequest.connectionId());
	}

	@Test
	void connectionTheftWrongPassword() throws RemoteException, ServerException {
		UUID connectionId = UUID.randomUUID();
		ConnectionRequest connectionRequest = ConnectionRequest.builder()
						.user(UNIT_TEST_USER)
						.clientType(CLIENT_TYPE)
						.connectionId(connectionId)
						.build();
		ConnectionRequest connectionRequest2 = ConnectionRequest.builder()
						.user(User.user(UNIT_TEST_USER.username(), "test".toCharArray()))
						.clientType(CLIENT_TYPE)
						.connectionId(connectionId)
						.build();

		server.connect(connectionRequest);

		//try to steal the connection using the same connectionId, but incorrect user credentials
		assertThrows(ServerAuthenticationException.class, () -> server.connect(connectionRequest2));

		server.disconnect(connectionRequest.connectionId());
	}

	@Test
	void connectionTheftWrongUsername() throws RemoteException, ServerException {
		UUID connectionId = UUID.randomUUID();
		ConnectionRequest connectionRequest = ConnectionRequest.builder()
						.user(UNIT_TEST_USER)
						.clientType(CLIENT_TYPE)
						.connectionId(connectionId)
						.build();
		ConnectionRequest connectionRequest2 = ConnectionRequest.builder()
						.user(User.user("test", UNIT_TEST_USER.password()))
						.clientType(CLIENT_TYPE)
						.connectionId(connectionId)
						.build();

		server.connect(connectionRequest);

		//try to steal the connection using the same connectionId, but incorrect user credentials
		assertThrows(ServerAuthenticationException.class, () -> server.connect(connectionRequest2));

		server.disconnect(connectionRequest.connectionId());
		System.out.println(server.connectionCount());
	}

	@Test
	void admin() throws RemoteException {
		ServerAdmin admin = server.getAdmin();
		admin.sessions();
		admin.connectionLimit(10);
		admin.connectionLimit();
		admin.users();
		admin.systemProperties();
		ServerInformation serverInformation = admin.serverInformation();
		serverInformation.name();
		serverInformation.id();
		serverInformation.port();
		serverInformation.version();
		serverInformation.locale();
		serverInformation.timeZone();
		serverInformation.startTime();
		ServerStatistics serverStatistics = admin.statistics(System.currentTimeMillis());
		serverStatistics.connectionCount();
		serverStatistics.connectionLimit();
		serverStatistics.usedMemory();
		try {
			ThreadStatistics threadStatistics = serverStatistics.threadStatistics();
			threadStatistics.threadCount();
			threadStatistics.daemonThreadCount();
			threadStatistics.threadStateCount();
		}
		catch (NullPointerException e) {/*See above*/}
		serverStatistics.totalMemory();
		serverStatistics.gcEvents();
		serverStatistics.connectionCount();
		serverStatistics.maximumMemory();
		serverStatistics.connectionLimit();
		serverStatistics.processCpuLoad();
		serverStatistics.systemCpuLoad();
		serverStatistics.requestsPerSecond();
		serverStatistics.timestamp();
	}

	@Test
	void rmiDisabledServerNotExported() throws RemoteException, ServerException {
		TestServer rmiDisabled = new TestServer(ServerConfiguration.builder()
						.rmi(false)
						.port(PORT + 1)
						.serverName("rmiDisabledTestServer")
						.objectInputFilterFactoryRequired(false)
						.build());
		try {
			//the server object is not exported for RMI: unexporting a never-exported object throws
			//(the admin is exported by TestServer regardless, but the server itself is not)
			assertThrows(NoSuchObjectException.class, () -> UnicastRemoteObject.unexportObject(rmiDisabled, true));
			//an in-process connect() still works: no active RMI client host, so it is not treated as a remote call
			ServerTest connection = rmiDisabled.connect(ConnectionRequest.builder()
							.user(UNIT_TEST_USER).clientType("rmiDisabledClient").build());
			assertNotNull(connection);
		}
		finally {
			rmiDisabled.shutdown();
		}
	}

	@Test
	void emptyServerName() {
		assertThrows(IllegalArgumentException.class, () -> ServerConfiguration.builder().port(PORT).serverName((String) null).build());
		assertThrows(IllegalArgumentException.class, () -> ServerConfiguration.builder().port(PORT).serverName("").build());

		assertThrows(IllegalArgumentException.class, () -> new TestServer(ServerConfiguration.builder().port(PORT).serverName(() -> null).build()));
		assertThrows(IllegalArgumentException.class, () -> new TestServer(ServerConfiguration.builder().port(PORT).serverName(() -> "").build()));
	}

	private static class ServerTestImpl implements ServerTest {

		private final RemoteSession session;

		public ServerTestImpl(RemoteSession session) {
			this.session = session;
		}

		@Override
		public RemoteSession session() throws RemoteException {
			return session;
		}
	}

	private interface ServerTest extends Remote {
		RemoteSession session() throws RemoteException;
	}

	private static ServerConfiguration configuration() {
		return ServerConfiguration.builder()
						.port(PORT)
						.serverName("remoteServerTestServer")
						.objectInputFilterFactoryRequired(false)
						.authenticator(TestAuthenticator.class.getName())
						.build();
	}

	private static final class TestServer extends AbstractServer<ServerTest, ServerAdmin> {

		private static final ServerConfiguration CONFIGURATION = configuration();

		private TestServer() throws RemoteException {
			this(CONFIGURATION);
		}

		private TestServer(ServerConfiguration configuration) throws RemoteException {
			super(configuration);
			setAdmin(new AbstractServerAdmin(this, configuration));
		}

		@Override
		protected ServerTest connect(RemoteSession session) {
			return new ServerTestImpl(session);
		}

		@Override
		public ServerAdmin admin(User user) throws RemoteException {
			return getAdmin();
		}

		@Override
		protected void disconnect(ServerTest connection) {}

		@Override
		protected void maintainConnections(Collection<SessionConnection<ServerTest>> connections) throws RemoteException {}
	}

	public static final class TestAuthenticator implements Authenticator {

		static final AtomicInteger LOGIN_COUNTER = new AtomicInteger();
		static final AtomicInteger LOGOUT_COUNTER = new AtomicInteger();
		static final AtomicInteger CLOSE_COUNTER = new AtomicInteger();
		static volatile boolean FOREIGN_SESSION = false;

		@Override
		public Optional<String> clientType() {
			return Optional.of(CLIENT_TYPE);
		}

		@Override
		public RemoteSession login(RemoteSession session) {
			LOGIN_COUNTER.incrementAndGet();
			if (FOREIGN_SESSION) {
				//a session for some other connection, which the server must refuse
				return RemoteSession.builder(ConnectionRequest.builder()
												.user(session.request().user())
												.clientType(session.request().clientType())
												.build())
								.clientHost(session.clientHost())
								.build();
			}

			return session;
		}

		@Override
		public void logout(RemoteSession session) {
			LOGOUT_COUNTER.incrementAndGet();
		}

		@Override
		public void close() {
			CLOSE_COUNTER.incrementAndGet();
		}
	}
}
