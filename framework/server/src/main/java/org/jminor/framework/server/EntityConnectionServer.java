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
import org.jminor.common.remote.client.ConnectionRequest;
import org.jminor.common.remote.server.AbstractServer;
import org.jminor.common.remote.server.ClientLog;
import org.jminor.common.remote.server.ConnectionValidator;
import org.jminor.common.remote.server.LoginProxy;
import org.jminor.common.remote.server.RemoteClient;
import org.jminor.common.remote.server.SerializationWhitelist;
import org.jminor.common.remote.server.Server;
import org.jminor.common.remote.server.Servers;
import org.jminor.common.remote.server.exception.ConnectionNotAvailableException;
import org.jminor.common.remote.server.exception.LoginException;
import org.jminor.common.remote.server.exception.ServerAuthenticationException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.common.version.Versions;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.EntityDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.lang.reflect.InvocationTargetException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jminor.common.Util.nullOrEmpty;
import static org.jminor.common.remote.server.SerializationWhitelist.isSerializationDryRunActive;
import static org.jminor.common.remote.server.SerializationWhitelist.writeDryRunWhitelist;

/**
 * A remote server class, responsible for handling requests for AbstractRemoteEntityConnections.
 */
public class EntityConnectionServer extends AbstractServer<AbstractRemoteEntityConnection, EntityConnectionServerAdmin> {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(EntityConnectionServer.class);

  protected static final String START = "start";
  protected static final String STOP = "stop";
  protected static final String SHUTDOWN = "shutdown";
  protected static final String RESTART = "restart";

  /** Only Java 8 compatible for now */
  private static final boolean OBJECT_INPUT_FILTER_ON_CLASSPATH = Util.onClasspath("sun.misc.ObjectInputFilter");
  private static final int DEFAULT_MAINTENANCE_INTERVAL_MS = 30000;
  private static final String FROM_CLASSPATH = "' from classpath";

  private final Database database;
  private final TaskScheduler connectionMaintenanceScheduler = new TaskScheduler(new MaintenanceTask(),
          DEFAULT_MAINTENANCE_INTERVAL_MS, DEFAULT_MAINTENANCE_INTERVAL_MS, TimeUnit.MILLISECONDS).start();
  private final Registry registry;
  private final boolean sslEnabled;
  private final boolean clientLoggingEnabled;
  private final Map<String, Integer> clientTypeConnectionTimeouts = new HashMap<>();
  private final Thread shutdownHook;
  private final Collection<AuxiliaryServer> auxiliaryServers = new LinkedList<>();

  private final EntityConnectionServerAdmin serverAdmin;
  private final User adminUser;

  private int connectionTimeout;

