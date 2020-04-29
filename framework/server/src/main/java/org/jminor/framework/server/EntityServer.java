/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.TaskScheduler;
import org.jminor.common.Util;
import org.jminor.common.db.database.Database;
import org.jminor.common.db.exception.AuthenticationException;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolProvider;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.event.EventListener;
import org.jminor.common.rmi.client.Clients;
import org.jminor.common.rmi.client.ConnectionRequest;
import org.jminor.common.rmi.server.AbstractServer;
import org.jminor.common.rmi.server.ClientLog;
import org.jminor.common.rmi.server.RemoteClient;
import org.jminor.common.rmi.server.Server;
import org.jminor.common.rmi.server.Servers;
import org.jminor.common.rmi.server.exception.ConnectionNotAvailableException;
import org.jminor.common.rmi.server.exception.LoginException;
import org.jminor.common.rmi.server.exception.ServerAuthenticationException;
import org.jminor.common.user.User;
import org.jminor.framework.db.rmi.RemoteEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.EntityDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * A remote server class, responsible for handling requests for AbstractRemoteEntityConnections.
 */
public class EntityServer extends AbstractServer<AbstractRemoteEntityConnection, EntityServerAdmin> {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(EntityServer.class);

  protected static final String START = "start";
  protected static final String STOP = "stop";
  protected static final String SHUTDOWN = "shutdown";
  protected static final String RESTART = "restart";

  private static final int DEFAULT_MAINTENANCE_INTERVAL_MS = 30000;

  private final EntityServerConfiguration configuration;
  private final Database database;
  private final TaskScheduler connectionMaintenanceScheduler = new TaskScheduler(new MaintenanceTask(),
          DEFAULT_MAINTENANCE_INTERVAL_MS, DEFAULT_MAINTENANCE_INTERVAL_MS, TimeUnit.MILLISECONDS).start();
  private final Registry registry;
  private final boolean clientLoggingEnabled;
  private final Map<String, Integer> clientTypeConnectionTimeouts = new HashMap<>();

  private final EntityServerAdmin serverAdmin;

  private int connectionTimeout;

  /**
   * Constructs a new DefaultEntityServer and binds it to a registry on the given port
   * @param configuration the server configuration
   * @throws RemoteException in case of a remote exception
   * @throws RuntimeException in case the domain model classes are not found on the classpath or if the
   * jdbc driver class is not found or in case of an exception while constructing the initial pooled connections
   */
  public EntityServer(final EntityServerConfiguration configuration) throws RemoteException {
    super(configuration);
    addShutdownListener(new ShutdownListener());
    this.configuration = configuration;
    try {
      this.database = requireNonNull(configuration.getDatabase(), "database");
      this.registry = LocateRegistry.createRegistry(configuration.getRegistryPort());
      this.clientLoggingEnabled = configuration.getClientLoggingEnabled();
      setConnectionTimeout(configuration.getConnectionTimeout());
      setClientTypeConnectionTimeouts(configuration.getClientSpecificConnectionTimeouts());
      loadDomainModels(configuration.getDomainModelClassNames());
      initializeConnectionPools(configuration.getDatabase(), configuration.getConnectionPoolProvider(), configuration.getStartupPoolUsers());
      setConnectionLimit(configuration.getConnectionLimit());
      serverAdmin = initializeServerAdmin(configuration);
      bindToRegistry(configuration.getRegistryPort());
    }
    catch (final Throwable t) {
      throw logShutdownAndReturn(new RuntimeException(t), this);
    }
  }

  /**
   * @param user the server admin user
   * @return the administration interface for this server
   * @throws ServerAuthenticationException in case authentication fails
   * @throws IllegalStateException in case no server admin instance is available
   */
  @Override
  public final EntityServerAdmin getServerAdmin(final User user) throws ServerAuthenticationException {
    validateUserCredentials(user, configuration.getAdminUser());
    if (serverAdmin == null) {
      throw new IllegalStateException("No admin instance available");
    }

    return serverAdmin;
  }

