/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.Configuration;
import org.jminor.common.DaemonThreadFactory;
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
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.ConnectionValidator;
import org.jminor.common.server.LoginProxy;
import org.jminor.common.server.RemoteClient;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.Servers;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.domain.Entities;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A remote server class, responsible for handling requests for AbstractRemoteEntityConnections.
 */
public class DefaultEntityConnectionServer extends AbstractServer<AbstractRemoteEntityConnection, EntityConnectionServerAdmin> {

  private static final int DEFAULT_SERVER_CONNECTION_LIMIT = -1;

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
   * The initial connection logging status on the server, either true (on) or false (off)<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final Value<Boolean> SERVER_CLIENT_LOGGING_ENABLED = Configuration.booleanValue("jminor.server.clientLoggingEnabled", false);

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

  private final Database database;
  private final TaskScheduler connectionMaintenanceScheduler = new TaskScheduler(new DefaultEntityConnectionServer.MaintenanceTask(),
          DEFAULT_MAINTENANCE_INTERVAL_MS, DEFAULT_MAINTENANCE_INTERVAL_MS, TimeUnit.MILLISECONDS).start();
  private final int registryPort;
  private final Registry registry;
  private final boolean sslEnabled;
  private final boolean clientLoggingEnabled;
  private final Map<String, Integer> clientTimeouts = new HashMap<>();
  private final Thread shutdownHook;
  private final Collection<AuxiliaryServer> auxiliaryServers = new LinkedList<>();

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
   * @param auxiliaryServerClassNames the class names of auxiliary servers to run alongside this server
   * @param clientLoggingEnabled if true then client logging is enabled on startup
   * @param connectionTimeout the idle connection timeout
   * @param clientSpecificConnectionTimeouts client specific connection timeouts, mapped to clientTypeId
   * @param adminUser the admin user
   * @throws RemoteException in case of a remote exception
   * @throws RuntimeException in case the domain model classes are not found on the classpath or if the
   * jdbc driver class is not found or in case of an exception while constructing the initial pooled connections
   */
  public DefaultEntityConnectionServer(final String serverName, final int serverPort, final int serverAdminPort,
                                       final int registryPort, final Database database, final boolean sslEnabled,
                                       final int connectionLimit, final Collection<String> domainModelClassNames,
                                       final Collection<String> loginProxyClassNames,
                                       final Collection<String> connectionValidatorClassNames,
                                       final Collection<User> initialPoolUsers,
                                       final Collection<String> auxiliaryServerClassNames,
                                       final boolean clientLoggingEnabled, final int connectionTimeout,
                                       final Map<String, Integer> clientSpecificConnectionTimeouts,
                                       final User adminUser)
          throws RemoteException {
    super(serverPort, serverName, sslEnabled ? new SslRMIClientSocketFactory() : null,
            sslEnabled ? new SslRMIServerSocketFactory() : null);
    try {
      this.shutdownHook = new Thread(getShutdownHook());
      Runtime.getRuntime().addShutdownHook(this.shutdownHook);
      this.database = Objects.requireNonNull(database, "database");
      this.registryPort = registryPort;
      this.registry = LocateRegistry.createRegistry(registryPort);
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
      startAuxiliaryServers(auxiliaryServerClassNames);
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
  public final EntityConnectionServerAdmin getServerAdmin(final User user) throws ServerException.AuthenticationException {
    validateUserCredentials(user, adminUser);

    return serverAdmin;
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  protected final AbstractRemoteEntityConnection doConnect(final RemoteClient remoteClient)
          throws RemoteException, ServerException.LoginException, ServerException.ServerFullException {
    try {
      final ConnectionPool connectionPool = ConnectionPools.getConnectionPool(remoteClient.getDatabaseUser());
      if (connectionPool != null) {
        checkConnectionPoolCredentials(connectionPool.getUser(), remoteClient.getDatabaseUser());
      }

      final AbstractRemoteEntityConnection connection = createRemoteConnection(connectionPool, getDatabase(), remoteClient,
              getServerInfo().getServerPort(), isClientLoggingEnabled(), isSslEnabled() ? new SslRMIClientSocketFactory() : null,
              isSslEnabled() ? new SslRMIServerSocketFactory() : null);

      connection.addDisconnectListener(this::disconnectQuietly);
      LOG.debug("{} connected", remoteClient);

      return connection;
    }
    catch (final RemoteException e) {
      throw e;
    }
    catch (final AuthenticationException ae) {
      throw ServerException.authenticationException(ae.getMessage());
    }
    catch (final Exception e) {
      LOG.debug(remoteClient + " unable to connect", e);
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
   * @param database the underlying database
   * @param remoteClient the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param clientLoggingEnabled specifies whether or not method logging is enabled
   * @param clientSocketFactory the client socket factory, null for default
   * @param serverSocketFactory the server socket factory, null for default
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   * @return a remote connection
   */
  protected AbstractRemoteEntityConnection createRemoteConnection(final ConnectionPool connectionPool, final Database database,
                                                                  final RemoteClient remoteClient, final int port,
                                                                  final boolean clientLoggingEnabled,
                                                                  final RMIClientSocketFactory clientSocketFactory,
                                                                  final RMIServerSocketFactory serverSocketFactory)
          throws RemoteException, DatabaseException {
    final String domainId = (String) remoteClient.getParameters().get(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID);
    if (domainId == null) {
      throw new IllegalArgumentException("'" + RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID + "' parameter not specified");
    }
    final Entities domainModel = Entities.getDomainEntities(domainId);
    if (connectionPool != null) {
      return new DefaultRemoteEntityConnection(domainModel, connectionPool, remoteClient, port, clientLoggingEnabled,
              clientSocketFactory, serverSocketFactory);
    }

    return new DefaultRemoteEntityConnection(domainModel, database, remoteClient, port, clientLoggingEnabled,
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
   * @param clientSpecificTimeouts the timeout values mapped to each clientTypeId
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
    for (final RemoteClient remoteClient : getConnections().keySet()) {
      users.add(remoteClient.getUser());
    }

    return users;
  }

  /**
   * @return info on all connected clients
   */
  Collection<RemoteClient> getClients() {
    return new ArrayList<>(getConnections().keySet());
  }

  /**
   * @param user the user
   * @return all clients connected with the given user
   */
  Collection<RemoteClient> getClients(final User user) {
    final Collection<RemoteClient> clients = new ArrayList<>();
    for (final RemoteClient remoteClient : getConnections().keySet()) {
      if (user == null || remoteClient.getUser().equals(user)) {
        clients.add(remoteClient);
      }
    }

    return clients;
  }

  /**
   * @param clientTypeId the client type ID
   * @return all clients of the given type
   */
  Collection<RemoteClient> getClients(final String clientTypeId) {
    final Collection<RemoteClient> clients = new ArrayList<>();
    //using the remoteClient from the connection since it contains the correct database user
    for (final AbstractRemoteEntityConnection connection : getConnections().values()) {
      if (connection.getRemoteClient().getClientTypeId().equals(clientTypeId)) {
        clients.add(connection.getRemoteClient());
      }
    }

    return clients;
  }

  /**
   * @return a map containing all defined entityIds, with their respective table names as an associated value
   */
  Map<String, String> getEntityDefinitions() {
    final Map<String, String> definitions = new HashMap<>();
    for (final Entities entities : Entities.getAllDomains()) {
      for (final String entityId : entities.getDefinedEntities()) {
        definitions.put(entityId, entities.getTableName(entityId));
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
   * Returns the server log for the connection identified by the given key.
   * @param clientId the UUID identifying the client
   * @return the server log for the given connection
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
   * @param status the new logging status
   */
  final void setLoggingEnabled(final UUID clientId, final boolean status) {
    final AbstractRemoteEntityConnection connection = getConnection(clientId);
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
    final List<RemoteClient> clients = new ArrayList<>(getConnections().keySet());
    for (final RemoteClient client : clients) {
      final AbstractRemoteEntityConnection connection = getConnection(client.getClientId());
      if (!connection.isActive()) {
        final boolean valid = connection.isConnected();
        final boolean timedOut = hasConnectionTimedOut(client.getClientTypeId(), connection);
        if (!valid || timedOut) {
          LOG.debug("Removing connection {}, valid: {}, timeout: {}", new Object[] {client, valid, timedOut});
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
  final void removeConnections(final boolean timedOutOnly) throws RemoteException {
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

  /** {@inheritDoc} */
  @Override
  protected final void handleShutdown() throws RemoteException {
    super.handleShutdown();
    connectionMaintenanceScheduler.stop();
    removeConnections(false);
    ConnectionPools.closeConnectionPools();
    auxiliaryServers.forEach(DefaultEntityConnectionServer::stopAuxiliaryServer);
    if (database.isEmbedded()) {
      database.shutdownEmbedded(null);
    }//todo does not work when shutdown requires user authentication, jminor.db.shutdownUser hmmm
    try {
      UnicastRemoteObject.unexportObject(registry, true);
    }
    catch (final NoSuchObjectException ignored) {/*ignored*/}
    UnicastRemoteObject.unexportObject(serverAdmin, true);
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
   */
  private void bindToRegistry() throws RemoteException {
    registry.rebind(getServerInfo().getServerName(), this);
    final String connectInfo = getServerInfo().getServerName() + " bound to registry on port: " + registryPort;
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  private void startAuxiliaryServers(final Collection<String> auxiliaryServerClassNames) {
    if (auxiliaryServerClassNames != null) {
      try {
        for (final String className : auxiliaryServerClassNames) {
          final Class<AuxiliaryServer> serverClass = (Class<AuxiliaryServer>) Class.forName(className);
          final AuxiliaryServer server = serverClass.getDeclaredConstructor(Server.class).newInstance(this);
          auxiliaryServers.add(server);
          LOG.info("Server starting auxiliary server: " + serverClass);
          Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory()).submit((Callable) () ->
                  startAuxiliaryServer(server)).get();
        }
      }
      catch (final Exception e) {
        LOG.error("Instantiating auxiliary server", e);
        throw new RuntimeException(e);
      }
    }
  }

  private void loadLoginProxies(final Collection<String> loginProxyClassNames) throws ClassNotFoundException {
    if (loginProxyClassNames != null) {
      for (final String loginProxyClassName : loginProxyClassNames) {
        LOG.info("Server loading login proxy class '" + loginProxyClassName + FROM_CLASSPATH);
        final Class<?> loginProxyClass = Class.forName(loginProxyClassName);
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
        final Class<?> clientValidatorClass = Class.forName(connectionValidatorClassName);
        try {
          final ConnectionValidator validator = (ConnectionValidator) clientValidatorClass.getConstructor().newInstance();
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

  /**
   * Checks the credentials provided by {@code remoteClient} against the credentials
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

  private boolean hasConnectionTimedOut(final String clientTypeId, final AbstractRemoteEntityConnection connection) {
    Integer timeout = clientTimeouts.get(clientTypeId);
    if (timeout == null) {
      timeout = connectionTimeout;
    }

    return connection.hasBeenInactive(timeout);
  }

  private void loadDomainModels(final Collection<String> domainModelClassNames) throws Throwable {
    try {
      if (domainModelClassNames != null) {
        for (final String className : domainModelClassNames) {
          final String message = "Server loading and registering domain model class '" + className + FROM_CLASSPATH;
          LOG.info(message);
          final Entities entities = (Entities) Class.forName(className).getDeclaredConstructor().newInstance();
          entities.registerDomain();
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

  private static void initializeConnectionPools(final Database database, final Collection<User> initialPoolUsers)
          throws ClassNotFoundException, DatabaseException {
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
    final Integer serverPort = Objects.requireNonNull(Server.SERVER_PORT.get(), Server.SERVER_PORT.toString());
    final Integer registryPort = Objects.requireNonNull(Server.REGISTRY_PORT.get(), Server.REGISTRY_PORT.toString());
    final Integer serverAdminPort = Objects.requireNonNull(Server.SERVER_ADMIN_PORT.get(), Server.SERVER_ADMIN_PORT.toString());
    final boolean sslEnabled = Server.SERVER_CONNECTION_SSL_ENABLED.get();
    final Integer connectionLimit = SERVER_CONNECTION_LIMIT.get();
    final Database database = Databases.getInstance();
    final String serverName = initializeServerName(database.getHost(), database.getSid());

    final Collection<String> domainModelClassNames = TextUtil.parseCommaSeparatedValues(SERVER_DOMAIN_MODEL_CLASSES.get());
    final Collection<String> loginProxyClassNames = TextUtil.parseCommaSeparatedValues(SERVER_LOGIN_PROXY_CLASSES.get());
    final Collection<String> connectionValidationClassNames = TextUtil.parseCommaSeparatedValues(SERVER_CONNECTION_VALIDATOR_CLASSES.get());
    final Collection<String> initialPoolUsers = TextUtil.parseCommaSeparatedValues(SERVER_CONNECTION_POOLING_INITIAL.get());
    final Collection<String> auxiliaryServerClassNames = TextUtil.parseCommaSeparatedValues(AUXILIARY_SERVER_CLASS_NAMES.get());
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
              getPoolUsers(initialPoolUsers), auxiliaryServerClassNames, clientLoggingEnabled, connectionTimeout,
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
    final String sid = Database.DATABASE_SID.get();
    final String host = Database.DATABASE_HOST.get();
    final String serverName = initializeServerName(host, sid);
    final String adminUserString = Server.SERVER_ADMIN_USER.get();
    if (Util.nullOrEmpty(adminUserString)) {
      throw ServerException.authenticationException("No admin user specified");
    }
    final User adminUser = User.parseUser(adminUserString);
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