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
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.report.Report;
import is.codion.common.logging.MethodTrace;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.AbstractServer;
import is.codion.common.rmi.server.AuxiliaryServer;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.server.EntityServerAdmin.DomainEntityDefinition;
import is.codion.framework.server.EntityServerAdmin.DomainOperation;
import is.codion.framework.server.EntityServerAdmin.DomainReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static is.codion.common.Text.nullOrEmpty;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A remote server class, responsible for handling requests for AbstractRemoteEntityConnections.
 */
public class EntityServer extends AbstractServer<AbstractRemoteEntityConnection, EntityServerAdmin> {

	@Serial
	private static final long serialVersionUID = 1;

	private static final Logger LOG = LoggerFactory.getLogger(EntityServer.class);

	private static final String SHUTDOWN = "shutdown";

	private final EntityServerConfiguration configuration;
	private final Map<DomainType, Domain> domainModels;
	private final Database database;
	private final boolean methodTracing;
	private final Map<String, Integer> clientTypeIdleConnectionTimeouts = new HashMap<>();

	private int idleConnectionTimeout;

	/**
	 * Constructs a new EntityServer and binds it to a registry on the port found in the configuration.
	 * @param configuration the server configuration
	 * @throws RemoteException in case of a remote exception
	 * @throws RuntimeException in case the domain model classes are not found on the classpath or if the
	 * jdbc driver class is not found or in case of an exception while constructing the initial pooled connections
	 */
	public EntityServer(EntityServerConfiguration configuration) throws RemoteException {
		super(configuration);
		addShutdownListener(new ShutdownListener());
		this.configuration = configuration;
		try {
			this.database = requireNonNull(configuration.database());
			this.methodTracing = configuration.methodTracing();
			this.domainModels = loadDomainModels(configuration.domainClassNames());
			configureDatabase(domainModels.values(), database);
			EntityServerAdmin serverAdmin = createServerAdmin(configuration);
			if (serverAdmin != null) {
				setAdmin(serverAdmin);
			}
			setIdleConnectionTimeout(configuration.idleConnectionTimeout());
			setClientTypeIdleConnectionTimeouts(configuration.clientTypeIdleConnectionTimeouts());
			createConnectionPools(configuration.database(), configuration.connectionPoolFactory(), configuration.connectionPoolUsers());
			registry().rebind(information().name(), this);
		}
		catch (Throwable t) {
			throw logShutdownAndReturn(new RuntimeException(t));
		}
	}

	/**
	 * @param user the server admin user
	 * @return the administration interface for this server
	 * @throws ServerAuthenticationException in case authentication fails
	 * @throws IllegalStateException in case no server admin instance is available
	 */
	@Override
	public final EntityServerAdmin admin(User user) throws ServerAuthenticationException {
		validateUserCredentials(user, configuration.adminUser());

		return getAdmin();
	}

	@Override
	protected final AbstractRemoteEntityConnection connect(RemoteClient remoteClient) throws RemoteException, LoginException {
		requireNonNull(remoteClient);
		try {
			AbstractRemoteEntityConnection connection = createRemoteConnection(database(), remoteClient,
							configuration.port(), configuration.rmiClientSocketFactory().orElse(null),
							configuration.rmiServerSocketFactory().orElse(null));
			connection.setTracingEnabled(methodTracing);

			connection.closedObserver().addConsumer(this::removeConnection);
			LOG.debug("{} connected", remoteClient);

			return connection;
		}
		catch (AuthenticationException e) {
			throw new ServerAuthenticationException(e.getMessage());
		}
		catch (RemoteException e) {
			throw e;
		}
		catch (Exception e) {
			LOG.debug("{} unable to connect", remoteClient, e);
			throw new LoginException(e.getMessage());
		}
	}

	@Override
	protected final void disconnect(AbstractRemoteEntityConnection connection) throws RemoteException {
		connection.close();
	}

	/**
	 * Creates the remote connection provided by this server
	 * @param database the underlying database
	 * @param remoteClient the client requesting the connection
	 * @param port the port to use when exporting this remote connection
	 * @param clientSocketFactory the client socket factory, null for default
	 * @param serverSocketFactory the server socket factory, null for default
	 * @return a remote connection
	 * @throws RemoteException in case of an exception
	 * @throws DatabaseException in case a database connection can not be established, for example
	 * if a wrong username or password is provided
	 */
	protected AbstractRemoteEntityConnection createRemoteConnection(Database database,
																																	RemoteClient remoteClient, int port,
																																	RMIClientSocketFactory clientSocketFactory,
																																	RMIServerSocketFactory serverSocketFactory)
					throws RemoteException {
		return new DefaultRemoteEntityConnection(clientDomainModel(remoteClient), database, remoteClient, port,
						clientSocketFactory, serverSocketFactory);
	}