  @Override
  public final int getServerLoad() {
    return AbstractRemoteEntityConnection.getRequestsPerSecond();
  }

  @Override
  protected final AbstractRemoteEntityConnection doConnect(final RemoteClient remoteClient)
          throws RemoteException, LoginException, ConnectionNotAvailableException {
    requireNonNull(remoteClient, "remoteClient");
    try {
      final ConnectionPool connectionPool = ConnectionPools.getConnectionPool(remoteClient.getDatabaseUser().getUsername());
      if (connectionPool != null) {
        checkConnectionPoolCredentials(connectionPool.getUser(), remoteClient.getDatabaseUser());
      }

      final AbstractRemoteEntityConnection connection = createRemoteConnection(connectionPool, getDatabase(), remoteClient,
              configuration.getServerPort(), configuration.getRmiClientSocketFactory(), configuration.getRmiServerSocketFactory());
      connection.setLoggingEnabled(clientLoggingEnabled);

      connection.addDisconnectListener(this::disconnectQuietly);
      LOG.debug("{} connected", remoteClient);

      return connection;
    }
    catch (final AuthenticationException e) {
      throw new ServerAuthenticationException(e.getMessage());
    }
    catch (final RemoteException | ServerAuthenticationException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.debug(remoteClient + " unable to connect", e);
      throw new LoginException(e.getMessage());
    }
  }

  @Override
  protected final void doDisconnect(final AbstractRemoteEntityConnection connection) throws RemoteException {
    connection.disconnect();
  }

  /**
   * Creates the remote connection provided by this server
   * @param connectionPool the connection pool to use, if none is provided a direct connection to the database is established
   * @param database the underlying database
   * @param remoteClient the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param clientSocketFactory the client socket factory, null for default
   * @param serverSocketFactory the server socket factory, null for default
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   * @return a remote connection
   */
  protected AbstractRemoteEntityConnection createRemoteConnection(final ConnectionPool connectionPool, final Database database,
                                                                  final RemoteClient remoteClient, final int port,
                                                                  final RMIClientSocketFactory clientSocketFactory,
                                                                  final RMIServerSocketFactory serverSocketFactory)
          throws RemoteException, DatabaseException {
    final Domain domainModel = getClientDomainModel(remoteClient);
    if (connectionPool != null) {
      return new DefaultRemoteEntityConnection(domainModel, connectionPool, remoteClient, port,
              clientSocketFactory, serverSocketFactory);
    }

    return new DefaultRemoteEntityConnection(domainModel, database, remoteClient, port,
            clientSocketFactory, serverSocketFactory);
  }

  /**
   * @return the underlying Database implementation class
   */
  final Database getDatabase() {
    return database;
  }

  /**
   * @return the connection timeout
   */
  final int getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * @param timeout the new timeout value in milliseconds
   * @throws IllegalArgumentException in case timeout is less than zero
   */
  final void setConnectionTimeout(final int timeout) {
    if (timeout < 0) {
      throw new IllegalArgumentException("Connection timeout must be a positive integer");
    }
    this.connectionTimeout = timeout;
  }

  /**
   * @param clientTypeConnectionTimeouts the timeout values mapped to each clientTypeId
   */
  final void setClientTypeConnectionTimeouts(final Map<String, Integer> clientTypeConnectionTimeouts) {
    this.clientTypeConnectionTimeouts.putAll(clientTypeConnectionTimeouts);
  }

  /**
   * Returns the statistics gathered via {@link Database#countQuery(String)}.
   * @return a {@link Database.Statistics} object containing query statistics collected since
   * the last time this function was called.
   */
  final Database.Statistics getDatabaseStatistics() {
    return database.getStatistics();
  }

  /**
   * @return info on all connected users
   */
  final Collection<User> getUsers() {
    return getConnections().keySet().stream().map(ConnectionRequest::getUser).collect(toSet());
  }

