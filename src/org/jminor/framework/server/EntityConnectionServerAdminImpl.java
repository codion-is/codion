/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.RemoteServer;
import org.jminor.common.server.ServerLog;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.Configuration;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implements the EntityConnectionServerAdmin interface, providing admin access to a EntityConnectionServer instance.
 */
public final class EntityConnectionServerAdminImpl extends UnicastRemoteObject implements EntityConnectionServerAdmin {

  private static final Logger LOG = LoggerFactory.getLogger(EntityConnectionServerAdminImpl.class);

  private static final long serialVersionUID = 1;

  static {
    Configuration.init();
    System.setSecurityManager(new RMISecurityManager());
  }

  /**
   * The server being administrated
   */
  private final EntityConnectionServer server;

  /**
   * Instantiates a new EntityConnectionServerAdminImpl
   * @param server the server to administer
   * @param serverAdminPort the port on which to make the server admin available
   * @throws RemoteException in case of an exception
   */
  public EntityConnectionServerAdminImpl(final EntityConnectionServer server, final int serverAdminPort) throws RemoteException {
    super(serverAdminPort,
            server.isSslEnabled() ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            server.isSslEnabled() ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.server = server;
    ServerUtil.getRegistry(server.getRegistryPort()).rebind(RemoteServer.SERVER_ADMIN_PREFIX + server.getServerName(), this);
    Runtime.getRuntime().addShutdownHook(new Thread(getShutdownHook()));
  }

  /** {@inheritDoc} */
  @Override
  public String getServerName() {
    return server.getServerName();
  }

  /** {@inheritDoc} */
  @Override
  public String getServerVersion() {
    return server.getServerVersion();
  }

  /** {@inheritDoc} */
  @Override
  public int getServerPort() throws RemoteException {
    return server.getServerPort();
  }

  /** {@inheritDoc} */
  @Override
  public String getSystemProperties() {
    return Util.getSystemProperties();
  }

  /** {@inheritDoc} */
  @Override
  public long getStartDate() {
    return server.getStartDate();
  }

  /** {@inheritDoc} */
  @Override
  public String getDatabaseURL() {
    return server.getDatabase().getURL(null);
  }

  /** {@inheritDoc} */
  @Override
  public Level getLoggingLevel() throws RemoteException {
    final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    return rootLogger.getLevel();
  }

  /** {@inheritDoc} */
  @Override
  public void setLoggingLevel(final Level level) throws RemoteException {
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
  public Collection<ClientInfo> getClients(final String clientTypeID) throws RemoteException {
    return server.getClients(clientTypeID);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ClientInfo> getClients() throws RemoteException {
    return server.getClients();
  }

  /** {@inheritDoc} */
  @Override
  public Collection<String> getClientTypes() throws RemoteException {
    final Set<String> clientTypes = new HashSet<String>();
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
      ServerUtil.getRegistry(server.getRegistryPort()).unbind(server.getServerName());
    }
    catch (NotBoundException ignored) {}
    try {
      ServerUtil.getRegistry(server.getRegistryPort()).unbind(RemoteServer.SERVER_ADMIN_PREFIX + server.getServerName());
    }
    catch (NotBoundException ignored) {}

    final String shutdownInfo = server.getServerName() + " removed from registry";
    LOG.info(shutdownInfo);
    System.out.println(shutdownInfo);

    LOG.info("Shutting down server");
    server.shutdown();
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (NoSuchObjectException ignored) {}
  }

  /** {@inheritDoc} */
  @Override
  public int getActiveConnectionCount() throws RemoteException {
    return RemoteEntityConnectionImpl.getActiveCount();
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
  public void removeConnections(final boolean inactiveOnly) throws RemoteException {
    LOG.info("removeConnections({})", inactiveOnly);
    server.removeConnections(inactiveOnly);
  }

  /** {@inheritDoc} */
  @Override
  public void resetConnectionPoolStatistics(final User user) throws RemoteException {
    LOG.info("resetConnectionPoolStatistics({})", user);
    RemoteEntityConnectionImpl.resetPoolStatistics(user);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCollectFineGrainedPoolStatistics(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.isCollectFineGrainedPoolStatistics(user);
  }

  /** {@inheritDoc} */
  @Override
  public void setCollectFineGrainedPoolStatistics(final User user, final boolean value) throws RemoteException {
    LOG.info("setCollectFineGrainedPoolStatistics({}, {})", user, value);
    RemoteEntityConnectionImpl.setCollectFineGrainedPoolStatistics(user, value);
  }

  /** {@inheritDoc} */
  @Override
  public int getRequestsPerSecond() throws RemoteException {
    return RemoteEntityConnectionImpl.getRequestsPerSecond();
  }

  /** {@inheritDoc} */
  @Override
  public int getWarningTimeThreshold() throws RemoteException {
    return RemoteEntityConnectionImpl.getWarningThreshold();
  }

  /** {@inheritDoc} */
  @Override
  public void setWarningTimeThreshold(final int threshold) throws RemoteException {
    LOG.info("setWarningThreshold({})", threshold);
    RemoteEntityConnectionImpl.setWarningThreshold(threshold);
  }

  /** {@inheritDoc} */
  @Override
  public int getWarningTimeExceededPerSecond() throws RemoteException {
    return RemoteEntityConnectionImpl.getWarningTimeExceededPerSecond();
  }

  /** {@inheritDoc} */
  @Override
  public ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) throws RemoteException {
    return RemoteEntityConnectionImpl.getPoolStatistics(user, since);
  }

  /** {@inheritDoc} */
  @Override
  public Database.Statistics getDatabaseStatistics() throws RemoteException {
    return Databases.getDatabaseStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public List<User> getEnabledConnectionPools() throws RemoteException {
    return RemoteEntityConnectionImpl.getEnabledConnectionPoolSettings();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConnectionPoolEnabled(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.isPoolEnabled(user);
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionPoolCleanupInterval(final User user, final int poolCleanupInterval) throws RemoteException {
    LOG.info("setConnectionPoolCleanupInterval({}, {})", user, poolCleanupInterval);
    RemoteEntityConnectionImpl.setPoolCleanupInterval(user, poolCleanupInterval);
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionPoolEnabled(final User user, final boolean enabled) throws RemoteException {
    LOG.info("setConnectionPoolEnabled({}, {})", user, enabled);
    RemoteEntityConnectionImpl.setPoolEnabled(user, enabled);
  }

  /** {@inheritDoc} */
  @Override
  public int getConnectionPoolCleanupInterval(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getPoolCleanupInterval(user);
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumConnectionPoolSize(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getMaximumPoolSize(user);
  }

  /** {@inheritDoc} */
  @Override
  public int getMinimumConnectionPoolSize(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getMinimumPoolSize(user);
  }

  /** {@inheritDoc} */
  @Override
  public int getPooledConnectionTimeout(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getPoolConnectionTimeout(user);
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumConnectionPoolSize(final User user, final int value) throws RemoteException {
    LOG.info("setMaximumConnectionPoolSize({}, {})", user, value);
    RemoteEntityConnectionImpl.setMaximumPoolSize(user, value);
  }

  /** {@inheritDoc} */
  @Override
  public void setMinimumConnectionPoolSize(final User user, final int value) throws RemoteException {
    LOG.info("setMinimumConnectionPoolSize({}, {})", user, value);
    RemoteEntityConnectionImpl.setMinimumPoolSize(user, value);
  }

  /** {@inheritDoc} */
  @Override
  public int getPoolConnectionThreshold(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getPoolConnectionThreshold(user);
  }

  /** {@inheritDoc} */
  @Override
  public void setPoolConnectionThreshold(final User user, final int value) throws RemoteException {
    LOG.info("setPoolConnectionThreshold({}, {})", user, value);
    RemoteEntityConnectionImpl.setPoolConnectionThreshold(user, value);
  }

  /** {@inheritDoc} */
  @Override
  public void setPooledConnectionTimeout(final User user, final int timeout) throws RemoteException {
    LOG.info("setPooledConnectionTimeout({}, {})", user, timeout);
    RemoteEntityConnectionImpl.setPoolConnectionTimeout(user, timeout);
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumPoolRetryWaitPeriod(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getMaximumPoolRetryWaitPeriod(user);
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumPoolRetryWaitPeriod(final User user, final int value) throws RemoteException {
    LOG.info("setMaximumPoolRetryWaitPeriod({}, {})", user, value);
    RemoteEntityConnectionImpl.setMaximumPoolRetryWaitPeriod(user, value);
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumPoolCheckOutTime(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getMaximumPoolCheckOutTime(user);
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumPoolCheckOutTime(final User user, final int value) throws RemoteException {
    LOG.info("setMaximumPoolCheckOutTime({}, {})", user, value);
    RemoteEntityConnectionImpl.setMaximumPoolCheckOutTime(user, value);
  }

  /** {@inheritDoc} */
  @Override
  public String getMemoryUsage() throws RemoteException {
    return Util.getMemoryUsageString();
  }

  /** {@inheritDoc} */
  @Override
  public long getAllocatedMemory() throws RemoteException {
    return Util.getAllocatedMemory();
  }

  /** {@inheritDoc} */
  @Override
  public long getUsedMemory() throws RemoteException {
    return Util.getUsedMemory();
  }

  /** {@inheritDoc} */
  @Override
  public long getMaxMemory() throws RemoteException {
    return Util.getMaxMemory();
  }

  /** {@inheritDoc} */
  @Override
  public void performGC() throws RemoteException {
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
  public int getConnectionLimit() throws RemoteException {
    return server.getConnectionLimit();
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionLimit(final int value) throws RemoteException {
    LOG.info("setConnectionLimit", value);
    server.setConnectionLimit(value);
  }

  /** {@inheritDoc} */
  @Override
  public ServerLog getServerLog(final UUID clientID) {
    return server.getServerLog(clientID);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLoggingEnabled(final UUID clientID) throws RemoteException {
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
  public int getConnectionTimeout() throws RemoteException {
    return server.getConnectionTimeout();
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionTimeout(final int timeout) throws RemoteException {
    LOG.info("setConnectionTimeout({})", timeout);
    server.setConnectionTimeout(timeout);
  }

  /** {@inheritDoc} */
  @Override
  public Map<String,String> getEntityDefinitions() throws RemoteException {
    return EntityConnectionServer.getEntityDefinitions();
  }

  private Runnable getShutdownHook() {
    return new Runnable() {
      /** {@inheritDoc} */
      @Override
      public void run() {
        if (server.isShuttingDown()) {
          return;
        }
        try {
          shutdown();
        }
        catch (RemoteException e) {
          LOG.error("Exception during shutdown", e);
        }
      }
    };
  }

  private static String initializeServerName(final String databaseHost, final String sid) {
    return Configuration.getStringValue(Configuration.SERVER_NAME_PREFIX) + " " + Util.getVersion()
            + "@" + (sid != null ? sid.toUpperCase() : databaseHost.toUpperCase());
  }

  private static void startServer() throws RemoteException, ClassNotFoundException, DatabaseException {
    final Integer serverPort = (Integer) Configuration.getValue(Configuration.SERVER_PORT);
    if (serverPort == null) {
      throw new IllegalArgumentException("Configuration property '" + Configuration.SERVER_PORT + "' is required");
    }
    final int registryPort = Configuration.getIntValue(Configuration.REGISTRY_PORT_NUMBER);
    final int serverAdminPort = Configuration.getIntValue(Configuration.SERVER_ADMIN_PORT);
    final boolean sslEnabled = Configuration.getBooleanValue(Configuration.SERVER_CONNECTION_SSL_ENABLED);
    final int connectionLimit = Configuration.getIntValue(Configuration.SERVER_CONNECTION_LIMIT);
    final Database database = Databases.createInstance();
    final String serverName = initializeServerName(database.getHost(), database.getSid());
    final EntityConnectionServer server = new EntityConnectionServer(serverName, serverPort, registryPort, database,
            sslEnabled, connectionLimit);
    new EntityConnectionServerAdminImpl(server, serverAdminPort);
  }

  /**
   * Connects to the server and shuts it down
   */
  private static void shutdownServer() {
    final int registryPort = Configuration.getIntValue(Configuration.REGISTRY_PORT_NUMBER);
    final String sid = System.getProperty(Database.DATABASE_SID);
    final String host = System.getProperty(Database.DATABASE_HOST);
    final String serverName = RemoteServer.SERVER_ADMIN_PREFIX + initializeServerName(host, sid);
    Configuration.resolveTruststoreProperty(EntityConnectionServerAdminImpl.class.getSimpleName());
    try {
      final Registry registry = ServerUtil.getRegistry(registryPort);
      final EntityConnectionServerAdmin serverAdmin = (EntityConnectionServerAdmin) registry.lookup(serverName);
      final String shutDownInfo = serverName + " found in registry on port: " + registryPort + ", shutting down";
      LOG.info(shutDownInfo);
      System.out.println(shutDownInfo);
      serverAdmin.shutdown();
    }
    catch (RemoteException e) {
      System.out.println("No rmi registry running on port: " + registryPort);
    }
    catch (NotBoundException e) {
      System.out.println(serverName + " not bound to registry on port: " + registryPort);
    }
  }

  /**
   * If no arguments are supplied a new EntityConnectionServer with a server admin interface is started,
   * If the argument 'shutdown' is supplied the server, if running, is shut down.
   * @param arguments 'shutdown' causes a running server to be shut down
   * @throws RemoteException in case of a remote exception during service export
   * @throws ClassNotFoundException in case the domain model classes required for the server is not found or
   * if the jdbc driver class is not found
   * @throws DatabaseException in case of an exception while constructing the initial pooled connections
   */
  public static void main(final String[] arguments) throws RemoteException, ClassNotFoundException, DatabaseException {
    if (arguments.length == 0) {
      startServer();
    }
    else if (arguments[0].equalsIgnoreCase("shutdown")) {
      shutdownServer();
    }
  }
}
