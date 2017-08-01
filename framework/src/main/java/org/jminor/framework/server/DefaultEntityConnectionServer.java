/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.Configuration;
import org.jminor.common.TaskScheduler;
import org.jminor.common.TextUtil;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.Version;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.AuthenticationException;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolProvider;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.server.AbstractServer;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.ConnectionValidator;
import org.jminor.common.server.LoginProxy;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.domain.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.lang.reflect.InvocationTargetException;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A remote server class, responsible for handling requests for AbstractRemoteEntityConnections.
 */
public class DefaultEntityConnectionServer extends AbstractServer<AbstractRemoteEntityConnection, Remote> {

  private static final int DEFAULT_SERVER_CONNECTION_LIMIT = -1;

  private static final int DEFAULT_WEB_SERVER_PORT = 80;

  /**
   * Specifies the web server class, must implement Server.AuxiliaryServer<br>
   * and contain a constructor with the following signature: (Server, String, Integer)<br>
   * for the server, file document root and port respectively<br>
   * Value type: String<br>
   * Default value: org.jminor.framework.plugins.rest.EntityRESTServer
   */
  public static final Value<String> WEB_SERVER_IMPLEMENTATION_CLASS = Configuration.stringValue("jminor.server.web.webServerClass", "org.jminor.framework.plugins.rest.EntityRESTServer");

  /**
   * Specifies the class name of the connection pool provider to user, if none is specified
   * the internal connection pool is used if necessary<br>
   * Value type: String<br>
   * Default value: none
   * @see ConnectionPoolProvider
   */
  public static final Value<String> SERVER_CONNECTION_POOL_PROVIDER_CLASS = Configuration.stringValue("jminor.server.pooling.poolProviderClass", null);

  /**
   * Specifies maximum number of concurrent connections the server accepts<br>
   * -1 indicates no limit and 0 indicates a closed server.
   * Value type: Integer<br>
   * Default value: -1
   */
  public static final Value<Integer> SERVER_CONNECTION_LIMIT = Configuration.integerValue("jminor.server.connectionLimit", DEFAULT_SERVER_CONNECTION_LIMIT);

  /**
   * Specifies the default client connection timeout (ms) in a comma separated list.
   * Example: org.jminor.demos.empdept.client.ui.EmpDeptAppPanel:60000,org.jminor.demos.chinook.ui.ChinookAppPanel:120000
   * Value type: String<br>
   * Default value: none
   */
  public static final Value<String> SERVER_CLIENT_CONNECTION_TIMEOUT = Configuration.stringValue("jminor.server.clientConnectionTimeout", null);

  /**
   * Specifies the port number for the WebStartServer<br>
   * Value type: Integer<br>
   * Default value: 80
   */
  public static final Value<Integer> WEB_SERVER_PORT = Configuration.integerValue("jminor.server.web.port", DEFAULT_WEB_SERVER_PORT);

  /**
   * The initial connection logging status on the server, either true (on) or false (off)<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final Value<Boolean> SERVER_CLIENT_LOGGING_ENABLED = Configuration.booleanValue("jminor.server.clientLoggingEnabled", false);

  /**
   * Specifies the document root for the WebStartServer, if no specified the web server will not be started<br>
   * Value type: String<br>
   * Default value: null
   */
  public static final Value<String> WEB_SERVER_DOCUMENT_ROOT = Configuration.stringValue("jminor.server.web.documentRoot", null);

  /**
   * Specifies a comma separated list of username:password combinations for which to create connection pools on startup
   * Example: scott:tiger,john:foo,paul:bar
   */
  public static final Value<String> SERVER_CONNECTION_POOLING_INITIAL = Configuration.stringValue("jminor.server.pooling.initial", null);

  /**
   * Specifies a comma separated list of ConnectionValidator class names, which should be initialized on server startup,
   * these classes must be available on the server classpath and contain a parameterless constructor
   * @see ConnectionValidator
   */
  public static final Value<String> SERVER_CONNECTION_VALIDATOR_CLASSES = Configuration.stringValue("jminor.server.connectionValidatorClasses", null);

  /**
   * Specifies a comma separated list of LoginProxy class names, which should be initialized on server startup,
   * these classes must be available on the server classpath and contain a parameterless constructor
   * @see LoginProxy
   */
  public static final Value<String> SERVER_LOGIN_PROXY_CLASSES = Configuration.stringValue("jminor.server.loginProxyClasses", null);

  /**
   * Specifies a comma separated list of domain model class names, these classes must be
   * available on the server classpath
   */
  public static final Value<String> SERVER_DOMAIN_MODEL_CLASSES = Configuration.stringValue("jminor.server.domain.classes", null);

