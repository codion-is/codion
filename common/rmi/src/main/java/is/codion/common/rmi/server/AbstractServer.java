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
package is.codion.common.rmi.server;

import is.codion.common.event.Event;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.user.User;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectInputFilter;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.*;

/**
 * A default Server implementation.
 * @param <T> the type of remote interface served by this server
 * @param <A> the type of the admin interface this server provides
 */
public abstract class AbstractServer<T extends Remote, A extends ServerAdmin> extends UnicastRemoteObject implements Server<T, A> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractServer.class);

	private final Map<UUID, ClientConnection<T>> connections = new ConcurrentHashMap<>();
	private final Map<String, Authenticator> authenticators = new HashMap<>();
	private final Collection<Authenticator> sharedAuthenticators = new ArrayList<>();
	private final Collection<AuxiliaryServer> auxiliaryServers = new ArrayList<>();
	private final TaskScheduler connectionMaintenanceScheduler;

	private final ServerConfiguration configuration;
	private final ServerInformation serverInformation;
	private final Event<?> shutdownEvent = Event.event();
	private volatile int connectionLimit = -1;
	private volatile boolean shuttingDown = false;

	private @Nullable Registry registry;
	private @Nullable A admin;

	/**
	 * Instantiates a new AbstractServer
	 * @param configuration the configuration
	 * @throws RemoteException in case of an exception
	 */
	protected AbstractServer(ServerConfiguration configuration) throws RemoteException {
		super(requireNonNull(configuration).port(),
						configuration.rmiClientSocketFactory().orElse(null), configuration.rmiServerSocketFactory().orElse(null));
		this.configuration = configuration;
		this.serverInformation = new DefaultServerInformation(UUID.randomUUID(),
						configuration.serverName(), configuration.port(), ZonedDateTime.now());
		this.connectionMaintenanceScheduler = TaskScheduler.builder()
						.task(new MaintenanceTask())
						.interval(configuration.connectionMaintenanceInterval(), TimeUnit.MILLISECONDS)
						.initialDelay(configuration.connectionMaintenanceInterval())
						.name("Connection maintenance")
						.start();
		setConnectionLimit(configuration.connectionLimit());
		getRuntime().addShutdownHook(new Thread(this::shutdown));
		try {
			configureObjectInputFilter(configuration);
			startAuxiliaryServers(configuration.auxiliaryServerFactory());
			loadAuthenticators();
		}
		catch (Throwable exception) {
			throw logShutdownAndReturn(new RuntimeException(exception));
		}
	}

	/**
	 * @return a map containing the current connections
	 */
	public final Map<RemoteClient, T> connections() {
		return connections.values().stream().collect(toMap(ClientConnection::remoteClient, ClientConnection::connection));
	}

	/**
	 * @param clientId the client id
	 * @return the connection associated with the given client
	 * @throws IllegalArgumentException in case no such client is connected
	 */
	public final T connection(UUID clientId) {
		ClientConnection<T> clientConnection = connections.get(requireNonNull(clientId));
		if (clientConnection != null) {
			return clientConnection.connection();
		}

		throw new IllegalArgumentException("Client not connected: " + clientId);
	}

	/**
	 * @return the current number of connections
	 */
	public final int connectionCount() {
		return connections.size();
	}

	/**
	 * @return the maximum number of concurrent connections accepted by this server,
	 * a negative number means no limit while 0 means the server is closed.
	 */
	public final int getConnectionLimit() {
		return connectionLimit;
	}

	/**
	 * @param connectionLimit the maximum number of concurrent connections accepted by this server,
	 * a negative number means no limit while 0 means the server is closed.
	 */
	public final void setConnectionLimit(int connectionLimit) {
		this.connectionLimit = connectionLimit;
	}

	/**
	 * @return the maintenance check interval in ms
	 */
	public final int getMaintenanceInterval() {
		return connectionMaintenanceScheduler.interval().getOrThrow();
	}

	/**
	 * @param maintenanceInterval the new maintenance interval in ms
	 */
	public final void setMaintenanceInterval(int maintenanceInterval) {
		connectionMaintenanceScheduler.interval().set(maintenanceInterval);
	}

	@Override
	public final ServerInformation information() {
		return serverInformation;
	}

	@Override
	public final boolean connectionsAvailable() {
		return !maximumNumberOfConnectionsReached();
	}

	@Override
	public final T connect(ConnectionRequest connectionRequest) throws RemoteException, ConnectionNotAvailableException, LoginException {
		if (shuttingDown) {
			throw new LoginException("Server is shutting down");
		}
		requireNonNull(connectionRequest);
		requireNonNull(connectionRequest.user(), "user must be specified");
		requireNonNull(connectionRequest.clientId(), "clientId must be specified");
		requireNonNull(connectionRequest.clientType(), "clientType must be specified");
		synchronized (connections) {
			ClientConnection<T> clientConnection = connections.get(connectionRequest.clientId());
			if (clientConnection != null) {
				validateUserCredentials(connectionRequest.user(), clientConnection.remoteClient().user());
				LOG.trace("Active connection exists {}", connectionRequest);

				return clientConnection.connection();
			}

			if (maximumNumberOfConnectionsReached()) {
				LOG.warn("Maximum number of connections reached {}", connectionLimit);
				throw new ConnectionNotAvailableException();
			}
			LOG.trace("No active connection found for client {}, establishing a new connection", connectionRequest);

			return createConnection(connectionRequest).connection();
		}
	}

	@Override
	public final void disconnect(UUID clientId) {
		ClientConnection<T> clientConnection = connections.remove(requireNonNull(clientId));
		if (clientConnection != null) {
			try {
				disconnect(clientConnection.connection());
			}
			catch (Exception e) {
				LOG.debug("Error while disconnecting a client: {}", clientId, e);
			}
			RemoteClient remoteClient = clientConnection.remoteClient();
			for (Authenticator authenticator : sharedAuthenticators) {
				authenticator.logout(remoteClient);
			}
			Authenticator authenticator = authenticators.get(remoteClient.clientType());
			if (authenticator != null) {
				authenticator.logout(remoteClient);
			}
			LOG.debug("Client disconnected {}", remoteClient);
		}
	}

	/**
	 * Shuts down this server.
	 */
	public final void shutdown() {
		if (shuttingDown) {
			return;
		}
		shuttingDown = true;
		connectionMaintenanceScheduler.stop();
		unexportSilently(asList(registry, this, admin));
		new ArrayList<>(connections.keySet()).forEach(this::disconnect);
		sharedAuthenticators.forEach(AbstractServer::closeAuthenticator);
		authenticators.values().forEach(AbstractServer::closeAuthenticator);
		auxiliaryServers.forEach(AbstractServer::stopAuxiliaryServer);
		SerializationFilter.handleDryRun();
		shutdownEvent.run();
	}

	/**
	 * Adds a {@link Authenticator} instance to this server.
	 * If {@link Authenticator#clientType()} is empty, the authenticator
	 * is shared between all client types, otherwise it is only used for clients of the given type.
	 * @param authenticator the authenticator to add
	 * @throws IllegalStateException in case an authenticator with the same clientType has been added
	 */
	public final void addAuthenticator(Authenticator authenticator) {
		requireNonNull(authenticator);
		if (authenticator.clientType().isPresent()) {
			String clientType = authenticator.clientType().get();
			if (authenticators.containsKey(clientType)) {
				throw new IllegalStateException("Authenticator for clientType '" + clientType + "' has alread been added");
			}
			authenticators.put(clientType, authenticator);
		}
		else {
			sharedAuthenticators.add(authenticator);
		}
	}

	/**
	 * @return info on all connected users
	 */
	final Collection<User> users() {
		return connections().keySet().stream()
						.map(ConnectionRequest::user)
						.map(User::copy)
						.map(User::clearPassword)
						.collect(toSet());
	}

	/**
	 * @return info on all connected clients
	 */
	final Collection<RemoteClient> clients() {
		return connections().keySet().stream()
						.map(RemoteClient::copy)
						.map(AbstractServer::clearPasswords)
						.collect(toList());
	}

	/**
	 * Sets the admin instance for this server
	 * @param admin the admin instance
	 * @throws IllegalStateException in case an admin instance has already been set
	 */
	protected final void setAdmin(A admin) {
		requireNonNull(admin);
		if (this.admin != null) {
			throw new IllegalStateException("Admin has already been set for this server");
		}
		this.admin = admin;
	}

	/**
	 * @return the admin instance associated with this server
	 * @throws IllegalStateException in case no admin instance has been set
	 * @see #setAdmin(ServerAdmin)
	 */
	protected final A getAdmin() {
		if (admin == null) {
			throw new IllegalStateException("No admin instance available");
		}

		return admin;
	}

	/**
	 * @param listener a listener notified when this server is shutting down.
	 */
	protected final void addShutdownListener(Runnable listener) {
		shutdownEvent.addListener(requireNonNull(listener));
	}

	/**
	 * Establishes the actual client connection.
	 * @param remoteClient the remote client
	 * @return a connection for the given client
	 * @throws RemoteException in case of an exception
	 * @throws LoginException in case of an error during the login
	 */
	protected abstract T connect(RemoteClient remoteClient) throws RemoteException, LoginException;

	/**
	 * Disconnects the given connection.
	 * @param connection the connection to disconnect
	 * @throws RemoteException in case of an exception
	 */
	protected abstract void disconnect(T connection) throws RemoteException;

	/**
	 * Maintains the given connections, that is, disconnects inactive or invalid connections, if required.
	 * @param connections all current connections
	 * @throws RemoteException in case of an exception
	 */
	protected abstract void maintainConnections(Collection<ClientConnection<T>> connections) throws RemoteException;

	/**
	 * @return the Registry instance
	 * @throws RemoteException in case of an exception
	 */
	protected final Registry registry() throws RemoteException {
		if (registry == null) {
			registry = LocateRegistry.createRegistry(configuration.registryPort());
		}

		return registry;
	}

	/**
	 * Logs the given exception and shuts down this server
	 * @param exception the exception
	 * @param <T> the exception type
	 * @return the exception
	 */
	protected final <T extends Throwable> T logShutdownAndReturn(T exception) {
		LOG.error("Exception on server startup", exception);
		shutdown();

		return exception;
	}

	/**
	 * @return an unmodifiable view of the auxialiary servers running along-side this server
	 */
	protected final Collection<AuxiliaryServer> auxiliaryServers() {
		return unmodifiableCollection(auxiliaryServers);
	}

	/**
	 * Validates the given user credentials
	 * @param userToCheck the credentials to check
	 * @param requiredUser the required credentials
	 * @throws ServerAuthenticationException in case either User instance is null or if the username or password do not match
	 */
	protected static void validateUserCredentials(User userToCheck, User requiredUser) throws ServerAuthenticationException {
		if (userToCheck == null || requiredUser == null
						|| !userToCheck.username().equalsIgnoreCase(requiredUser.username())
						|| !Arrays.equals(userToCheck.password(), requiredUser.password())) {
			throw new ServerAuthenticationException("Wrong username or password");
		}
	}

	private boolean maximumNumberOfConnectionsReached() {
		return connectionLimit > -1 && connectionCount() >= connectionLimit;
	}

	private ClientConnection<T> createConnection(ConnectionRequest connectionRequest) throws LoginException, RemoteException {
		RemoteClient remoteClient = RemoteClient.builder(connectionRequest)
						.clientHost(clientHost((String) connectionRequest.parameters().get(CLIENT_HOST)))
						.build();
		for (Authenticator authenticator : sharedAuthenticators) {
			remoteClient = authenticator.login(remoteClient);
		}
		Authenticator clientAuthenticator = authenticators.get(connectionRequest.clientType());
		LOG.debug("Connecting client {}, authenticator {}", connectionRequest, clientAuthenticator);
		if (clientAuthenticator != null) {
			remoteClient = clientAuthenticator.login(remoteClient);
		}
		ClientConnection<T> clientConnection = new ClientConnection<>(remoteClient, connect(remoteClient));
		connections.put(remoteClient.clientId(), clientConnection);

		return clientConnection;
	}

	private void startAuxiliaryServers(Collection<String> auxiliaryServerFactories) {
		try {
			for (String auxiliaryServerFactory : auxiliaryServerFactories) {
				AuxiliaryServerFactory<T, A, ?> auxiliaryServerFactoryInstance = AuxiliaryServerFactory.instance(auxiliaryServerFactory);
				AuxiliaryServer auxiliaryServer = auxiliaryServerFactoryInstance.createServer(this);
				auxiliaryServers.add(auxiliaryServer);
				Callable<?> starter = () -> {
					startAuxiliaryServer(auxiliaryServer);
					return null;
				};
				newSingleThreadExecutor(new DaemonThreadFactory(auxiliaryServer.getClass().getSimpleName())).submit(starter).get();
			}
		}
		catch (InterruptedException e) {
			LOG.error("Interrupted during auxiliary server startup", e);
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			LOG.error("Starting auxiliary server", e);
			throw new RuntimeException(e);
		}
	}

	private static RemoteClient clearPasswords(RemoteClient remoteClient) {
		remoteClient.user().clearPassword();
		remoteClient.databaseUser().clearPassword();

		return remoteClient;
	}

	private static void configureObjectInputFilter(ServerConfiguration configuration) {
		if (configuration.objectInputFilterFactory().isPresent()) {
			String objectInputFilterFactory = configuration.objectInputFilterFactory().get();
			ObjectInputFilterFactory inputFilterFactory = ObjectInputFilterFactory.instance(objectInputFilterFactory);
			ObjectInputFilter objectInputFilter = inputFilterFactory.createObjectInputFilter();
			ObjectInputFilter.Config.setSerialFilter(objectInputFilter);
			LOG.info("ObjectInputFilter {} enabled", objectInputFilter.getClass().getName());
		}
		else if (configuration.objectInputFilterFactoryRequired()) {
			throw new IllegalStateException("An ObjectInputFilterFactory is required, but none is specified. " +
							"Either configure codion.server.objectInputFilterFactory or set " +
							"codion.server.objectInputFilterFactoryRequired=false to disable this check");
		}
		else {
			LOG.warn("No ObjectInputFilterFactoryClassName specified");
		}
	}

	private static void closeAuthenticator(Authenticator authenticator) {
		try {
			authenticator.close();
		}
		catch (Exception e) {
			LOG.error("Exception while closing authenticator {} for client type: {}",
							authenticator.getClass(), authenticator.clientType().orElse(null), e);
		}
	}

	private static void startAuxiliaryServer(AuxiliaryServer server) throws Exception {
		try {
			server.start();
			LOG.info("Auxiliary server started: {}", server);
		}
		catch (Exception e) {
			LOG.error("Starting auxiliary server", e);
			throw e;
		}
	}

	private static void stopAuxiliaryServer(AuxiliaryServer server) {
		try {
			server.stop();
			LOG.info("Auxiliary server stopped: {}", server);
		}
		catch (Exception e) {
			LOG.error("Stopping auxiliary server", e);
		}
	}

	private static void unexportSilently(List<Remote> remotes) {
		for (Remote remote : remotes) {
			if (remote != null) {
				try {
					unexportObject(remote, true);
				}
				catch (NoSuchObjectException e) {
					LOG.error("Exception while unexporting {} on shutdown", remote, e);
				}
			}
		}
	}

	private static String clientHost(@Nullable String requestParameterHost) {
		if (requestParameterHost == null) {
			try {
				return getClientHost();
			}
			catch (ServerNotActiveException ignored) {
				return RemoteClient.UNKNOWN_CLIENT_HOST;
			}
		}

		return requestParameterHost;
	}

	private void loadAuthenticators() {
		Authenticator.authenticators().forEach(authenticator -> {
			String clientType = authenticator.clientType().orElse(null);
			LOG.info("Server loading authenticator '{}' as service, {}", authenticator.getClass()
							.getName(), clientType == null ? "shared" : "(clientType: '" + clientType + "'");
			addAuthenticator(authenticator);
		});
	}

	/**
	 * Represents a remote client connection.
	 * @param <T> the connection type
	 */
	protected static final class ClientConnection<T> {

		private final RemoteClient client;
		private final T connection;

		private ClientConnection(RemoteClient client, T connection) {
			this.client = client;
			this.connection = connection;
		}

		/**
		 * @return the remote client
		 */
		public RemoteClient remoteClient() {
			return client;
		}

		/**
		 * @return the connection
		 */
		public T connection() {
			return connection;
		}
	}

	private final class MaintenanceTask implements Runnable {

		@Override
		public void run() {
			try {
				if (connectionCount() > 0) {
					maintainConnections(new ArrayList<>(connections.values()));
				}
			}
			catch (Exception e) {
				LOG.error("Exception while maintaining connections", e);
			}
		}
	}

	private static final class DaemonThreadFactory implements ThreadFactory {

		private final String name;

		private DaemonThreadFactory(String name) {
			this.name = name;
		}

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			thread.setName(name);

			return thread;
		}
	}
}
