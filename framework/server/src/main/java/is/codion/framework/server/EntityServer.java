/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.Util;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.event.EventListener;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.AbstractServer;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.Servers;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

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

  private final EntityServerConfiguration configuration;
  private final Map<DomainType, Domain> domainModels;
  private final Database database;
  private final boolean clientLoggingEnabled;
  private final Map<String, Integer> clientTypeConnectionTimeouts = new HashMap<>();

  private int connectionTimeout;

  /**
   * Constructs a new EntityServer and binds it to a registry on the port found in the configuration.
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
      this.clientLoggingEnabled = configuration.getClientLoggingEnabled();
      this.domainModels = loadDomainModels(configuration.getDomainModelClassNames());
      setAdmin(initializeServerAdmin(configuration));
      setConnectionTimeout(configuration.getConnectionTimeout());
      setClientTypeConnectionTimeouts(configuration.getClientSpecificConnectionTimeouts());
      initializeConnectionPools(configuration.getDatabase(), configuration.getConnectionPoolProvider(), configuration.getStartupPoolUsers());
      setConnectionLimit(configuration.getConnectionLimit());
      bindToRegistry(configuration.getRegistryPort());
    }
    catch (final Throwable t) {
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
  public final EntityServerAdmin getServerAdmin(final User user) throws ServerAuthenticationException {
    validateUserCredentials(user, configuration.getAdminUser());

    return getAdmin();
  }

  @Override
  public final int getServerLoad() {
    return AbstractRemoteEntityConnection.getRequestsPerSecond();
  }

  @Override
  protected final AbstractRemoteEntityConnection doConnect(final RemoteClient remoteClient) throws RemoteException, LoginException {
    requireNonNull(remoteClient, "remoteClient");
    try {
      final AbstractRemoteEntityConnection connection = createRemoteConnection(getDatabase(), remoteClient,
              configuration.getServerPort(), configuration.getRmiClientSocketFactory(), configuration.getRmiServerSocketFactory());
      connection.setLoggingEnabled(clientLoggingEnabled);

      connection.addDisconnectListener(this::disconnectQuietly);
      LOG.debug("{} connected", remoteClient);

      return connection;
    }
    catch (final AuthenticationException e) {
      throw new ServerAuthenticationException(e.getMessage());
    }
    catch (final RemoteException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.debug(remoteClient + " unable to connect", e);
      throw new LoginException(e.getMessage());
    }
  }

  @Override
  protected final void doDisconnect(final AbstractRemoteEntityConnection connection) throws RemoteException {
    connection.close();
  }

  /**
   * Creates the remote connection provided by this server
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
  protected AbstractRemoteEntityConnection createRemoteConnection(final Database database,
                                                                  final RemoteClient remoteClient, final int port,
                                                                  final RMIClientSocketFactory clientSocketFactory,
                                                                  final RMIServerSocketFactory serverSocketFactory)
          throws RemoteException, DatabaseException {
    return new DefaultRemoteEntityConnection(getClientDomainModel(remoteClient), database, remoteClient, port,
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

  @Override
  protected final void maintainConnections(final Collection<ClientConnection<AbstractRemoteEntityConnection>> connections) throws RemoteException {
    for (final ClientConnection<AbstractRemoteEntityConnection> client : connections) {
      final AbstractRemoteEntityConnection connection = client.getConnection();
      if (!connection.isActive()) {
        final boolean connected = connection.isConnected();
        final boolean timedOut = hasConnectionTimedOut(connection);
        if (!connected || timedOut) {
          LOG.debug("Removing connection {}, connected: {}, timeout: {}", client, connected, timedOut);
          disconnect(client.getRemoteClient().getClientId());
        }
      }
    }
  }

  @Override
  protected final Collection<RemoteClient> getClients(final String clientTypeId) {
    //using the remoteClient from the connection since it contains the correct database user
    return getConnections().values().stream()
            .map(AbstractRemoteEntityConnection::getRemoteClient)
            .filter(remoteClient -> remoteClient.getClientTypeId().equals(clientTypeId)).collect(toList());
  }

  /**
   * @return a map containing all defined entityTypes, with their respective table names as an associated value
   */
  final Map<EntityType<?>, String> getEntityDefinitions() {
    final Map<EntityType<?>, String> definitions = new HashMap<>();
    for (final Domain domain : domainModels.values()) {
      for (final EntityDefinition definition : domain.getEntities().getDefinitions()) {
        definitions.put(definition.getEntityType(), definition.getTableName());
      }
    }

    return definitions;
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
   * @param clientId the client id
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
   * @param clientId the client id
   * @param loggingEnabled the new logging status
   */
  final void setLoggingEnabled(final UUID clientId, final boolean loggingEnabled) {
    final AbstractRemoteEntityConnection connection = getConnection(clientId);
    if (connection != null) {
      connection.setLoggingEnabled(loggingEnabled);
    }
  }

  /**
   * @param timedOutOnly if true only connections that have timed out are culled
   * @throws RemoteException in case of an exception
   * @see #hasConnectionTimedOut(AbstractRemoteEntityConnection)
   */
  final void disconnectClients(final boolean timedOutOnly) throws RemoteException {
    final List<RemoteClient> clients = new ArrayList<>(getConnections().keySet());
    for (final RemoteClient client : clients) {
      final AbstractRemoteEntityConnection connection = getConnection(client.getClientId());
      if (timedOutOnly) {
        final boolean active = connection.isActive();
        if (!active && hasConnectionTimedOut(connection)) {
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
    getRegistry().rebind(getServerInformation().getServerName(), this);
    final String connectInfo = getServerInformation().getServerName() + " bound to registry on port: " + registryPort;
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  private boolean hasConnectionTimedOut(final AbstractRemoteEntityConnection connection) {
    Integer timeout = clientTypeConnectionTimeouts.get(connection.getRemoteClient().getClientTypeId());
    if (timeout == null) {
      timeout = connectionTimeout;
    }

    return connection.hasBeenInactive(timeout);
  }

  private Domain getClientDomainModel(final RemoteClient remoteClient) {
    final String domainTypeName = (String) remoteClient.getParameters().get(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE);
    if (domainTypeName == null) {
      throw new IllegalArgumentException("'" + RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE + "' parameter not specified");
    }

    return domainModels.get(DomainType.getDomainType(domainTypeName));
  }

  private static Map<DomainType, Domain> loadDomainModels(final Collection<String> domainModelClassNames) throws Throwable {
    final Map<DomainType, Domain> domains = new HashMap<>();
    final List<Domain> serviceDomains = Domain.getDomains();
    try {
      serviceDomains.forEach(domain -> {
        LOG.info("Server loading and registering domain model '" + domain.getDomainType() + "' as a service");
        domains.put(domain.getDomainType(), domain);
      });
      for (final String className : domainModelClassNames) {
        LOG.info("Server loading and registering domain model class '" + className + "' from classpath");
        final Domain domain = (Domain) Class.forName(className).getConstructor().newInstance();
        domains.put(domain.getDomainType(), domain);
      }

      return unmodifiableMap(domains);
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

  private static void initializeConnectionPools(final Database database, final String connectionPoolFactoryClassName,
                                                final Collection<User> startupPoolUsers) throws DatabaseException {
    if (!startupPoolUsers.isEmpty()) {
      final ConnectionPoolFactory poolFactory;
      if (Util.nullOrEmpty(connectionPoolFactoryClassName)) {
        poolFactory = ConnectionPoolFactory.getInstance();
      }
      else {
        poolFactory = ConnectionPoolFactory.getInstance(connectionPoolFactoryClassName);
      }
      for (final User user : startupPoolUsers) {
        database.initializeConnectionPool(poolFactory, user);
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
      final Server<?, EntityServerAdmin> server = (Server<?, EntityServerAdmin>) registry.lookup(serverName);
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
   * If no arguments are supplied a new EntityServer is started.
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
      database.closeConnectionPools();
      database.shutdownEmbedded();
    }
  }
}