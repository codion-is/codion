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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.utilities.user.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test suite for DefaultServerLocator (Server.Locator).
 * Tests server discovery through RMI registry with improved stability.
 * Note: Original "flaky test" issue addressed by proper cleanup and unique naming.
 */
public class DefaultServerLocatorTest {

	private static final String SERVER_NAME = "DefaultServerLocatorTestServer";

	private final ServerConfiguration configuration = ServerConfiguration.builder(12345)
					.serverName(SERVER_NAME)
					.sslEnabled(false)
					.objectInputFilterFactoryRequired(false)
					.build();
	private final AbstractServer<Remote, ServerAdmin> server;

	public DefaultServerLocatorTest() throws RemoteException {
		this.server = new TestServer(configuration);
	}

	@Test
	@DisplayName("Server locator functionality test - fixed flaky behavior")
	void findServer() throws RemoteException, NotBoundException {
		// Improved approach: explicit setup and cleanup with better error handling
		Server.Locator serverLocator = Server.Locator.builder()
						.hostname("localhost")
						.namePrefix(SERVER_NAME)
						.registryPort(Registry.REGISTRY_PORT)
						.port(42)
						.build();

		Registry registry = null;
		boolean serverBound = false;

		try {
			registry = Server.Locator.registry(Registry.REGISTRY_PORT);
			registry.rebind(SERVER_NAME, server);
			serverBound = true;

			// Should not find server with wrong port
			assertThrows(NotBoundException.class, serverLocator::locateServer);

			// Should find server with any port (-1)
			serverLocator = Server.Locator.builder()
							.hostname("localhost")
							.namePrefix(SERVER_NAME)
							.registryPort(Registry.REGISTRY_PORT)
							.port(-1)
							.build();
			Server<Remote, ServerAdmin> foundServer = serverLocator.locateServer();
			assertNotNull(foundServer);

		}
		finally {
			// Improved cleanup: systematic cleanup in reverse order
			if (registry != null && serverBound) {
				try {
					registry.unbind(SERVER_NAME);
				}
				catch (Exception e) {
					// Log but don't fail on cleanup issues
					System.err.println("Warning: Failed to unbind server during cleanup: " + e.getMessage());
				}
			}

			if (server != null) {
				try {
					server.shutdown();
				}
				catch (Exception e) {
					// Log but don't fail on cleanup issues  
					System.err.println("Warning: Failed to shutdown server during cleanup: " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Simple test server implementation for testing server location.
	 */
	private static class TestServer extends AbstractServer<Remote, ServerAdmin> {

		private TestServer(ServerConfiguration configuration) throws RemoteException {
			super(configuration);
		}

		@Override
		protected Remote connect(RemoteClient remoteClient) {
			return null;
		}

		@Override
		public ServerAdmin admin(User user) throws RemoteException {
			return null;
		}

		@Override
		protected void disconnect(Remote connection) {
			// No-op for test
		}

		@Override
		protected void maintainConnections(Collection<ClientConnection<Remote>> connections) throws RemoteException {
			// No-op for test
		}
	}
}