	/**
	 * @return the underlying Database implementation class
	 */
	final Database database() {
		return database;
	}

	/**
	 * @return the idle connection timeout in milliseconds
	 */
	final int getIdleConnectionTimeout() {
		return idleConnectionTimeout;
	}

	/**
	 * @param idleConnectionTimeout the new idle connection timeout value in milliseconds
	 * @throws IllegalArgumentException in case timeout is less than zero
	 */
	final void setIdleConnectionTimeout(int idleConnectionTimeout) {
		if (idleConnectionTimeout < 0) {
			throw new IllegalArgumentException("Idle connection timeout must be a positive integer");
		}
		this.idleConnectionTimeout = idleConnectionTimeout;
	}

	/**
	 * @param clientTypeIdleConnectionTimeouts the idle connection timeout values mapped to each clientTypeId
	 */
	final void setClientTypeIdleConnectionTimeouts(Map<String, Integer> clientTypeIdleConnectionTimeouts) {
		this.clientTypeIdleConnectionTimeouts.putAll(clientTypeIdleConnectionTimeouts);
	}

	/**
	 * Returns the statistics gathered via {@link Database#countQuery(String)}.
	 * @return a {@link Database.Statistics} object containing query statistics collected since
	 * the last time this function was called.
	 */
	final Database.Statistics databaseStatistics() {
		return database.statistics();
	}

	@Override
	protected final void maintainConnections(Collection<ClientConnection<AbstractRemoteEntityConnection>> connections) throws RemoteException {
		for (ClientConnection<AbstractRemoteEntityConnection> client : connections) {
			AbstractRemoteEntityConnection connection = client.connection();
			if (!connection.active()) {
				boolean connected = connection.connected();
				boolean timedOut = hasConnectionTimedOut(connection);
				if (!connected || timedOut) {
					LOG.debug("Removing connection {}, connected: {}, timeout: {}", client, connected, timedOut);
					disconnect(client.remoteClient().clientId());
				}
			}
		}
	}

	final Map<DomainType, Collection<DomainEntityDefinition>> domainEntityDefinitions() {
		Map<DomainType, Collection<DomainEntityDefinition>> domainEntities = new HashMap<>();
		for (Domain domain : domainModels.values()) {
			domainEntities.put(domain.type(), domain.entities().definitions().stream()
							.map(definition -> new DefaultDomainEntityDefinition(domain.type().name(),
											definition.type().name(), definition.table()))
							.collect(Collectors.toList()));
		}

		return domainEntities;
	}

	final Map<DomainType, Collection<DomainReport>> domainReports() {
		Map<DomainType, Collection<DomainReport>> domainReports = new HashMap<>();
		for (Domain domain : domainModels.values()) {
			domainReports.put(domain.type(), domain.reports().entrySet().stream()
							.map(entry -> new DefaultDomainReport(domain.type().name(), entry.getKey().name(),
											entry.getValue().getClass().getSimpleName(), entry.getValue().toString(), entry.getValue().cached()))
							.collect(toList()));
		}

		return domainReports;
	}

	final Map<DomainType, Collection<DomainOperation>> domainOperations() {
		Map<DomainType, Collection<DomainOperation>> domainOperations = new HashMap<>();
		for (Domain domain : domainModels.values()) {
			Collection<DomainOperation> operations = new ArrayList<>();
			operations.addAll(domain.procedures().entrySet().stream()
							.map(entry -> new DefaultDomainOperation(domain.type().name(), "Procedure", entry.getKey().name(),
											entry.getValue().getClass().getName()))
							.collect(toList()));
			operations.addAll(domain.functions().entrySet().stream()
							.map(entry -> new DefaultDomainOperation(domain.type().name(), "Function", entry.getKey().name(),
											entry.getValue().getClass().getName()))
							.collect(toList()));
			domainOperations.put(domain.type(), operations);
		}

		return domainOperations;
	}

	/**
	 * Clears all cached reports, triggering a reload on next usage.
	 */
	final void clearReportCache() {
		for (Domain domain : domainModels.values()) {
			domain.reports().values().forEach(Report::clearCache);
		}
	}

	/**
	 * Returns the method traces for the connection identified by the given key.
	 * @param clientId the UUID identifying the client
	 * @return the method traces for the given connection
	 */
	final List<MethodTrace> methodTraces(UUID clientId) {
		return connection(clientId).methodTraces();
	}

