/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseUtil;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.Configuration;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
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
import java.util.Set;
import java.util.UUID;

/**
 * Implements the EntityConnectionServerAdmin interface, providing admin access to a EntityConnectionServer instance.
 */
public final class DefaultEntityConnectionServerAdmin extends UnicastRemoteObject implements EntityConnectionServerAdmin {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityConnectionServerAdmin.class);

  private static final long serialVersionUID = 1;

  private static final int USERNAME_PASSWORD_SPLIT_COUNT = 2;

  private static final String START = "start";
  private static final String STOP = "stop";
  private static final String SHUTDOWN = "shutdown";
  private static final String RESTART = "restart";

  static {
    Configuration.init();
  }

  /**
   * The server being administrated
   */
  private final EntityConnectionServer server;
  private final String serverName;
  private final Thread shutdownHook;

  /**
   * Instantiates a new DefaultEntityConnectionServerAdmin
   * @param server the server to administer
   * @param serverAdminPort the port on which to make the server admin available
   * @throws RemoteException in case of an exception
   */
  public DefaultEntityConnectionServerAdmin(final EntityConnectionServer server, final int serverAdminPort) throws RemoteException {
    super(serverAdminPort,
            server.isSslEnabled() ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            server.isSslEnabled() ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.server = server;
    this.serverName = server.getServerInfo().getServerName();
    this.shutdownHook = new Thread(getShutdownHook());
    Runtime.getRuntime().addShutdownHook(this.shutdownHook);
  }

  /**
   * Binds this admin instance to the registry
   * @throws RemoteException in case of an exception
   */
  public void bindToRegistry() throws RemoteException {
    final int registryPort = server.getRegistryPort();
    ServerUtil.initializeRegistry(registryPort);
    ServerUtil.getRegistry(registryPort).rebind(Configuration.SERVER_ADMIN_PREFIX + serverName, this);
  }

  /** {@inheritDoc} */
  @Override
  public Server.ServerInfo getServerInfo() {
    return server.getServerInfo();
  }

  /** {@inheritDoc} */
  @Override
  public String getSystemProperties() {
    return Util.getSystemProperties();
  }

  /** {@inheritDoc} */
  @Override
  public String getDatabaseURL() {
    return server.getDatabase().getURL(null);
  }

  /** {@inheritDoc} */
  @Override
  public Level getLoggingLevel() {
    final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    return rootLogger.getLevel();
  }

  /** {@inheritDoc} */
  @Override
  public void setLoggingLevel(final Level level) {
    LOG.info("setLoggingLevel({})", level);
    final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(level);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<User> getUsers() throws RemoteException {
    return server.getUsers();
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ClientInfo> getClients(final User user) throws RemoteException {
    return server.getClients(user);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ClientInfo> getClients(final String clientTypeID) {
    return server.getClients(clientTypeID);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ClientInfo> getClients() {
    return server.getClients();
  }

  /** {@inheritDoc} */
  @Override
  public Collection<String> getClientTypes() {
    final Set<String> clientTypes = new HashSet<>();
    for (final ClientInfo client : getClients()) {
      clientTypes.add(client.getClientTypeID());
    }

    return clientTypes;
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect(final UUID clientID) throws RemoteException {
    LOG.info("disconnect({})", clientID);
    server.disconnect(clientID);
  }

  /** {@inheritDoc} */
  @Override
  public void shutdown() throws RemoteException {
    try {
      ServerUtil.getRegistry(server.getRegistryPort()).unbind(serverName);
    }
    catch (final NotBoundException ignored) {/*ignored*/}
    try {
      ServerUtil.getRegistry(server.getRegistryPort()).unbind(Configuration.SERVER_ADMIN_PREFIX + serverName);
    }
    catch (final NotBoundException ignored) {/*ignored*/}

    final String shutdownInfo = serverName + " removed from registry";
    LOG.info(shutdownInfo);
    System.out.println(shutdownInfo);

    LOG.info("Shutting down server");
    server.shutdown();
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (final NoSuchObjectException ignored) {/*ignored*/}
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }
    catch (final IllegalStateException e) {/*Shutdown in progress*/}
  }

  /** {@inheritDoc} */
  @Override
  public void restart() throws RemoteException {
    shutdown();
    startServer();
  }

  /** {@inheritDoc} */
  @Override
  public int getActiveConnectionCount() {
    return DefaultRemoteEntityConnection.getActiveCount();
  }

  /** {@inheritDoc} */
  @Override
  public int getMaintenanceInterval() {
    return server.getMaintenanceInterval();
  }

  /** {@inheritDoc} */
  @Override
  public void setMaintenanceInterval(final int interval) {
    LOG.info("setMaintenanceInterval({})", interval);
    server.setMaintenanceInterval(interval);
  }

  /** {@inheritDoc} */
  @Override
  public void removeConnections(final boolean timedOutOnly) throws RemoteException {
    LOG.info("removeConnections({})", timedOutOnly);
    server.removeConnections(timedOutOnly);
  }

  /** {@inheritDoc} */
  @Override
  public void resetConnectionPoolStatistics(final User user) {
    LOG.info("resetConnectionPoolStatistics({})", user);
    ConnectionPools.getConnectionPool(user).resetStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCollectFineGrainedPoolStatistics(final User user) {
    return ConnectionPools.getConnectionPool(user).isCollectFineGrainedStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public void setCollectFineGrainedPoolStatistics(final User user, final boolean value) {
    LOG.info("setCollectFineGrainedPoolStatistics({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setCollectFineGrainedStatistics(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getRequestsPerSecond() {
    return DefaultRemoteEntityConnection.getRequestsPerSecond();
  }

  /** {@inheritDoc} */
  @Override
  public ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) {
    return ConnectionPools.getConnectionPool(user).getStatistics(since);
  }

  /** {@inheritDoc} */
  @Override
  public Database.Statistics getDatabaseStatistics() {
    return DatabaseUtil.getDatabaseStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public List<User> getConnectionPools() {
    final List<User> poolUsers = new ArrayList<>();
    for (final ConnectionPool pool : ConnectionPools.getConnectionPools()) {
      poolUsers.add(pool.getUser());
    }

    return poolUsers;
  }

  /** {@inheritDoc} */
  @Override
  public int getConnectionPoolCleanupInterval(final User user) {
    return ConnectionPools.getConnectionPool(user).getCleanupInterval();
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionPoolCleanupInterval(final User user, final int poolCleanupInterval) {
    LOG.info("setConnectionPoolCleanupInterval({}, {})", user, poolCleanupInterval);
    ConnectionPools.getConnectionPool(user).setCleanupInterval(poolCleanupInterval);
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumConnectionPoolSize(final User user) {
    return ConnectionPools.getConnectionPool(user).getMaximumPoolSize();
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumConnectionPoolSize(final User user, final int value) {
    LOG.info("setMaximumConnectionPoolSize({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setMaximumPoolSize(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getMinimumConnectionPoolSize(final User user) {
    return ConnectionPools.getConnectionPool(user).getMinimumPoolSize();
  }

  /** {@inheritDoc} */
  @Override
  public void setMinimumConnectionPoolSize(final User user, final int value) {
    LOG.info("setMinimumConnectionPoolSize({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setMinimumPoolSize(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getPoolConnectionThreshold(final User user) {
    return ConnectionPools.getConnectionPool(user).getNewConnectionThreshold();
  }

  /** {@inheritDoc} */
  @Override
  public void setPoolConnectionThreshold(final User user, final int value) {
    LOG.info("setPoolConnectionThreshold({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setNewConnectionThreshold(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getPooledConnectionTimeout(final User user) {
    return ConnectionPools.getConnectionPool(user).getConnectionTimeout();
  }

  /** {@inheritDoc} */
  @Override
  public void setPooledConnectionTimeout(final User user, final int timeout) {
    LOG.info("setPooledConnectionTimeout({}, {})", user, timeout);
    ConnectionPools.getConnectionPool(user).setConnectionTimeout(timeout);
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumPoolRetryWaitPeriod(final User user) {
    return ConnectionPools.getConnectionPool(user).getMaximumRetryWaitPeriod();
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumPoolRetryWaitPeriod(final User user, final int value) {
    LOG.info("setMaximumPoolRetryWaitPeriod({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setMaximumRetryWaitPeriod(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumPoolCheckOutTime(final User user) {
    return ConnectionPools.getConnectionPool(user).getMaximumCheckOutTime();
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumPoolCheckOutTime(final User user, final int value) {
    LOG.info("setMaximumPoolCheckOutTime({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setMaximumCheckOutTime(value);
  }

  /** {@inheritDoc} */
  @Override
  public String getMemoryUsage() {
    return Util.getMemoryUsageString();
  }

  /** {@inheritDoc} */
  @Override
  public long getAllocatedMemory() {
    return Util.getAllocatedMemory();
  }

  /** {@inheritDoc} */
  @Override
  public long getUsedMemory() {
    return Util.getUsedMemory();
  }

  /** {@inheritDoc} */
  @Override
  public long getMaxMemory() {
    return Util.getMaxMemory();
  }

  /** {@inheritDoc} */
  @Override
  public void performGC() {
    LOG.info("performGG()");
    Runtime.getRuntime().gc();
  }

  /** {@inheritDoc} */
  @Override
  public int getConnectionCount() {
    return server.getConnectionCount();
  }

  /** {@inheritDoc} */
  @Override
  public int getConnectionLimit() {
    return server.getConnectionLimit();
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionLimit(final int value) {
    LOG.info("setConnectionLimit", value);
    server.setConnectionLimit(value);
  }

  /** {@inheritDoc} */
  @Override
  public ClientLog getClientLog(final UUID clientID) {
    return server.getClientLog(clientID);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLoggingEnabled(final UUID clientID) {
    return server.isLoggingEnabled(clientID);
  }

  /** {@inheritDoc} */
  @Override
  public void setLoggingEnabled(final UUID clientID, final boolean status) {
    LOG.info("setLoggingEnabled({}, {})", clientID, status);
    server.setLoggingEnabled(clientID, status);
  }

  /** {@inheritDoc} */
  @Override
  public int getConnectionTimeout() {
    return server.getConnectionTimeout();
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionTimeout(final int timeout) {
    LOG.info("setConnectionTimeout({})", timeout);
    server.setConnectionTimeout(timeout);
  }

  /** {@inheritDoc} */
  @Override
  public Map<String,String> getEntityDefinitions() {
    return EntityConnectionServer.getEntityDefinitions();
  }

  /**
   * @return the server instance being administered
   */
  EntityConnectionServer getServer() {
    return server;
  }

  private Runnable getShutdownHook() {
    return new Runnable() {
      @Override
      public void run() {
        if (server.isShuttingDown()) {
          return;
        }
        try {
          shutdown();
        }
        catch (final RemoteException e) {
          LOG.error("Exception during shutdown", e);
        }
      }
    };
  }

  private static String initializeServerName(final String databaseHost, final String sid) {
    return Configuration.getStringValue(Configuration.SERVER_NAME_PREFIX) + " " + Util.getVersionString()
            + "@" + (sid != null ? sid.toUpperCase() : databaseHost.toUpperCase());
  }

  public static synchronized DefaultEntityConnectionServerAdmin startServer() throws RemoteException {
    final Integer serverPort = (Integer) Configuration.getValue(Configuration.SERVER_PORT);
    if (serverPort == null) {
      throw new IllegalArgumentException("Configuration property '" + Configuration.SERVER_PORT + "' is required");
    }
    final int registryPort = Configuration.getIntValue(Configuration.REGISTRY_PORT);
    final int serverAdminPort = Configuration.getIntValue(Configuration.SERVER_ADMIN_PORT);
    final boolean sslEnabled = Configuration.getBooleanValue(Configuration.SERVER_CONNECTION_SSL_ENABLED);
    final int connectionLimit = Configuration.getIntValue(Configuration.SERVER_CONNECTION_LIMIT);
    final Database database = Databases.createInstance();
    final String serverName = initializeServerName(database.getHost(), database.getSid());

    final Collection<String> domainModelClassNames = Configuration.parseCommaSeparatedValues(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    final Collection<String> loginProxyClassNames = Configuration.parseCommaSeparatedValues(Configuration.SERVER_LOGIN_PROXY_CLASSES);
    final Collection<String> initialPoolUsers = Configuration.parseCommaSeparatedValues(Configuration.SERVER_CONNECTION_POOLING_INITIAL);
    final String webDocumentRoot = Configuration.getStringValue(Configuration.WEB_SERVER_DOCUMENT_ROOT);
    final Integer webServerPort = Configuration.getIntValue(Configuration.WEB_SERVER_PORT);
    final boolean clientLoggingEnabled = Configuration.getBooleanValue(Configuration.SERVER_CLIENT_LOGGING_ENABLED);
    final int connectionTimeout = Configuration.getIntValue(Configuration.SERVER_CONNECTION_TIMEOUT);
    final Map<String, Integer> clientTimeouts = getClientTimeoutValues();
    final EntityConnectionServer server = new EntityConnectionServer(serverName, serverPort, registryPort, database,
            sslEnabled, connectionLimit, domainModelClassNames, loginProxyClassNames, getPoolUsers(initialPoolUsers),
            webDocumentRoot, webServerPort, clientLoggingEnabled, connectionTimeout, clientTimeouts);
    final DefaultEntityConnectionServerAdmin admin = new DefaultEntityConnectionServerAdmin(server, serverAdminPort);
    try {
      server.bindToRegistry();
      admin.bindToRegistry();

      return admin;
    }
    catch (final Exception e) {
      LOG.error("Exception on binding server to registry", e);
      admin.shutdown();
      throw new RuntimeException(e);
    }
  }

  /**
   * Connects to the server and shuts it down
   */
  static synchronized void shutdownServer() {
    final int registryPort = Configuration.getIntValue(Configuration.REGISTRY_PORT);
    final String sid = System.getProperty(Database.DATABASE_SID);
    final String host = System.getProperty(Database.DATABASE_HOST);
    final String serverName = Configuration.SERVER_ADMIN_PREFIX + initializeServerName(host, sid);
    Util.resolveTrustStoreFromClasspath(DefaultEntityConnectionServerAdmin.class.getSimpleName());
    try {
      final Registry registry = ServerUtil.getRegistry(registryPort);
      final EntityConnectionServerAdmin serverAdmin = (EntityConnectionServerAdmin) registry.lookup(serverName);
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
  }

  private static Collection<User> getPoolUsers(final Collection<String> poolUsers) {
    final Collection<User> users = new ArrayList<>();
    for (final String usernamePassword : poolUsers) {
      final String[] split = splitString(usernamePassword);
      users.add(new User(split[0], split[1]));
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
      final String[] split = splitString(clientTimeout);
      timeoutMap.put(split[0], Integer.parseInt(split[1]));
    }

    return timeoutMap;
  }

  private static String[] splitString(final String usernamePassword) {
    final String[] splitResult = usernamePassword.split(":");
    if (splitResult.length < USERNAME_PASSWORD_SPLIT_COUNT) {
      throw new IllegalArgumentException("Expecting a ':' delimiter");
    }

    return splitResult;
  }

  /**
   * If no arguments are supplied a new EntityConnectionServer with a server admin interface is started.
   * @param arguments 'start' (or no argument) starts the server, 'stop' or 'shutdown' causes a running server to be shut down and 'restart' restarts the server
   * @throws RemoteException in case of a remote exception during service export
   * @throws ClassNotFoundException in case the domain model classes required for the server is not found or
   * if the jdbc driver class is not found
   * @throws DatabaseException in case of an exception while constructing the initial pooled connections
   */
  public static void main(final String[] arguments) throws RemoteException, ClassNotFoundException, DatabaseException {
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