  /**
   * @return info on all connected clients
   */
  final Collection<RemoteClient> getClients() {
    return new ArrayList<>(getConnections().keySet());
  }

  /**
   * @param user the user
   * @return all clients connected with the given user
   */
  final Collection<RemoteClient> getClients(final User user) {
    return getConnections().keySet().stream().filter(remoteClient ->
            user == null || remoteClient.getUser().equals(user)).collect(toList());
  }

  /**
   * @param clientTypeId the client type ID
   * @return all clients of the given type
   */
  final Collection<RemoteClient> getClients(final String clientTypeId) {
    //using the remoteClient from the connection since it contains the correct database user
    return getConnections().values().stream()
            .filter(connection -> connection.getRemoteClient().getClientTypeId().equals(clientTypeId))
            .map(AbstractRemoteEntityConnection::getRemoteClient).collect(toList());
  }

  /**
   * @return a map containing all defined entityIds, with their respective table names as an associated value
   */
  final Map<String, String> getEntityDefinitions() {
    final Map<String, String> definitions = new HashMap<>();
    for (final Domain domain : Domain.getRegisteredDomains()) {
      for (final EntityDefinition definition : domain.getDefinitions()) {
        definitions.put(definition.getEntityId(), definition.getTableName());
      }
    }

    return definitions;
  }

  /**
   * @return the maintenance check interval in ms
   */
  final int getMaintenanceInterval() {
    return connectionMaintenanceScheduler.getInterval();
  }

  /**
   * @param maintenanceInterval the new maintenance interval in ms
   */
  final void setMaintenanceInterval(final int maintenanceInterval) {
    connectionMaintenanceScheduler.setInterval(maintenanceInterval);
  }

  /**
   * Returns the client log for the connection identified by the given key.
   * @param clientId the UUID identifying the client
   * @return the client log for the given connection
   */
  final ClientLog getClientLog(final UUID clientId) {
    final AbstractRemoteEntityConnection connection = getConnection(clientId);
    if (connection != null) {
      return connection.getClientLog();
    }

    throw new IllegalArgumentException("Client not connected: " + clientId);
  }

  /**
   * @param clientId the client ID
   * @return true if logging is enabled for the given client
   */
  final boolean isLoggingEnabled(final UUID clientId) {
    final AbstractRemoteEntityConnection connection = getConnection(clientId);
    if (connection != null) {
      return connection.isLoggingEnabled();
    }

    return false;
  }

  /**
   * @param clientId the client ID
   * @param loggingEnabled the new logging status
   */
  final void setLoggingEnabled(final UUID clientId, final boolean loggingEnabled) {
    final AbstractRemoteEntityConnection connection = getConnection(clientId);
    if (connection != null) {
      connection.setLoggingEnabled(loggingEnabled);
    }
  }

  /**
   * Disconnects clients that have exceeded the idle timeout.
   * @throws RemoteException in case of an exception
   */
  final void maintainConnections() throws RemoteException {
    final List<RemoteClient> clients = new ArrayList<>(getConnections().keySet());
    for (final RemoteClient client : clients) {
      final AbstractRemoteEntityConnection connection = getConnection(client.getClientId());
      if (!connection.isActive()) {
        final boolean connected = connection.isConnected();
        final boolean timedOut = hasConnectionTimedOut(client.getClientTypeId(), connection);
        if (!connected || timedOut) {
          LOG.debug("Removing connection {}, connected: {}, timeout: {}", new Object[] {client, connected, timedOut});
          disconnect(client.getClientId());
        }
      }
    }
  }