	/**
	 * @param clientId the client id
	 * @return true if method tracing is enabled for the given client
	 */
	final boolean isTracingEnabled(UUID clientId) {
		return connection(clientId).isTracingEnabled();
	}

	/**
	 * @param clientId the client id
	 * @param tracingEnabled the new tracing status
	 */
	final void setTracingEnabled(UUID clientId, boolean tracingEnabled) {
		connection(clientId).setTracingEnabled(tracingEnabled);
	}

	/**
	 * @param timedOutOnly if true only connections that have timed out are culled
	 * @see #hasConnectionTimedOut(AbstractRemoteEntityConnection)
	 */
	final void disconnectClients(boolean timedOutOnly) {
		List<RemoteClient> clients = new ArrayList<>(connections().keySet());
		for (RemoteClient client : clients) {
			AbstractRemoteEntityConnection connection = connection(client.clientId());
			if (timedOutOnly) {
				boolean active = connection.active();
				if (!active && hasConnectionTimedOut(connection)) {
					disconnect(client.clientId());
				}
			}
			else {
				disconnect(client.clientId());
			}
		}
	}

	private void removeConnection(AbstractRemoteEntityConnection connection) {
		disconnect(connection.remoteClient().clientId());
	}

	/**
	 * Creates a {@link EntityServerAdmin} instance if the server admin port is specified.
	 * @param configuration the server configuration
	 * @return a admin instance
	 * @throws RemoteException in case of an exception
	 */
	private EntityServerAdmin createServerAdmin(EntityServerConfiguration configuration) throws RemoteException {
		if (configuration.adminPort() != 0) {
			return new DefaultEntityServerAdmin(this, configuration);
		}

		return null;
	}

	private boolean hasConnectionTimedOut(AbstractRemoteEntityConnection connection) {
		Integer timeout = clientTypeIdleConnectionTimeouts.get(connection.remoteClient().clientType());
		if (timeout == null) {
			timeout = idleConnectionTimeout;
		}

		return connection.hasBeenInactive(timeout);
	}

	private Domain clientDomainModel(RemoteClient remoteClient) {
		String domainTypeName = (String) remoteClient.parameters().get(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE);
		if (domainTypeName == null) {
			throw new IllegalArgumentException("'" + RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE + "' parameter not specified");
		}

		return domainModels.get(DomainType.domainTypeByName(domainTypeName));
	}

	private static void configureDatabase(Collection<Domain> domainModels, Database database) {
		for (Domain domain : domainModels) {
			LocalEntityConnection.configureDatabase(database, domain);
		}
	}

	private static Map<DomainType, Domain> loadDomainModels(Collection<String> domainModelClassNames) throws Throwable {
		Map<DomainType, Domain> domains = new HashMap<>();
		try {
			for (Domain domain : Domain.domains()) {
				LOG.info("Server loading domain model '{}' as a service", domain.type());
				domains.put(domain.type(), domain);
			}
			for (String className : domainModelClassNames) {
				LOG.info("Server loading domain model '{}' from classpath", className);
				Domain domain = (Domain) Class.forName(className).getDeclaredConstructor().newInstance();
				domains.put(domain.type(), domain);
			}

			return unmodifiableMap(domains);
		}
		catch (Exception e) {
			LOG.error("Exception while loading and registering domain model", e);
			throw e;
		}
	}

	private static void createConnectionPools(Database database, String connectionPoolFactoryClassName,
																						Collection<User> connectionPoolUsers) {
		if (!connectionPoolUsers.isEmpty()) {
			ConnectionPoolFactory poolFactory;
			if (nullOrEmpty(connectionPoolFactoryClassName)) {
				poolFactory = ConnectionPoolFactory.instance();
			}
			else {
				poolFactory = ConnectionPoolFactory.instance(connectionPoolFactoryClassName);
			}
			for (User user : connectionPoolUsers) {
				database.createConnectionPool(poolFactory, user);
			}
		}
	}

	/**
	 * Starts the server, using the configuration from system properties.
	 * @return the server instance
	 * @throws RemoteException in case of an exception
	 */
	public static EntityServer startServer() throws RemoteException {
		return startServer(EntityServerConfiguration.builderFromSystemProperties().build());
	}

	/**
	 * Starts the server.
	 * @param configuration the configuration
	 * @return the server instance
	 * @throws RemoteException in case of an exception
	 */
	public static synchronized EntityServer startServer(EntityServerConfiguration configuration) throws RemoteException {
		requireNonNull(configuration);
		long currentTime = System.currentTimeMillis();
		try {
			EntityServer server = new EntityServer(configuration);
			printStartupInfo(server, System.currentTimeMillis() - currentTime);

			return server;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			LOG.error("Exception when starting server", e);
			throw new RuntimeException(e);
		}
	}

