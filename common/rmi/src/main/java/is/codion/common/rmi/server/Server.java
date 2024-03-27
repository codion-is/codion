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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.UUID;

/**
 * A server for serving remote interfaces
 * @param <C> the type of remote interface this server supplies to clients
 * @param <A> the type of the admin interface this server supplies
 */
public interface Server<C extends Remote, A extends ServerAdmin> extends Remote {

	/**
	 * Specifies the client host connection parameter
	 */
	String CLIENT_HOST = "clientHost";

	/**
	 * Establishes a connection to this Server
	 * @param connectionRequest the information required for establishing a connection
	 * @return a remote connection instance
	 * @throws RemoteException in case of a communication error
	 * @throws ConnectionNotAvailableException in case the server isn't accepting more connections
	 * @throws LoginException in case the login fails
	 */
	C connect(ConnectionRequest connectionRequest) throws RemoteException,
					ConnectionNotAvailableException, LoginException;

	/**
	 * Returns the admin interface used to administer this server
	 * @param user the admin user credentials
	 * @return the admin interface
	 * @throws RemoteException in case of a communication error
	 * @throws ServerAuthenticationException in case authentication fails
	 */
	A serverAdmin(User user) throws RemoteException, ServerAuthenticationException;

	/**
	 * Disconnects the connection identified by the given key.
	 * @param clientId the UUID identifying the client that should be disconnected
	 * @throws RemoteException in case of a communication error
	 */
	void disconnect(UUID clientId) throws RemoteException;

	/**
	 * @return static information about this server
	 * @throws RemoteException in case of an exception
	 */
	ServerInformation serverInformation() throws RemoteException;

	/**
	 * @return the server load as number of service requests per second
	 * @throws RemoteException in case of an exception
	 */
	int serverLoad() throws RemoteException;

	/**
	 * @return true if there are connections available
	 * @throws RemoteException in case of an exception
	 */
	boolean connectionsAvailable() throws RemoteException;

	/**
	 * Locates {@link Server}s by name on a registry.
	 */
	interface Locator {

		/**
		 * Retrieves a Server from a registry running on the given host, using the
		 * given server name prefix as a condition. Returns the first server satisfying the condition.
		 * @param <C> the Remote connection type served by the server
		 * @param <A> the server admin type supplied by the server
		 * @return the servers having a name with the given prefix
		 * @throws RemoteException in case of a remote exception
		 * @throws NotBoundException in case no such server is found
		 */
		<C extends Remote, A extends ServerAdmin> Server<C, A> locateServer() throws RemoteException, NotBoundException;

		/**
		 * Returns a {@link Locator.Builder} instance.
		 * @return a {@link Locator.Builder} instance.
		 */
		static Locator.Builder builder() {
			return new DefaultServerLocator.DefaultBuilder();
		}

		/**
		 * Initializes a Registry if one is not running on the port defined by {@link ServerConfiguration#REGISTRY_PORT}
		 * @return the Registry
		 * @throws java.rmi.RemoteException in case of an exception
		 */
		static Registry registry() throws RemoteException {
			return registry(ServerConfiguration.REGISTRY_PORT.getOrThrow());
		}

		/**
		 * Initializes a Registry if one is not running
		 * @param registryPort the port on which to look for (or create) a registry
		 * @return the Registry
		 * @throws java.rmi.RemoteException in case of an exception
		 */
		static Registry registry(int registryPort) throws RemoteException {
			return DefaultServerLocator.initializeRegistry(registryPort);
		}

		/**
		 * A builder for {@link Locator} instances.
		 */
		interface Builder {

			/**
			 * @param hostName the name of the host
			 * @return this builder instance
			 */
			Builder hostName(String hostName);

			/**
			 * @param namePrefix the server name prefix to use when looking up, an empty string results in all servers being returned
			 * @return this builder instance
			 */
			Builder namePrefix(String namePrefix);

			/**
			 * @param registryPort the port on which to lookup/configure the registry
			 * @return this builder instance
			 */
			Builder registryPort(int registryPort);

			/**
			 * @param port the required server port, -1 for a server on any port
			 * @return this builder instance
			 */
			Builder port(int port);

			/**
			 * @return a new {@link Locator} instance based on this builder
			 */
			Locator build();
		}
	}
}