  /**
   * @param timedOutOnly if true only connections that have timed out are culled
   * @throws RemoteException in case of an exception
   * @see #hasConnectionTimedOut(String, AbstractRemoteEntityConnection)
   */
  final void disconnectClients(final boolean timedOutOnly) throws RemoteException {
    final List<RemoteClient> clients = new ArrayList<>(getConnections().keySet());
    for (final RemoteClient client : clients) {
      final AbstractRemoteEntityConnection connection = getConnection(client.getClientId());
      if (timedOutOnly) {
        final boolean active = connection.isActive();
        if (!active && hasConnectionTimedOut(client.getClientTypeId(), connection)) {
          disconnect(client.getClientId());
        }
      }
      else {
        disconnect(client.getClientId());
      }
    }
  }

  private void disconnectQuietly(final AbstractRemoteEntityConnection connection) {
    try {
      disconnect(connection.getRemoteClient().getClientId());
    }
    catch (final RemoteException ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

  /**
   * Creates a {@link EntityServerAdmin} instance if the server admin port is specified.
   * @param configuration the server configuration
   * @return a admin instance
   * @throws RemoteException in case of an exception
   */
  private EntityServerAdmin initializeServerAdmin(final EntityServerConfiguration configuration) throws RemoteException {
    if (configuration.getServerAdminPort() != null) {
      return new DefaultEntityServerAdmin(this, configuration);
    }

    return null;
  }

  /**
   * Binds this server instance to the registry
   * @throws RemoteException in case of an exception
   * @param registryPort the registry port
   */
  private void bindToRegistry(final int registryPort) throws RemoteException {
    registry.rebind(getServerInformation().getServerName(), this);
    final String connectInfo = getServerInformation().getServerName() + " bound to registry on port: " + registryPort;
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  /**
   * Checks the credentials provided by {@code remoteClient} against the credentials
   * found in the connection pool user, assuming the user names match
   * @param connectionPoolUser the connection pool user credentials
   * @param user the user credentials to check
   * @throws ServerAuthenticationException in case the password does not match the one in the connection pool user
   */
  private static void checkConnectionPoolCredentials(final User connectionPoolUser, final User user) throws ServerAuthenticationException {
    if (!Arrays.equals(connectionPoolUser.getPassword(), user.getPassword())) {
      throw new ServerAuthenticationException("Wrong username or password");
    }
  }

  private boolean hasConnectionTimedOut(final String clientTypeId, final AbstractRemoteEntityConnection connection) {
    Integer timeout = clientTypeConnectionTimeouts.get(clientTypeId);
    if (timeout == null) {
      timeout = connectionTimeout;
    }

    return connection.hasBeenInactive(timeout);
  }

  private static Domain getClientDomainModel(final RemoteClient remoteClient) {
    final String domainId = (String) remoteClient.getParameters().get(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID);
    if (domainId == null) {
      throw new IllegalArgumentException("'" + RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID + "' parameter not specified");
    }

    return Domain.getDomain(domainId);
  }

  private static void loadDomainModels(final Collection<String> domainModelClassNames) throws Throwable {
    try {
      for (final String className : domainModelClassNames) {
        LOG.info("Server loading and registering domain model class '" + className + " from classpath");
        final Domain domain = (Domain) Class.forName(className).getDeclaredConstructor().newInstance();
        domain.registerDomain();
      }
    }
    catch (final InvocationTargetException ite) {
      LOG.error("Exception while loading and registering domain model", ite);
      throw ite.getCause();
    }
    catch (final Exception e) {
      LOG.error("Exception while loading and registering domain model", e);
      throw e;
    }
  }

  private static void initializeConnectionPools(final Database database, final String connectionPoolProviderClassName,
                                                final Collection<User> startupPoolUsers) throws DatabaseException {
    if (!startupPoolUsers.isEmpty()) {
      final ConnectionPoolProvider poolProvider;
      if (Util.nullOrEmpty(connectionPoolProviderClassName)) {
        poolProvider = ConnectionPoolProvider.getConnectionPoolProvider();
      }
      else {
        poolProvider = ConnectionPoolProvider.getConnectionPoolProvider(connectionPoolProviderClassName);
      }
      ConnectionPools.initializeConnectionPools(poolProvider, database, startupPoolUsers);
    }
  }

  private static <T extends Throwable> T logShutdownAndReturn(final T exception, final EntityServer server) {
    LOG.error("Exception on server startup", exception);
    server.shutdown();

    return exception;
  }

  private final class MaintenanceTask implements Runnable {
    @Override
    public void run() {
      try {
        if (getConnectionCount() > 0) {
          maintainConnections();
        }
      }
      catch (final Exception e) {
        LOG.error("Exception while maintaining connections", e);
      }
    }
  }

  /**
   * Starts the server, using the configuration from system properties.
   * @return the server instance
   * @throws RemoteException in case of an exception
   */
  public static EntityServer startServer() throws RemoteException {
    return startServer(EntityServerConfiguration.fromSystemProperties());
  }

  /**
   * Starts the server.
   * @param configuration the configuration
   * @return the server instance
   * @throws RemoteException in case of an exception
   */
  public static synchronized EntityServer startServer(final EntityServerConfiguration configuration) throws RemoteException {
    requireNonNull(configuration, "configuration");
    try {
      return new EntityServer(configuration);
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error("Exception when starting server", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Connects to the server and shuts it down
   */
  static synchronized void shutdownServer() throws ServerAuthenticationException {
    final EntityServerConfiguration configuration = EntityServerConfiguration.fromSystemProperties();
    final String serverName = configuration.getServerName();
    final int registryPort = configuration.getRegistryPort();
    final User adminUser = configuration.getAdminUser();
    if (adminUser == null) {
      throw new ServerAuthenticationException("No admin user specified");
    }
    Clients.resolveTrustStoreFromClasspath(DefaultEntityServerAdmin.class.getSimpleName());
    try {
      final Registry registry = Servers.getRegistry(registryPort);
      final Server<?, EntityServerAdmin> server = (Server) registry.lookup(serverName);
      final EntityServerAdmin serverAdmin = server.getServerAdmin(adminUser);
      final String shutDownInfo = serverName + " found in registry on port: " + registryPort + ", shutting down";
      LOG.info(shutDownInfo);
      System.out.println(shutDownInfo);
      serverAdmin.shutdown();
    }
    catch (final RemoteException e) {
      System.out.println("Unable to shutdown server: " + e.getMessage());
      LOG.error("Error on shutdown", e);
    }
    catch (final NotBoundException e) {
      System.out.println(serverName + " not bound to registry on port: " + registryPort);
    }
    catch (final ServerAuthenticationException e) {
      LOG.error("Admin user info not provided or incorrect", e);
      throw e;
    }
  }

  /**
   * If no arguments are supplied a new DefaultEntityServer is started.
   * @param arguments 'start' (or no argument) starts the server, 'stop' or 'shutdown' causes a running server to be shut down and 'restart' restarts the server
   * @throws RemoteException in case of a remote exception during service export
   * @throws ServerAuthenticationException in case of missing or incorrect admin user information
   */
  public static void main(final String[] arguments) throws RemoteException, ServerAuthenticationException {
    final String argument = arguments.length == 0 ? START : arguments[0];
    switch (argument) {
      case START:
        startServer();
        break;
      case STOP:
      case SHUTDOWN:
        shutdownServer();
        break;
      case RESTART:
        shutdownServer();
        startServer();
        break;
      default:
        throw new IllegalArgumentException("Unknown argument '" + argument + "'");
    }
  }

  private final class ShutdownListener implements EventListener {

    @Override
    public void onEvent() {
      try {
        unexportObject(registry, true);
      }
      catch (final NoSuchObjectException ignored) {/*ignored*/}
      try {
        if (serverAdmin != null) {
          unexportObject(serverAdmin, true);
        }
      }
      catch (final NoSuchObjectException ignored) {/*ignored*/}
      connectionMaintenanceScheduler.stop();
      ConnectionPools.closeConnectionPools();
    }
  }
}