	private static void printStartupInfo(EntityServer server, long startTime) {
		String startupInfo = server.information().name()
						+ " started on port: " + server.information().port()
						+ ", registryPort: " + server.configuration.registryPort()
						+ ", adminPort: " + server.configuration.adminPort()
						+ ", hostname: " + ServerConfiguration.RMI_SERVER_HOSTNAME.get()
						+ auxiliaryServerInfo(server.auxiliaryServers())
						+ "Server started in " + startTime + " ms";
		LOG.info(startupInfo);
		System.out.println(startupInfo);
	}

	private static String auxiliaryServerInfo(Collection<AuxiliaryServer> auxiliaryServers) {
		return auxiliaryServers.stream()
						.map(AuxiliaryServer::information)
						.collect(Collectors.joining("\n", !auxiliaryServers.isEmpty() ? "\n" : "", "\n"));
	}

	/**
	 * Connects to the server and shuts it down
	 */
	static synchronized void shutdownServer() throws ServerAuthenticationException {
		Clients.resolveTrustStore();
		EntityServerConfiguration configuration = EntityServerConfiguration.builderFromSystemProperties().build();
		String serverName = configuration.serverName();
		int registryPort = configuration.registryPort();
		User adminUser = configuration.adminUser();
		if (adminUser == null) {
			throw new ServerAuthenticationException("No admin user specified");
		}
		try {
			Registry registry = LocateRegistry.getRegistry(registryPort);
			Server<?, EntityServerAdmin> server = (Server<?, EntityServerAdmin>) registry.lookup(serverName);
			EntityServerAdmin serverAdmin = server.admin(adminUser);
			String shutDownInfo = serverName + " found in registry on port: " + registryPort + ", shutting down";
			LOG.info(shutDownInfo);
			System.out.println(shutDownInfo);
			serverAdmin.shutdown();
		}
		catch (RemoteException e) {
			System.out.println("Unable to shutdown server: " + e.getMessage());
			LOG.error("Error on shutdown", e);
		}
		catch (NotBoundException e) {
			System.out.println(serverName + " not bound to registry on port: " + registryPort);
		}
		catch (ServerAuthenticationException e) {
			LOG.error("Admin user info not provided or incorrect", e);
			throw e;
		}
	}

	/**
	 * If no arguments are supplied a new EntityServer is started.
	 * @param arguments 'shutdown' (case-insensitive) causes a running server to be shut down, otherwise a server is started
	 * @throws RemoteException in case of a remote exception during service export
	 * @throws ServerAuthenticationException in case of missing or incorrect admin user information
	 */
	public static void main(String[] arguments) throws RemoteException, ServerAuthenticationException {
		if (arguments.length > 0 && arguments[0].equalsIgnoreCase(SHUTDOWN)) {
			shutdownServer();
		}
		else {
			startServer();
		}
	}

	private final class ShutdownListener implements Runnable {

		@Override
		public void run() {
			database.closeConnectionPools();
		}
	}

	private static final class DefaultDomainEntityDefinition implements DomainEntityDefinition, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final String domain;
		private final String name;
		private final String table;

		private DefaultDomainEntityDefinition(String domain, String name, String table) {
			this.domain = domain;
			this.name = name;
			this.table = table;
		}

		@Override
		public String domain() {
			return domain;
		}

		@Override
		public String entity() {
			return name;
		}

		@Override
		public String table() {
			return table;
		}
	}

	private static final class DefaultDomainReport implements DomainReport, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final String domain;
		private final String name;
		private final String type;
		private final String path;
		private final boolean cached;

		private DefaultDomainReport(String domain, String name, String type, String path, boolean cached) {
			this.domain = domain;
			this.name = name;
			this.type = type;
			this.path = path;
			this.cached = cached;
		}

		@Override
		public String domain() {
			return domain;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public String path() {
			return path;
		}

		@Override
		public boolean cached() {
			return cached;
		}
	}

	private static final class DefaultDomainOperation implements DomainOperation, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final String domain;
		private final String type;
		private final String name;
		private final String className;

		private DefaultDomainOperation(String domain, String type, String name, String className) {
			this.domain = domain;
			this.type = type;
			this.name = name;
			this.className = className;
		}

		@Override
		public String domain() {
			return domain;
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String className() {
			return className;
		}
	}
}