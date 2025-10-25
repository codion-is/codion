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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.rmi;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a remote entity connection.
 * @see RemoteEntityConnectionProvider#builder()
 */
final class DefaultRemoteEntityConnectionProvider extends AbstractEntityConnectionProvider
				implements RemoteEntityConnectionProvider {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultRemoteEntityConnectionProvider.class);

	private Server<RemoteEntityConnection, ServerAdmin> server;
	private String serverName;
	private boolean truststoreResolved = false;

	private final String hostname;
	private final int port;
	private final int registryPort;
	private final String namePrefix;

	DefaultRemoteEntityConnectionProvider(DefaultRemoteEntityConnectionProviderBuilder builder) {
		super(builder);
		this.hostname = requireNonNull(builder.hostname, "hostname must be specified");
		this.port = builder.port;
		this.registryPort = builder.registryPort;
		this.namePrefix = builder.namePrefix;
	}

	@Override
	public String connectionType() {
		return CONNECTION_TYPE_REMOTE;
	}

	/**
	 * @return a string describing the server connection
	 */
	@Override
	public Optional<String> description() {
		return Optional.of(DESCRIPTION.optional().orElse(serverName + "@" + hostname));
	}

	/**
	 * @return the name of the host of the server providing the connection
	 */
	@Override
	public String hostname() {
		return hostname;
	}

	@Override
	protected EntityConnection connect() {
		if (!truststoreResolved) {
			Clients.resolveTrustStore();
			truststoreResolved = true;
		}
		try {
			LOG.debug("Initializing connection for {}", user());
			return (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(),
							new Class[] {EntityConnection.class}, new RemoteEntityConnectionHandler(
											server().connect(ConnectionRequest.builder()
															.user(user())
															.clientId(clientId())
															.clientType(clientType().orElseThrow(() ->
																			new IllegalStateException("client type must be specified")))
															.version(clientVersion().orElse(null))
															.parameter(REMOTE_CLIENT_DOMAIN_TYPE, domainType().name())
															.build())));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void close(EntityConnection connection) {
		try {
			server.disconnect(clientId());
		}
		catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return connects to and returns the Server instance
	 * @throws java.rmi.NotBoundException if no server is reachable or if the servers found are not using the specified port
	 * @throws java.rmi.RemoteException in case of remote exceptions
	 */
	private Server<RemoteEntityConnection, ServerAdmin> server() throws RemoteException, NotBoundException {
		boolean unreachable = false;
		try {
			if (server != null) {
				server.connectionsAvailable();
			}//just to check the connection
		}
		catch (RemoteException e) {
			LOG.info("{} was unreachable, {} - {} reconnecting...", serverName, user(), clientId());
			unreachable = true;
		}
		if (server == null || unreachable) {
			//if server is not reachable, try to reconnect once and return
			connectToServer();
			LOG.info("ClientID: {}, {} connected to server: {}", user(), clientId(), serverName);
		}

		return this.server;
	}

	private void connectToServer() throws RemoteException, NotBoundException {
		server = Server.Locator.builder()
						.hostname(hostname)
						.namePrefix(namePrefix)
						.registryPort(registryPort)
						.port(port)
						.build()
						.locateServer();
		serverName = server.information().name();
	}

	private static final class RemoteEntityConnectionHandler implements InvocationHandler {

		private static final String CONNECTED = "connected";
		private static final String ENTITIES = "entities";
		private static final String ITERATOR = "iterator";

		private final Map<Method, Method> methodCache = new HashMap<>();
		private final RemoteEntityConnection remoteConnection;

		private Entities entities;

		private RemoteEntityConnectionHandler(RemoteEntityConnection remoteConnection) {
			this.remoteConnection = remoteConnection;
		}

		@Override
		public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Exception {
			String methodName = method.getName();
			if (methodName.equals(CONNECTED)) {
				return connected();
			}
			if (methodName.equals(ENTITIES)) {
				return entities();
			}

			Method remoteMethod = methodCache.computeIfAbsent(method, RemoteEntityConnectionHandler::remoteMethod);
			try {
				Object result = remoteMethod.invoke(remoteConnection, args);
				if (methodName.equals(ITERATOR)) {
					return new RemoteEntityResultIteratorWrapper((RemoteEntityResultIterator) result);
				}

				return result;
			}
			catch (InvocationTargetException e) {
				LOG.error(e.getMessage(), e);
				throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
			}
			catch (Exception e) {
				LOG.error(e.getMessage(), e);
				throw e;
			}
		}

		private Object connected() throws RemoteException {
			try {
				return remoteConnection.connected();
			}
			catch (NoSuchObjectException | ConnectException e) {
				return false;
			}
		}

		private Entities entities() throws RemoteException {
			if (entities == null) {
				entities = remoteConnection.entities();
			}

			return entities;
		}

		private static Method remoteMethod(Method method) {
			try {
				return RemoteEntityConnection.class.getMethod(method.getName(), method.getParameterTypes());
			}
			catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final class RemoteEntityResultIteratorWrapper implements EntityResultIterator {

		private final RemoteEntityResultIterator iterator;

		private RemoteEntityResultIteratorWrapper(RemoteEntityResultIterator iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			try {
				return iterator.hasNext();
			}
			catch (RemoteException e) {
				LOG.error(e.getMessage(), e);
				throw new DatabaseException("Remote iterator call failed");
			}
		}

		@Override
		public Entity next() {
			try {
				return iterator.next();
			}
			catch (RemoteException e) {
				LOG.error(e.getMessage(), e);
				throw new DatabaseException("Remote iterator call failed");
			}
		}

		@Override
		public void close() {
			try {
				iterator.close();
			}
			catch (RemoteException e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}
}