  /**
   * Constructs a new DefaultEntityConnectionServer and binds it to a registry on the given port
   * @param serverName the serverName
   * @param configuration the server configuration
   * @throws RemoteException in case of a remote exception
   * @throws RuntimeException in case the domain model classes are not found on the classpath or if the
   * jdbc driver class is not found or in case of an exception while constructing the initial pooled connections
   */
  public EntityConnectionServer(final String serverName, final ServerConfiguration configuration) throws RemoteException {
    super(configuration.getServerPort(), serverName, configuration.getSslEnabled() ? new SslRMIClientSocketFactory() : null,
            configuration.getSslEnabled() ? new SslRMIServerSocketFactory() : null);
    try {
      if (OBJECT_INPUT_FILTER_ON_CLASSPATH) {
        if (configuration.getSerializationFilterDryRun()) {
          SerializationWhitelist.configureDryRun(configuration.getSerializationFilterWhitelist());
        }
        else {
          SerializationWhitelist.configure(configuration.getSerializationFilterWhitelist());
        }
      }
      this.shutdownHook = new Thread(getShutdownHook());
      Runtime.getRuntime().addShutdownHook(this.shutdownHook);
      this.database = requireNonNull(configuration.getDatabase(), "database");
      this.registry = LocateRegistry.createRegistry(configuration.getRegistryPort());
      this.sslEnabled = configuration.getSslEnabled();
      this.clientLoggingEnabled = configuration.getClientLoggingEnabled();
      this.adminUser = configuration.getAdminUser();
      setConnectionTimeout(configuration.getConnectionTimeout());
      setClientTypeConnectionTimeouts(configuration.getClientSpecificConnectionTimeouts());
      loadDomainModels(configuration.getDomainModelClassNames());
      initializeConnectionPools(configuration.getDatabase(), configuration.getConnectionPoolProvider(), configuration.getStartupPoolUsers());
      loadLoginProxies(configuration.getLoginProxyClassNames());
      loadConnectionValidators(configuration.getConnectionValidatorClassNames());
      setConnectionLimit(configuration.getConnectionLimit());
      startAuxiliaryServers(configuration.getAuxiliaryServerClassNames());
      serverAdmin = new DefaultEntityConnectionServerAdmin(this, configuration.getServerAdminPort());
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
   */
  @Override
  public final EntityConnectionServerAdmin getServerAdmin(final User user) throws ServerAuthenticationException {
    validateUserCredentials(user, adminUser);

    return serverAdmin;
  }

  @Override
  public final int getServerLoad() {
    return AbstractRemoteEntityConnection.getRequestsPerSecond();
  }

  /**
   * @return true if client logging is enabled
   */
  public final boolean isClientLoggingEnabled() {
    return clientLoggingEnabled;
  }

  @Override
  protected final AbstractRemoteEntityConnection doConnect(final RemoteClient remoteClient)
          throws RemoteException, LoginException, ConnectionNotAvailableException {
    try {
      final ConnectionPool connectionPool = ConnectionPools.getConnectionPool(remoteClient.getDatabaseUser().getUsername());
      if (connectionPool != null) {
        checkConnectionPoolCredentials(connectionPool.getUser(), remoteClient.getDatabaseUser());
      }

      final AbstractRemoteEntityConnection connection = createRemoteConnection(connectionPool, getDatabase(), remoteClient,
              getServerInfo().getServerPort(), isSslEnabled() ? new SslRMIClientSocketFactory() : null,
              isSslEnabled() ? new SslRMIServerSocketFactory() : null);
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
   * @param connectionPool the connection pool to use, if none is provided a local connection is established
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
    if (clientTypeConnectionTimeouts != null) {
      this.clientTypeConnectionTimeouts.putAll(clientTypeConnectionTimeouts);
    }
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
   * @return true if connections to this server are ssl enabled
   */
  final boolean isSslEnabled() {
    return sslEnabled;
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

  @Override
  protected final void onShutdown() throws RemoteException {
    super.onShutdown();
    connectionMaintenanceScheduler.stop();
    ConnectionPools.closeConnectionPools();
    auxiliaryServers.forEach(EntityConnectionServer::stopAuxiliaryServer);
    if (database.isEmbedded()) {
      database.shutdownEmbedded(null);
    }
    try {
      UnicastRemoteObject.unexportObject(registry, true);
    }
    catch (final NoSuchObjectException ignored) {/*ignored*/}
    UnicastRemoteObject.unexportObject(serverAdmin, true);
    if (OBJECT_INPUT_FILTER_ON_CLASSPATH && isSerializationDryRunActive()) {
      writeDryRunWhitelist();
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
   * Binds this server instance to the registry
   * @throws RemoteException in case of an exception
   * @param registryPort the registry port
   */
  private void bindToRegistry(final int registryPort) throws RemoteException {
    registry.rebind(getServerInfo().getServerName(), this);
    final String connectInfo = getServerInfo().getServerName() + " bound to registry on port: " + registryPort;
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  private void startAuxiliaryServers(final Collection<String> auxiliaryServerClassNames) {
    if (!Util.nullOrEmpty(auxiliaryServerClassNames)) {
      try {
        for (final String auxiliaryServerClassName : auxiliaryServerClassNames) {
          final AuxiliaryServer auxiliaryServer = AuxiliaryServer.getAuxiliaryServer(auxiliaryServerClassName);
          auxiliaryServer.setServer(this);
          auxiliaryServers.add(auxiliaryServer);
          newSingleThreadScheduledExecutor(new DaemonThreadFactory()).submit((Callable) () ->
                  startAuxiliaryServer(auxiliaryServer)).get();
        }
      }
      catch (final Exception e) {
        LOG.error("Starting auxiliary server", e);
        throw new RuntimeException(e);
      }
    }
  }

  private void loadLoginProxies(final Collection<String> loginProxyClassNames) throws ClassNotFoundException {
    if (loginProxyClassNames != null) {
      for (final String loginProxyClassName : loginProxyClassNames) {
        LOG.info("Server loading login proxy class '" + loginProxyClassName + FROM_CLASSPATH);
        final Class loginProxyClass = Class.forName(loginProxyClassName);
        try {
          final LoginProxy proxy = (LoginProxy) loginProxyClass.getConstructor().newInstance();
          setLoginProxy(proxy.getClientTypeId(), proxy);
        }
        catch (final Exception ex) {
          LOG.error("Exception while instantiating LoginProxy: " + loginProxyClassName, ex);
          throw new RuntimeException(ex);
        }
      }
    }
  }

  private void loadConnectionValidators(final Collection<String> connectionValidatorClassNames) throws ClassNotFoundException {
    if (connectionValidatorClassNames != null) {
      for (final String connectionValidatorClassName : connectionValidatorClassNames) {
        LOG.info("Server loading connection validation class '" + connectionValidatorClassName + FROM_CLASSPATH);
        final Class connectionValidatorClass = Class.forName(connectionValidatorClassName);
        try {
          final ConnectionValidator validator = (ConnectionValidator) connectionValidatorClass.getConstructor().newInstance();
          setConnectionValidator(validator.getClientTypeId(), validator);
        }
        catch (final Exception ex) {
          LOG.error("Exception while instantiating ConnectionValidator: " + connectionValidatorClassName, ex);
          throw new RuntimeException(ex);
        }
      }
    }
  }

  private Runnable getShutdownHook() {
    return () -> {
      try {
        shutdown();
      }
      catch (final RemoteException e) {
        LOG.error("Exception during shutdown", e);
      }
    };
  }

  protected static String initializeServerName(final String databaseHost, final String sid) {
    return Server.SERVER_NAME_PREFIX.get() + " " + Versions.getVersionString()
            + "@" + (sid != null ? sid.toUpperCase() : databaseHost.toUpperCase());
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
      if (domainModelClassNames != null) {
        for (final String className : domainModelClassNames) {
          final String message = "Server loading and registering domain model class '" + className + FROM_CLASSPATH;
          LOG.info(message);
          final Domain domain = (Domain) Class.forName(className).getDeclaredConstructor().newInstance();
          domain.registerDomain();
        }
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
    if (!nullOrEmpty(startupPoolUsers)) {
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

  private static Object startAuxiliaryServer(final AuxiliaryServer server) throws Exception {
    try {
      server.startServer();

      return null;
    }
    catch (final Exception e) {
      LOG.error("Starting auxiliary server", e);
      throw e;
    }
  }

  private static void stopAuxiliaryServer(final AuxiliaryServer server) {
    try {
      server.stopServer();
    }
    catch (final Exception e) {
      LOG.error("Stopping auxiliary server", e);
    }
  }

  private static <T extends Throwable> T logShutdownAndReturn(final T exception, final EntityConnectionServer server) {
    LOG.error("Exception on server startup", exception);
    try {
      server.shutdown();
    }
    catch (final RemoteException ignored) {/*ignored*/}

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

  private static final class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(final Runnable runnable) {
      final Thread thread = new Thread(runnable);
      thread.setDaemon(true);

      return thread;
    }
  }

  /**
   * Starts the server, using the configuration from system properties.
   * @return the server instance
   * @throws RemoteException in case of an exception
   */
  public static synchronized EntityConnectionServer startServer() throws RemoteException {
    return startServer(ServerConfiguration.fromSystemProperties());
  }

  /**
   * Starts the server.
   * @param configuration the configuration
   * @return the server instance
   * @throws RemoteException in case of an exception
   */
  public static synchronized EntityConnectionServer startServer(final ServerConfiguration configuration) throws RemoteException {
    requireNonNull(configuration, "configuration");
    try {
      final String serverName = initializeServerName(configuration.getDatabase().getHost(), configuration.getDatabase().getSid());

      return new EntityConnectionServer(serverName, configuration);
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
    final int registryPort = Server.REGISTRY_PORT.get();
    final String sid = Database.DATABASE_SID.get();
    final String host = Database.DATABASE_HOST.get();
    final String serverName = initializeServerName(host, sid);
    final String adminUserString = Server.SERVER_ADMIN_USER.get();
    if (nullOrEmpty(adminUserString)) {
      throw new ServerAuthenticationException("No admin user specified");
    }
    final User adminUser = Users.parseUser(adminUserString);
    Servers.resolveTrustStoreFromClasspath(DefaultEntityConnectionServerAdmin.class.getSimpleName());
    try {
      final Registry registry = Servers.getRegistry(registryPort);
      final Server<?, EntityConnectionServerAdmin> server = (Server) registry.lookup(serverName);
      final EntityConnectionServerAdmin serverAdmin = server.getServerAdmin(adminUser);
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
   * If no arguments are supplied a new DefaultEntityConnectionServer is started.
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
}