  private static final long serialVersionUID = 1;

  protected static final Logger LOG = LoggerFactory.getLogger(DefaultEntityConnectionServer.class);

  protected static final String START = "start";
  protected static final String STOP = "stop";
  protected static final String SHUTDOWN = "shutdown";
  protected static final String RESTART = "restart";

  private static final int DEFAULT_MAINTENANCE_INTERVAL_MS = 30000;
  private static final String FROM_CLASSPATH = "' from classpath";

  private final AuxiliaryServer webServer;
  private final Database database;
  private final TaskScheduler connectionMaintenanceScheduler = new TaskScheduler(new DefaultEntityConnectionServer.MaintenanceTask(),
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
   * Constructs a new DefaultEntityConnectionServer and binds it to a registry on the given port
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
      bindToRegistry();
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
  public final Remote getServerAdmin(final User user) throws ServerException.AuthenticationException {
    validateUserCredentials(user, adminUser);

    return serverAdmin;
  }

  /** {@inheritDoc} */
  @Override
  public final int getServerLoad() {
    return AbstractRemoteEntityConnection.getRequestsPerSecond();
  }

  /**
   * @return true if client loggin is enabled
   */
  public final boolean isClientLoggingEnabled() {
    return clientLoggingEnabled;
  }

  /** {@inheritDoc} */
  @Override
  protected final AbstractRemoteEntityConnection doConnect(final ClientInfo clientInfo) throws RemoteException, ServerException.LoginException,
          ServerException.ServerFullException {
    try {
      final ConnectionPool connectionPool = ConnectionPools.getConnectionPool(clientInfo.getDatabaseUser());
      if (connectionPool != null) {
        checkConnectionPoolCredentials(connectionPool.getUser(), clientInfo.getDatabaseUser());
      }

      final AbstractRemoteEntityConnection connection = createRemoteConnection(connectionPool, getDatabase(), clientInfo,
              getServerInfo().getServerPort(), isClientLoggingEnabled(), isSslEnabled());

      connection.addDisconnectListener(this::disconnectQuietly);
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

  /** {@inheritDoc} */
  @Override
  protected final void doDisconnect(final AbstractRemoteEntityConnection connection) throws RemoteException {
    connection.disconnect();
  }

  /**
   * Creates the remote connection provided by this server
   * @param connectionPool the connection pool to use, if none is provided a local connection is established
   * @param database defines the underlying database
   * @param clientInfo information about the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param clientLoggingEnabled specifies whether or not method logging is enabled
   * @param sslEnabled specifies whether or not ssl should be enabled
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   * @return a remote connection
   */
  protected AbstractRemoteEntityConnection createRemoteConnection(final ConnectionPool connectionPool, final Database database,
                                                                  final ClientInfo clientInfo, final int port,
                                                                  final boolean clientLoggingEnabled, final boolean sslEnabled)
          throws RemoteException, DatabaseException {
    return new DefaultRemoteEntityConnection(connectionPool, database, clientInfo, port, clientLoggingEnabled, sslEnabled);
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
   * @param clientSpecificTimeouts the timeout values mapped to each clientTypeID
   */
  final void setClientSpecificConnectionTimeout(final Map<String, Integer> clientSpecificTimeouts) {
    if (clientSpecificTimeouts != null) {
      this.clientTimeouts.putAll(clientSpecificTimeouts);
    }
  }

  /**
   * @return info on all connected users
   */
  Collection<User> getUsers() {
    final Set<User> users = new HashSet<>();
    for (final ClientInfo clientInfo : getConnections().keySet()) {
      users.add(clientInfo.getUser());
    }

    return users;
  }

  /**
   * @return info on all connected clients
   */
  Collection<ClientInfo> getClients() {
    return new ArrayList<>(getConnections().keySet());
  }

  /**
   * @param user the user
   * @return all clients connected with the given user
   */
  Collection<ClientInfo> getClients(final User user) {
    final Collection<ClientInfo> clients = new ArrayList<>();
    for (final ClientInfo clientInfo : getConnections().keySet()) {
      if (user == null || clientInfo.getUser().equals(user)) {
        clients.add(clientInfo);
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
    //using the clientInfo from the connection since it contains the correct database user
    for (final AbstractRemoteEntityConnection connection : getConnections().values()) {
      if (connection.getClientInfo().getClientTypeID().equals(clientTypeID)) {
        clients.add(connection.getClientInfo());
      }
    }

    return clients;
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
   * Returns the server log for the connection identified by the given key.
   * @param clientID the UUID identifying the client
   * @return the server log for the given connection
   */
  final ClientLog getClientLog(final UUID clientID) {
    final AbstractRemoteEntityConnection connection = getConnection(clientID);
    if (connection != null) {
      return connection.getClientLog();
    }

    return null;
  }

  /**
   * @param clientID the client ID
   * @return true if logging is enabled for the given client
   */
  final boolean isLoggingEnabled(final UUID clientID) {
    final AbstractRemoteEntityConnection connection = getConnection(clientID);
    if (connection != null) {
      return connection.isLoggingEnabled();
    }

    return false;
  }

  /**
   * @param clientID the client ID
   * @param status the new logging status
   */
  final void setLoggingEnabled(final UUID clientID, final boolean status) {
    final AbstractRemoteEntityConnection connection = getConnection(clientID);
    if (connection != null) {
      connection.setLoggingEnabled(status);
    }
  }

  /**
   * @return the port of the registry this server is using
   */
  final int getRegistryPort() {
    return registryPort;
  }

  /**
   * @return true if connections to this server are ssl enabled
   */
  final boolean isSslEnabled() {
    return sslEnabled;
  }

  /**
   * Validates and keeps alive local connections and disconnects clients that have exceeded the idle timeout
   * @throws RemoteException in case of an exception
   */
  final void maintainConnections() throws RemoteException {
    final List<ClientInfo> clients = new ArrayList<>(getConnections().keySet());
    for (final ClientInfo client : clients) {
      final AbstractRemoteEntityConnection connection = getConnection(client.getClientID());
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
   * @see #hasConnectionTimedOut(String, AbstractRemoteEntityConnection)
   */
  final void removeConnections(final boolean timedOutOnly) throws RemoteException {
    final List<ClientInfo> clients = new ArrayList<>(getConnections().keySet());
    for (final ClientInfo client : clients) {
      final AbstractRemoteEntityConnection connection = getConnection(client.getClientID());
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
   * @return a map containing all defined entityIDs, with their respective table names as an associated value
   */
  static Map<String, String> getEntityDefinitions() {
    return Entities.getDefinitions();
  }

  /** {@inheritDoc} */
  @Override
  protected final void handleShutdown() throws RemoteException {
    super.handleShutdown();
    connectionMaintenanceScheduler.stop();
    removeConnections(false);
    ConnectionPools.closeConnectionPools();
    try {
      stopWebServer();
    }
    catch (final Exception e) {
      LOG.error("Error when stopping web server", e);
    }
    if (database.isEmbedded()) {
      database.shutdownEmbedded(null);
    }//todo does not work when shutdown requires user authentication, jminor.db.shutdownUser hmmm
    UnicastRemoteObject.unexportObject(serverAdmin, true);
  }

  private void disconnectQuietly(final AbstractRemoteEntityConnection connection) {
    try {
      disconnect(connection.getClientInfo().getClientID());
    }
    catch (final RemoteException ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

  /**
   * Binds this server instance to the registry
   * @throws RemoteException in case of an exception
   */
  private void bindToRegistry() throws RemoteException {
    ServerUtil.initializeRegistry(registryPort).rebind(getServerInfo().getServerName(), this);
    final String connectInfo = getServerInfo().getServerName() + " bound to registry on port: " + registryPort;
    LOG.info(connectInfo);
    System.out.println(connectInfo);
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

  protected static String initializeServerName(final String databaseHost, final String sid) {
    return Server.SERVER_NAME_PREFIX.get() + " " + Version.getVersionString()
            + "@" + (sid != null ? sid.toUpperCase() : databaseHost.toUpperCase());
  }

  protected static Collection<User> getPoolUsers(final Collection<String> poolUsers) {
    final Collection<User> users = new ArrayList<>();
    for (final String usernamePassword : poolUsers) {
      users.add(User.parseUser(usernamePassword));
    }

    return users;
  }

  protected static Map<String, Integer> getClientTimeoutValues() {
    final Collection<String> values = TextUtil.parseCommaSeparatedValues(SERVER_CLIENT_CONNECTION_TIMEOUT.get());

    return getClientTimeouts(values);
  }

  private static Map<String, Integer> getClientTimeouts(final Collection<String> values) {
    final Map<String, Integer> timeoutMap = new HashMap<>();
    for (final String clientTimeout : values) {
      final String[] split = clientTimeout.split(":");
      if (split.length < 2) {
        throw new IllegalArgumentException("Expecting a ':' delimiter");
      }
      timeoutMap.put(split[0], Integer.parseInt(split[1]));
    }

    return timeoutMap;
  }

  private AuxiliaryServer startWebServer(final String webDocumentRoot, final Integer webServerPort)
          throws ExecutionException, InterruptedException, ClassNotFoundException, NoSuchMethodException,
          IllegalAccessException, InvocationTargetException, InstantiationException {
    final String webServerClassName = WEB_SERVER_IMPLEMENTATION_CLASS.get();
    if (Util.nullOrEmpty(webDocumentRoot) || Util.nullOrEmpty(webServerClassName)) {
      return null;
    }

    final AuxiliaryServer auxiliaryServer = (AuxiliaryServer) Class.forName(webServerClassName).getConstructor(
            Server.class, String.class, Integer.class).newInstance(this, webDocumentRoot, webServerPort);
    Executors.newSingleThreadExecutor().submit((Callable) () -> {
      LOG.info("Starting web server on port: {}, document root: {}", webServerPort, webDocumentRoot);
      try {
        auxiliaryServer.startServer();

        return null;
      }
      catch (final Exception e) {
        LOG.error("Trying to start web server", e);
        throw e;
      }
    }).get();

    return auxiliaryServer;
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

  private boolean hasConnectionTimedOut(final String clientTypeID, final AbstractRemoteEntityConnection connection) {
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
      final String connectionPoolProviderClassName = SERVER_CONNECTION_POOL_PROVIDER_CLASS.get();
      final Class<? extends ConnectionPoolProvider> providerClass;
      if (Util.nullOrEmpty(connectionPoolProviderClassName)) {
        providerClass = null;
      }
      else {
        providerClass = (Class<? extends ConnectionPoolProvider>) Class.forName(connectionPoolProviderClassName);
      }
      ConnectionPools.initializeConnectionPools(providerClass, database, initialPoolUsers,
              EntityConnection.CONNECTION_VALIDITY_CHECK_TIMEOUT.get());
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

  /**
   * Starts the server
   * @return the server instance
   * @throws RemoteException in case of an exception
   */
  public static synchronized DefaultEntityConnectionServer startServer() throws RemoteException {
    final Integer serverPort = Server.SERVER_PORT.get();
    if (serverPort == null) {
      throw new IllegalArgumentException("Configuration property '" + Server.SERVER_PORT + "' is required");
    }
    final Integer registryPort = Server.REGISTRY_PORT.get();
    final Integer serverAdminPort = Server.SERVER_ADMIN_PORT.get();
    final boolean sslEnabled = Server.SERVER_CONNECTION_SSL_ENABLED.get();
    final Integer connectionLimit = SERVER_CONNECTION_LIMIT.get();
    final Database database = Databases.getInstance();
    final String serverName = initializeServerName(database.getHost(), database.getSid());

    final Collection<String> domainModelClassNames = TextUtil.parseCommaSeparatedValues(SERVER_DOMAIN_MODEL_CLASSES.get());
    final Collection<String> loginProxyClassNames = TextUtil.parseCommaSeparatedValues(SERVER_LOGIN_PROXY_CLASSES.get());
    final Collection<String> connectionValidationClassNames = TextUtil.parseCommaSeparatedValues(SERVER_CONNECTION_VALIDATOR_CLASSES.get());
    final Collection<String> initialPoolUsers = TextUtil.parseCommaSeparatedValues(SERVER_CONNECTION_POOLING_INITIAL.get());
    final String webDocumentRoot = WEB_SERVER_DOCUMENT_ROOT.get();
    final Integer webServerPort = WEB_SERVER_PORT.get();
    final boolean clientLoggingEnabled = SERVER_CLIENT_LOGGING_ENABLED.get();
    final Integer connectionTimeout = Server.SERVER_CONNECTION_TIMEOUT.get();
    final Map<String, Integer> clientTimeouts = getClientTimeoutValues();
    final String adminUserString = Server.SERVER_ADMIN_USER.get();
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

      return server;
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
  static synchronized void shutdownServer() throws ServerException.AuthenticationException {
    final int registryPort = Server.REGISTRY_PORT.get();
    final String sid = System.getProperty(Database.DATABASE_SID);
    final String host = System.getProperty(Database.DATABASE_HOST);
    final String serverName = initializeServerName(host, sid);
    final String adminUserString = Server.SERVER_ADMIN_USER.get();
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
   * @throws ServerException.AuthenticationException in case of missing or incorrect admin user information
   */
  public static void main(final String[] arguments) throws RemoteException, ServerException.AuthenticationException {
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