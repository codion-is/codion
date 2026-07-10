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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.rmi;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.QueryCache;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;
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
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static java.lang.reflect.InvocationHandler.invokeDefault;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableList;

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

	/**
	 * @return a string describing the server connection
	 */
	@Override
	public Optional<String> description() {
		return Optional.of(DESCRIPTION.optional().orElse(serverName + "@" + hostname));
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
															.clientType(clientType())
															.version(clientVersion().orElse(null))
															.parameter(REMOTE_CLIENT_DOMAIN_TYPE, domainType().name())
															.build())));
		}
		catch (Exception e) {
			throw Exceptions.runtime(e);
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
		if (server == null) {
			connectToServer();
			LOG.info("ClientID: {}, {} connected to server: {}", user(), clientId(), serverName);
		}
		else if (unreachable) {
			//if server is not reachable, try to reconnect once and return
			reconnectToServer();
			LOG.info("ClientID: {}, {} connected to server: {}", user(), clientId(), serverName);
		}

		return this.server;
	}

	/**
	 * A server holding this client's live session accepts it back even when full, since existing
	 * connections are exempt from the connection limit. Server discovery filters out servers with no
	 * connections available, hiding exactly that server, so look it up by name and let connect() decide.
	 */
	private void reconnectToServer() throws RemoteException, NotBoundException {
		if (serverName != null) {
			try {
				Server<RemoteEntityConnection, ServerAdmin> namedServer = (Server<RemoteEntityConnection, ServerAdmin>)
								LocateRegistry.getRegistry(hostname, registryPort).lookup(serverName);
				namedServer.information();//just to check the connection
				server = namedServer;

				return;
			}
			catch (Exception e) {
				LOG.info("Unable to reconnect to {} by name, searching for a server", serverName, e);
			}
		}
		connectToServer();
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
		private static final String CACHE_QUERIES = "cacheQueries";
		private static final String SELECT = "select";
		private static final String SELECT_SINGLE = "selectSingle";

		private final Map<Method, Method> methodCache = new HashMap<>();
		private final RemoteEntityConnection remoteConnection;

		private Entities entities;
		private @Nullable ProxyQueryCache queryCache;

		private RemoteEntityConnectionHandler(RemoteEntityConnection remoteConnection) {
			this.remoteConnection = remoteConnection;
		}

		@Override
		public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (methodName.equals(CONNECTED)) {
				return connected();
			}
			if (methodName.equals(ENTITIES)) {
				return entities();
			}
			if (methodName.equals(CACHE_QUERIES)) {
				return cacheQueries();
			}
			if (method.isDefault()) {
				return invokeDefault(proxy, method, args);
			}
			Select cacheKey = cacheKey(methodName, args);
			boolean singleResult = cacheKey != null && singleResult(methodName, args);
			Object cached = cachedResult(cacheKey, singleResult);
			if (cached != null) {
				//served without a round-trip to the server
				return cached;
			}

			Object result = invokeRemote(method, args);
			if (methodName.equals(ITERATOR)) {
				return new RemoteEntityResultIteratorWrapper((RemoteEntityResultIterator) result);
			}
			if (cacheKey != null) {
				return cacheResult(cacheKey, singleResult, result);
			}

			return result;
		}

		/**
		 * @return the cached result for the given invocation, or null if it must be forwarded to the server,
		 * there being no query cache, no cached result, or a single result requested but not exactly one cached
		 */
		private @Nullable Object cachedResult(@Nullable Select cacheKey, boolean singleResult) {
			if (cacheKey == null) {
				return null;
			}
			List<Entity> cached = queryCache.cached.get(cacheKey);
			if (cached == null) {
				return null;
			}
			if (!singleResult) {
				return cached;
			}
			if (cached.size() == 1) {
				return cached.get(0);
			}
			//zero or multiple rows cached, forward and let the server throw
			//EntityNotFoundException/MultipleEntitiesFoundException with its own messages
			return null;
		}

		private Object cacheResult(Select cacheKey, boolean singleResult, Object result) {
			//the cached result is shared by every hit, immutable entities in an unmodifiable
			//list keep a single caller from modifying what the next one receives
			if (singleResult) {
				Entity entity = ((Entity) result).immutable();
				queryCache.cached.put(cacheKey, singletonList(entity));

				return entity;
			}
			List<Entity> cached = immutable((List<Entity>) result);
			queryCache.cached.put(cacheKey, cached);

			return cached;
		}

		private Object invokeRemote(Method method, Object[] args) throws Throwable {
			Method remoteMethod = methodCache.computeIfAbsent(method, RemoteEntityConnectionHandler::remoteMethod);
			try {
				return remoteMethod.invoke(remoteConnection, args);
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

		private QueryCache cacheQueries() {
			if (queryCache != null) {
				throw new IllegalStateException("A query cache is already active on this connection");
			}

			return queryCache = new ProxyQueryCache();
		}

		/**
		 * @return the cache key for the given invocation or null if it is not a cacheable select
		 * or no query cache is active
		 */
		private @Nullable Select cacheKey(String methodName, Object @Nullable [] args) {
			if (queryCache == null || args == null || args.length != 1
							|| (!methodName.equals(SELECT) && !methodName.equals(SELECT_SINGLE))) {
				return null;
			}
			Select select = null;
			if (args[0] instanceof Select) {
				select = (Select) args[0];
			}
			else if (args[0] instanceof Condition) {
				select = where((Condition) args[0]).build();
			}
			else if (args[0] instanceof Entity.Key) {
				//select(Key) is selectSingle(key(key)) in the local and http tiers, share their cache key
				select = where(key((Entity.Key) args[0])).build();
			}

			return select == null || select.forUpdate() ? null : select;
		}

		/**
		 * @return true if the given invocation returns a single Entity rather than a List
		 */
		private static boolean singleResult(String methodName, Object[] args) {
			return methodName.equals(SELECT_SINGLE) || args[0] instanceof Entity.Key;
		}

		private static List<Entity> immutable(List<Entity> entities) {
			return entities.stream()
							.map(Entity::immutable)
							.collect(toUnmodifiableList());
		}

		private final class ProxyQueryCache implements QueryCache {

			private final Map<Select, List<Entity>> cached = new HashMap<>();

			@Override
			public void close() {
				synchronized (RemoteEntityConnectionHandler.this) {
					cached.clear();
					if (queryCache == this) {
						queryCache = null;
					}
				}
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
				throw iteratorCallFailed(e);
			}
		}

		@Override
		public Entity next() {
			try {
				return iterator.next();
			}
			catch (RemoteException e) {
				throw iteratorCallFailed(e);
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

		private static DatabaseException iteratorCallFailed(RemoteException exception) {
			LOG.error(exception.getMessage(), exception);
			DatabaseException databaseException = new DatabaseException("Remote iterator call failed");
			//DatabaseException only chains a SQLException cause, without this the client side trace dead-ends here
			databaseException.initCause(exception);

			return databaseException;
		}
	}
}
