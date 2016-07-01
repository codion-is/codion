/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Version;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.AuthenticationException;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolProvider;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.model.tools.TaskScheduler;
import org.jminor.common.server.AbstractServer;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.ConnectionValidator;
import org.jminor.common.server.LoginProxy;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.RemoteEntityConnection;
import org.jminor.framework.domain.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The remote server class, responsible for handling requests for RemoteEntityConnections.
 */
public final class DefaultEntityConnectionServer extends AbstractServer<RemoteEntityConnection, Remote>
        implements Server<RemoteEntityConnection, Remote> {

  private static final long serialVersionUID = 1;

  static {
    Configuration.init();
  }

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityConnectionServer.class);

  private static final String START = "start";
  private static final String STOP = "stop";
  private static final String SHUTDOWN = "shutdown";
  private static final String RESTART = "restart";
  private static final int SPLIT_COUNT = 2;
  private static final int DEFAULT_MAINTENANCE_INTERVAL_MS = 30000;
  private static final String FROM_CLASSPATH = "' from classpath";

  private final AuxiliaryServer webServer;
  private final Database database;
  private final TaskScheduler connectionMaintenanceScheduler = new TaskScheduler(new MaintenanceTask(),
          DEFAULT_MAINTENANCE_INTERVAL_MS, DEFAULT_MAINTENANCE_INTERVAL_MS, TimeUnit.MILLISECONDS).start();
  private final int registryPort;
  private final boolean sslEnabled;
  private final boolean clientLoggingEnabled;
  private final Map<String, Integer> clientTimeouts = new HashMap<>();
  private final Thread shutdownHook;

  private final EntityConnectionServerAdmin serverAdmin;
  private final User adminUser;

  private int connectionTimeout;

  /**
   * Constructs a new EntityConnectionServer and binds it to a registry on the given port
   * @param serverName the serverName
   * @param serverPort the port on which to make the server accessible
   * @param serverAdminPort the port on which to make the server admin interface accessible
   * @param registryPort the registry port to use
   * @param database the Database implementation
   * @param sslEnabled if true then ssl is enabled
   * @param connectionLimit the maximum number of concurrent connections, -1 for no limit
   * @param domainModelClassNames the domain model classes to load on startup
   * @param loginProxyClassNames the login proxy classes to initialize on startup
   * @param connectionValidatorClassNames the connection validation classes to initialize on startup
   * @param initialPoolUsers the users for which to initialize connection pools on startup
   * @param webDocumentRoot the web root from which to server files, if any
   * @param webServerPort the web server port, if any
   * @param clientLoggingEnabled if true then client logging is enabled on startup
   * @param connectionTimeout the idle connection timeout
   * @param clientSpecificConnectionTimeouts client specific connection timeouts, mapped to clientTypeID
   * @param adminUser the admin user
   * @throws RemoteException in case of a remote exception
   * @throws RuntimeException in case the domain model classes are not found on the classpath or if the
   * jdbc driver class is not found or in case of an exception while constructing the initial pooled connections
   */
  public DefaultEntityConnectionServer(final String serverName, final int serverPort, final int serverAdminPort,
                                       final int registryPort, final Database database, final boolean sslEnabled,
                                       final int connectionLimit, final Collection<String> domainModelClassNames,
                                       final Collection<String> loginProxyClassNames, final Collection<String> connectionValidatorClassNames,
                                       final Collection<User> initialPoolUsers, final String webDocumentRoot,
                                       final Integer webServerPort, final boolean clientLoggingEnabled,
                                       final int connectionTimeout, final Map<String, Integer> clientSpecificConnectionTimeouts,
                                       final User adminUser)
          throws RemoteException {
    super(serverPort, serverName,
            sslEnabled ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            sslEnabled ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    try {
      this.shutdownHook = new Thread(getShutdownHook());
      Runtime.getRuntime().addShutdownHook(this.shutdownHook);
      this.database = Objects.requireNonNull(database, "database");
      this.registryPort = registryPort;
      this.sslEnabled = sslEnabled;
      this.clientLoggingEnabled = clientLoggingEnabled;
      this.adminUser = adminUser;
      setConnectionTimeout(connectionTimeout);
      setClientSpecificConnectionTimeout(clientSpecificConnectionTimeouts);
      loadDomainModels(domainModelClassNames);
      initializeConnectionPools(database, initialPoolUsers);
      loadLoginProxies(loginProxyClassNames);
      loadConnectionValidators(connectionValidatorClassNames);
      setConnectionLimit(connectionLimit);
      webServer = startWebServer(webDocumentRoot, webServerPort);
      serverAdmin = new DefaultEntityConnectionServerAdmin(this, serverAdminPort);
    }
    catch (final Throwable t) {
      throw logShutdownAndReturn(new RuntimeException(t), this);
    }
  }

  /**
   * @param user the server admin user
   * @return the administration interface for this server
   * @throws ServerException.AuthenticationException in case authentication fails
   */
  @Override
  public Remote getServerAdmin(final User user) throws ServerException.AuthenticationException {
    validateUserCredentials(user, adminUser);

    return serverAdmin;
  }

  /** {@inheritDoc} */
  @Override
  public int getServerLoad() {
    return DefaultRemoteEntityConnection.getRequestsPerSecond();
  }

  /**
   * @return the underlying Database implementation class
   */
  Database getDatabase() {
    return database;
  }

  /**
   * @return the connection timeout
   */
  int getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * @param timeout the new timeout value in milliseconds
   * @throws IllegalArgumentException in case timeout is less than zero
   */
  void setConnectionTimeout(final int timeout) {
    if (timeout < 0) {
      throw new IllegalArgumentException("Connection timeout must be a positive integer");
    }
    this.connectionTimeout = timeout;
  }

  /**
   * @param clientSpecificTimeouts the timeout values mapped to each clientTypeID
   */
  void setClientSpecificConnectionTimeout(final Map<String, Integer> clientSpecificTimeouts) {
    this.clientTimeouts.putAll(clientSpecificTimeouts);
  }

  /**
   * @return info on all connected users
   * @throws RemoteException in case of an exception
   */
  Collection<User> getUsers() throws RemoteException {
    final Set<User> users = new HashSet<>();
    for (final RemoteEntityConnection connection : getConnections().values()) {
      users.add(connection.getUser());
    }

    return users;
  }

  /**
   * @return info on all connected clients
   */
  Collection<ClientInfo> getClients() {
    final Collection<ClientInfo> clients = new ArrayList<>();
    for (final RemoteEntityConnection connection : getConnections().values()) {
      clients.add(((DefaultRemoteEntityConnection) connection).getClientInfo());
    }

    return clients;
  }

  /**
   * @param user the user
   * @return all clients connected with the given user
   * @throws RemoteException in case of an exception
   */
  Collection<ClientInfo> getClients(final User user) throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<>();
    for (final RemoteEntityConnection connection : getConnections().values()) {
      if (user == null || connection.getUser().equals(user)) {
        clients.add(((DefaultRemoteEntityConnection) connection).getClientInfo());
      }
    }

    return clients;
  }

  /**
   * @param clientTypeID the client type ID
   * @return all clients of the given type
   */
  Collection<ClientInfo> getClients(final String clientTypeID) {
    final Collection<ClientInfo> clients = new ArrayList<>();
    for (final RemoteEntityConnection connection : getConnections().values()) {
      if (((DefaultRemoteEntityConnection) connection).getClientInfo().getClientTypeID().equals(clientTypeID)) {
        clients.add(((DefaultRemoteEntityConnection) connection).getClientInfo());
      }
    }

    return clients;
  }

  /**
   * @return the maintenance check interval in ms
   */
  int getMaintenanceInterval() {
    return connectionMaintenanceScheduler.getInterval();
  }

  /**
   * @param maintenanceInterval the new maintenance interval in ms
   */
  void setMaintenanceInterval(final int maintenanceInterval) {
    connectionMaintenanceScheduler.setInterval(maintenanceInterval);
  }

  /**
   * Returns the server log for the connection identified by the given key.
   * @param clientID the UUID identifying the client
   * @return the server log for the given connection
   */
  ClientLog getClientLog(final UUID clientID) {
    final DefaultRemoteEntityConnection connection = (DefaultRemoteEntityConnection) getConnection(clientID);
    if (connection != null) {
      return connection.getClientLog();
    }

    return null;
  }

  /**
   * @param clientID the client ID
   * @return true if logging is enabled for the given client
   */
  boolean isLoggingEnabled(final UUID clientID) {
    final DefaultRemoteEntityConnection connection = (DefaultRemoteEntityConnection) getConnection(clientID);
    if (connection != null) {
      return connection.isLoggingEnabled();
    }

    return false;
  }

  /**
   * @param clientID the client ID
   * @param status the new logging status
   */
  void setLoggingEnabled(final UUID clientID, final boolean status) {
    final DefaultRemoteEntityConnection connection = (DefaultRemoteEntityConnection) getConnection(clientID);
    if (connection != null) {
      connection.setLoggingEnabled(status);
    }
  }

  /**
   * @return the port of the registry this server is using
   */
  int getRegistryPort() {
    return registryPort;
  }

  /**
   * @return true if connections to this server are ssl enabled
   */
  boolean isSslEnabled() {
    return sslEnabled;
  }

  /**
   * Validates and keeps alive local connections and disconnects clients that have exceeded the idle timeout
   * @throws RemoteException in case of an exception
   */
  void maintainConnections() throws RemoteException {
    final List<ClientInfo> clients = new ArrayList<>(getConnections().keySet());
    for (final ClientInfo client : clients) {
      final DefaultRemoteEntityConnection connection = (DefaultRemoteEntityConnection) getConnection(client.getClientID());
      if (!connection.isActive()) {
        final boolean valid = connection.isConnected();
        final boolean timedOut = hasConnectionTimedOut(client.getClientTypeID(), connection);
        if (!valid || timedOut) {
          LOG.debug("Removing connection {}, valid: {}, timeout: {}", new Object[] {client, valid, timedOut});
          disconnect(client.getClientID());
        }
      }
    }
  }

  /**
   * @param timedOutOnly if true only connections that have timed out are culled
   * @throws RemoteException in case of an exception
   * @see #hasConnectionTimedOut(String, DefaultRemoteEntityConnection)
   */
  void removeConnections(final boolean timedOutOnly) throws RemoteException {
    final List<ClientInfo> clients = new ArrayList<>(getConnections().keySet());
    for (final ClientInfo client : clients) {
      final DefaultRemoteEntityConnection connection = (DefaultRemoteEntityConnection) getConnection(client.getClientID());
      if (timedOutOnly) {
        final boolean active = connection.isActive();
        if (!active && hasConnectionTimedOut(client.getClientTypeID(), connection)) {
          disconnect(client.getClientID());
        }
      }
      else {
        disconnect(client.getClientID());
      }
    }
  }

  /**
   * Binds this server instance to the registry
   * @throws RemoteException in case of an exception
   */
  void bindToRegistry() throws RemoteException {
    ServerUtil.initializeRegistry(registryPort);
    ServerUtil.getRegistry(registryPort).rebind(getServerInfo().getServerName(), this);
    final String connectInfo = getServerInfo().getServerName() + " bound to registry on port: " + registryPort;
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  /**
   * @return a map containing all defined entityIDs, with their respective table names as an associated value
   */
  static Map<String, String> getEntityDefinitions() {
    return Entities.getDefinitions();
  }

  /** {@inheritDoc} */
  @Override
  protected void handleShutdown() throws RemoteException {
    connectionMaintenanceScheduler.stop();
    removeConnections(false);
    ConnectionPools.closeConnectionPools();
    try {
      stopWebServer();
    }
    catch (final Exception ignored) {/*ignored*/}
    if (database.isEmbedded()) {
      database.shutdownEmbedded(null);
    }//todo does not work when shutdown requires user authentication, jminor.db.shutdownUser hmmm
    UnicastRemoteObject.unexportObject(serverAdmin, true);
  }

  /** {@inheritDoc} */
  @Override
  protected void doDisconnect(final RemoteEntityConnection connection) throws RemoteException {
    connection.disconnect();
  }

  /** {@inheritDoc} */
  @Override
  protected RemoteEntityConnection doConnect(final ClientInfo clientInfo) throws RemoteException, ServerException.LoginException {
    try {
      final DefaultRemoteEntityConnection connection;
      final ConnectionPool connectionPool = ConnectionPools.getConnectionPool(clientInfo.getDatabaseUser());
      if (connectionPool == null) {
        connection = new DefaultRemoteEntityConnection(database, clientInfo, getServerInfo().getServerPort(),
                clientLoggingEnabled, sslEnabled);
      }
      else {
        checkConnectionPoolCredentials(connectionPool.getUser(), clientInfo.getDatabaseUser());
        connection = new DefaultRemoteEntityConnection(connectionPool, database, clientInfo, getServerInfo().getServerPort(),
                clientLoggingEnabled, sslEnabled);
      }
      connection.addDisconnectListener(() -> {
        try {
          disconnect(connection.getClientInfo().getClientID());
        }
        catch (final RemoteException ex) {
          LOG.error(ex.getMessage(), ex);
        }
      });
      LOG.debug("{} connected", clientInfo);

      return connection;
    }
    catch (final RemoteException e) {
      throw e;
    }
    catch (final AuthenticationException ae) {
      throw ServerException.authenticationException(ae.getMessage());
    }
    catch (final Exception e) {
      LOG.debug(clientInfo + " unable to connect", e);
      throw ServerException.loginException(e.getMessage());
    }
  }

  private void loadLoginProxies(final Collection<String> loginProxyClassNames) throws ClassNotFoundException {
    if (loginProxyClassNames != null) {
      for (final String loginProxyClassName : loginProxyClassNames) {
        final String message = "Server loading login proxy class '" + loginProxyClassName + FROM_CLASSPATH;
        LOG.info(message);
        final Class<?> loginProxyClass = Class.forName(loginProxyClassName);
        try {
          final LoginProxy proxy = (LoginProxy) loginProxyClass.getConstructor().newInstance();
          setLoginProxy(proxy.getClientTypeID(), proxy);
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
        final String message = "Server loading connection validation class '" + connectionValidatorClassName + FROM_CLASSPATH;
        LOG.info(message);
        final Class<?> clientValidatorClass = Class.forName(connectionValidatorClassName);
        try {
          final ConnectionValidator validator = (ConnectionValidator) clientValidatorClass.getConstructor().newInstance();
          setConnectionValidator(validator.getClientTypeID(), validator);
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

  /**
   * Starts the server administered by this admin class
   * @return the admin instance
   * @throws RemoteException in case of an exception
   */
  public static synchronized DefaultEntityConnectionServer startServer() throws RemoteException {
    final Integer serverPort = (Integer) Configuration.getValue(Configuration.SERVER_PORT);
    if (serverPort == null) {
      throw new IllegalArgumentException("Configuration property '" + Configuration.SERVER_PORT + "' is required");
    }
    final Integer registryPort = Configuration.getIntValue(Configuration.REGISTRY_PORT);
    final Integer serverAdminPort = Configuration.getIntValue(Configuration.SERVER_ADMIN_PORT);
    final boolean sslEnabled = Configuration.getBooleanValue(Configuration.SERVER_CONNECTION_SSL_ENABLED);
    final Integer connectionLimit = Configuration.getIntValue(Configuration.SERVER_CONNECTION_LIMIT);
    final Database database = Databases.createInstance();
    final String serverName = initializeServerName(database.getHost(), database.getSid());

    final Collection<String> domainModelClassNames = Configuration.parseCommaSeparatedValues(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    final Collection<String> loginProxyClassNames = Configuration.parseCommaSeparatedValues(Configuration.SERVER_LOGIN_PROXY_CLASSES);
    final Collection<String> connectionValidationClassNames = Configuration.parseCommaSeparatedValues(Configuration.SERVER_CONNECTION_VALIDATOR_CLASSES);
    final Collection<String> initialPoolUsers = Configuration.parseCommaSeparatedValues(Configuration.SERVER_CONNECTION_POOLING_INITIAL);
    final String webDocumentRoot = Configuration.getStringValue(Configuration.WEB_SERVER_DOCUMENT_ROOT);
    final Integer webServerPort = Configuration.getIntValue(Configuration.WEB_SERVER_PORT);
    final boolean clientLoggingEnabled = Configuration.getBooleanValue(Configuration.SERVER_CLIENT_LOGGING_ENABLED);
    final Integer connectionTimeout = Configuration.getIntValue(Configuration.SERVER_CONNECTION_TIMEOUT);
    final Map<String, Integer> clientTimeouts = getClientTimeoutValues();
    final String adminUserString = Configuration.getStringValue(Configuration.SERVER_ADMIN_USER);
    final User adminUser = Util.nullOrEmpty(adminUserString) ? null : User.parseUser(adminUserString);
    if (adminUser == null) {
      LOG.info("No admin user specified");
    }
    else {
      LOG.info("Admin user: " + adminUser);
    }
    DefaultEntityConnectionServer server = null;
    try {
      server = new DefaultEntityConnectionServer(serverName, serverPort, serverAdminPort, registryPort, database,
              sslEnabled, connectionLimit, domainModelClassNames, loginProxyClassNames, connectionValidationClassNames,
              getPoolUsers(initialPoolUsers), webDocumentRoot, webServerPort, clientLoggingEnabled, connectionTimeout,
              clientTimeouts, adminUser);
      server.bindToRegistry();

      return server;
    }
    catch (final Exception e) {
      LOG.error("Exception when starting server", e);
      if (server != null) {
        server.shutdown();
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * Connects to the server and shuts it down
   */
  static synchronized void shutdownServer() throws ServerException.AuthenticationException {
    final int registryPort = Configuration.getIntValue(Configuration.REGISTRY_PORT);
    final String sid = System.getProperty(Database.DATABASE_SID);
    final String host = System.getProperty(Database.DATABASE_HOST);
    final String serverName = initializeServerName(host, sid);
    final String adminUserString = Configuration.getStringValue(Configuration.SERVER_ADMIN_USER);
    if (Util.nullOrEmpty(adminUserString)) {
      throw ServerException.authenticationException("No admin user specified");
    }
    final User adminUser = User.parseUser(adminUserString);
    ServerUtil.resolveTrustStoreFromClasspath(DefaultEntityConnectionServerAdmin.class.getSimpleName());
    try {
      final Registry registry = ServerUtil.getRegistry(registryPort);
      final Server server = (Server) registry.lookup(serverName);
      final EntityConnectionServerAdmin serverAdmin = (EntityConnectionServerAdmin) server.getServerAdmin(adminUser);
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
    catch (final ServerException.AuthenticationException e) {
      LOG.error("Admin user info not provided or incorrect", e);
      throw e;
    }
  }

  /**
   * If no arguments are supplied a new DefaultEntityConnectionServer is started.
   * @param arguments 'start' (or no argument) starts the server, 'stop' or 'shutdown' causes a running server to be shut down and 'restart' restarts the server
   * @throws RemoteException in case of a remote exception during service export
   * @throws ClassNotFoundException in case the domain model classes required for the server is not found or
   * if the jdbc driver class is not found
   * @throws DatabaseException in case of an exception while constructing the initial pooled connections
   * @throws ServerException.AuthenticationException in case of missing or incorrect admin user information
   */
  public static void main(final String[] arguments) throws RemoteException, ClassNotFoundException, DatabaseException,
          ServerException.AuthenticationException {
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

  private static String initializeServerName(final String databaseHost, final String sid) {
    return Configuration.getStringValue(Configuration.SERVER_NAME_PREFIX) + " " + Version.getVersionString()
            + "@" + (sid != null ? sid.toUpperCase() : databaseHost.toUpperCase());
  }

  private static Collection<User> getPoolUsers(final Collection<String> poolUsers) {
    final Collection<User> users = new ArrayList<>();
    for (final String usernamePassword : poolUsers) {
      users.add(User.parseUser(usernamePassword));
    }

    return users;
  }

  private static Map<String, Integer> getClientTimeoutValues() {
    final Collection<String> values = Configuration.parseCommaSeparatedValues(Configuration.SERVER_CLIENT_CONNECTION_TIMEOUT);

    return getClientTimeouts(values);
  }

  private static Map<String, Integer> getClientTimeouts(final Collection<String> values) {
    final Map<String, Integer> timeoutMap = new HashMap<>();
    for (final String clientTimeout : values) {
      final String[] split = splitString(clientTimeout, ":");
      timeoutMap.put(split[0], Integer.parseInt(split[1]));
    }

    return timeoutMap;
  }

  public static String[] splitString(final String usernamePassword, final String delimiter) {
    final String[] splitResult = usernamePassword.split(delimiter);
    if (splitResult.length < SPLIT_COUNT) {
      throw new IllegalArgumentException("Expecting a '" + delimiter + "' delimiter");
    }

    return splitResult;
  }

  private AuxiliaryServer startWebServer(final String webDocumentRoot, final Integer webServerPort) {
    final String webServerClassName = Configuration.getStringValue(Configuration.WEB_SERVER_IMPLEMENTATION_CLASS);
    if (Util.nullOrEmpty(webDocumentRoot) || Util.nullOrEmpty(webServerClassName)) {
      return null;
    }

    try {
      final AuxiliaryServer auxiliaryServer = (AuxiliaryServer) Class.forName(webServerClassName).getConstructor(
              Server.class, String.class, Integer.class).newInstance(this, webDocumentRoot, webServerPort);
      Executors.newSingleThreadExecutor().submit(() -> {
        LOG.info("Starting web server on port: {}, document root: {}", webServerPort, webDocumentRoot);
        try {
          auxiliaryServer.startServer();
        }
        catch (final Exception e) {
          LOG.error(e.getMessage(), e);
          LOG.error("Trying to start web server on port: {}, document root: {}", webServerPort, webDocumentRoot);
        }
      }).get();

      return auxiliaryServer;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Stops the web server in case it's running
   */
  private void stopWebServer() throws Exception {
    if (webServer != null) {
      LOG.info("Shutting down web server");
      webServer.stopServer();
    }
  }

  /**
   * Checks the credentials provided by {@code clientInfo} against the credentials
   * found in the connection pool user, assuming the user names match
   * @param connectionPoolUser the connection pool user credentials
   * @param user the user credentials to check
   * @throws AuthenticationException in case the password does not match the one in the connection pool user
   */
  private static void checkConnectionPoolCredentials(final User connectionPoolUser, final User user) throws AuthenticationException {
    if (!connectionPoolUser.getPassword().equals(user.getPassword())) {
      throw new AuthenticationException("Wrong username or password for connection pool");
    }
  }

  private boolean hasConnectionTimedOut(final String clientTypeID, final DefaultRemoteEntityConnection connection) {
    Integer timeout = clientTimeouts.get(clientTypeID);
    if (timeout == null) {
      timeout = connectionTimeout;
    }

    return connection.hasBeenInactive(timeout);
  }

  private static void loadDomainModels(final Collection<String> domainModelClassNames) throws ClassNotFoundException {
    if (domainModelClassNames != null) {
      for (final String className : domainModelClassNames) {
        final String message = "Server loading domain model class '" + className + FROM_CLASSPATH;
        LOG.info(message);
        Class.forName(className);
      }
    }
  }

  private static void initializeConnectionPools(final Database database, final Collection<User> initialPoolUsers) throws ClassNotFoundException, DatabaseException {
    if (initialPoolUsers != null) {
      final String connectionPoolProviderClassName = Configuration.getStringValue(Configuration.SERVER_CONNECTION_POOL_PROVIDER_CLASS);
      final Class<? extends ConnectionPoolProvider> providerClass;
      if (Util.nullOrEmpty(connectionPoolProviderClassName)) {
        providerClass = null;
      }
      else {
        providerClass = (Class<? extends ConnectionPoolProvider>) Class.forName(connectionPoolProviderClassName);
      }
      ConnectionPools.initializeConnectionPools(providerClass, database, initialPoolUsers,
              Configuration.getIntValue(Configuration.CONNECTION_VALIDITY_CHECK_TIMEOUT));
    }
  }

  private static <T> T logShutdownAndReturn(final T exception, final DefaultEntityConnectionServer server) {
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
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
