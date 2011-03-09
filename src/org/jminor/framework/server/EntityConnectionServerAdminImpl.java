/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.RemoteServer;
import org.jminor.common.server.ServerLog;
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

  private static final long serialVersionUID = 1;

  private static final int SERVER_ADMIN_PORT;

  static {
    System.setSecurityManager(new RMISecurityManager());
    final String serverAdminPortProperty = Configuration.getStringValue(Configuration.SERVER_ADMIN_PORT);
    Util.require(Configuration.SERVER_ADMIN_PORT, serverAdminPortProperty);
    SERVER_ADMIN_PORT = Integer.parseInt(serverAdminPortProperty);
  }

  private final EntityConnectionServer server;

  /**
   * Instantiates a new EntityConnectionServerAdminImpl
   * @param server the server to administer
   * @throws RemoteException in case of an exception
   */
  public EntityConnectionServerAdminImpl(final EntityConnectionServer server) throws RemoteException {
    super(SERVER_ADMIN_PORT, EntityConnectionServer.SSL_CONNECTION_ENABLED ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            EntityConnectionServer.SSL_CONNECTION_ENABLED ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.server = server;
    Util.getRegistry(EntityConnectionServer.REGISTRY_PORT).rebind(RemoteServer.SERVER_ADMIN_PREFIX + server.getServerName(), this);
    Runtime.getRuntime().addShutdownHook(new Thread(getShutdownHook()));
  }

  /** {@inheritDoc} */
  public String getServerName() {
    return server.getServerName();
  }

  /** {@inheritDoc} */
  public int getServerPort() throws RemoteException {
    return server.getServerPort();
  }

  /** {@inheritDoc} */
  public String getSystemProperties() {
    return Util.getSystemProperties();
  }

  /** {@inheritDoc} */
  public int getServerDbPort() throws RemoteException {
    return EntityConnectionServer.getServerDbPort();
  }

  /** {@inheritDoc} */
  public long getStartDate() {
    return server.getStartDate();
  }

  /** {@inheritDoc} */
  public String getDatabaseURL() {
    return server.getDatabase().getURL(null);
  }

  /** {@inheritDoc} */
  public Level getLoggingLevel() throws RemoteException {
    final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    return rootLogger.getLevel();
  }

  /** {@inheritDoc} */
  public void setLoggingLevel(final Level level) throws RemoteException {
    final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(level);
  }

  /** {@inheritDoc} */
  public Collection<User> getUsers() throws RemoteException {
    return server.getUsers();
  }

  /** {@inheritDoc} */
  public Collection<ClientInfo> getClients(final User user) throws RemoteException {
    return server.getClients(user);
  }

  /** {@inheritDoc} */
  public Collection<ClientInfo> getClients(final String clientTypeID) throws RemoteException {
    return server.getClients(clientTypeID);
  }

  /** {@inheritDoc} */
  public Collection<ClientInfo> getClients() throws RemoteException {
    return server.getClients();
  }

  /** {@inheritDoc} */
  public Collection<String> getClientTypes() throws RemoteException {
    final Set<String> clientTypes = new HashSet<String>();
    for (final ClientInfo client : getClients()) {
      clientTypes.add(client.getClientTypeID());
    }

    return clientTypes;
  }

  /** {@inheritDoc} */
  public void disconnect(final UUID clientID) throws RemoteException {
    server.disconnect(clientID);
  }

  /** {@inheritDoc} */
  public void shutdown() throws RemoteException {
    try {
      Util.getRegistry(EntityConnectionServer.REGISTRY_PORT).unbind(server.getServerName());
    }
    catch (NotBoundException e) {/**/}
    try {
      Util.getRegistry(EntityConnectionServer.REGISTRY_PORT).unbind(RemoteServer.SERVER_ADMIN_PREFIX + server.getServerName());
    }
    catch (NotBoundException e) {/**/}

    final String shutdownInfo = server.getServerName() + " removed from registry";
    EntityConnectionServer.LOG.info(shutdownInfo);
    System.out.println(shutdownInfo);

    EntityConnectionServer.LOG.info("Shutting down server");
    server.shutdown();
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (NoSuchObjectException e) {/**/}
  }

  /** {@inheritDoc} */
  public int getActiveConnectionCount() throws RemoteException {
    return RemoteEntityConnectionImpl.getActiveCount();
  }

  /** {@inheritDoc} */
  public int getMaintenanceInterval() {
    return server.getMaintenanceInterval();
  }

  /** {@inheritDoc} */
  public void setMaintenanceInterval(final int interval) {
    server.setMaintenanceInterval(interval);
  }

  /** {@inheritDoc} */
  public void removeConnections(final boolean inactiveOnly) throws RemoteException {
    server.removeConnections(inactiveOnly);
  }

  /** {@inheritDoc} */
  public void resetConnectionPoolStatistics(final User user) throws RemoteException {
    RemoteEntityConnectionImpl.resetPoolStatistics(user);
  }

  /** {@inheritDoc} */
  public boolean isCollectFineGrainedPoolStatistics(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.isCollectFineGrainedPoolStatistics(user);
  }

  /** {@inheritDoc} */
  public void setCollectFineGrainedPoolStatistics(final User user, final boolean value) throws RemoteException {
    RemoteEntityConnectionImpl.setCollectFineGrainedPoolStatistics(user, value);
  }

  /** {@inheritDoc} */
  public int getRequestsPerSecond() throws RemoteException {
    return RemoteEntityConnectionImpl.getRequestsPerSecond();
  }

  /** {@inheritDoc} */
  public int getWarningTimeThreshold() throws RemoteException {
    return RemoteEntityConnectionImpl.getWarningThreshold();
  }

  /** {@inheritDoc} */
  public void setWarningTimeThreshold(final int threshold) throws RemoteException {
    RemoteEntityConnectionImpl.setWarningThreshold(threshold);
  }

  /** {@inheritDoc} */
  public int getWarningTimeExceededPerSecond() throws RemoteException {
    return RemoteEntityConnectionImpl.getWarningTimeExceededPerSecond();
  }

  /** {@inheritDoc} */
  public ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) throws RemoteException {
    return RemoteEntityConnectionImpl.getPoolStatistics(user, since);
  }

  /** {@inheritDoc} */
  public Database.Statistics getDatabaseStatistics() throws RemoteException {
    return Databases.getDatabaseStatistics();
  }

  /** {@inheritDoc} */
  public List<User> getEnabledConnectionPools() throws RemoteException {
    return RemoteEntityConnectionImpl.getEnabledConnectionPoolSettings();
  }

  /** {@inheritDoc} */
  public boolean isConnectionPoolEnabled(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.isPoolEnabled(user);
  }

  /** {@inheritDoc} */
  public void setConnectionPoolCleanupInterval(final User user, final int poolCleanupInterval) throws RemoteException {
    RemoteEntityConnectionImpl.setPoolCleanupInterval(user, poolCleanupInterval);
  }

  /** {@inheritDoc} */
  public void setConnectionPoolEnabled(final User user, final boolean enabled) throws RemoteException {
    RemoteEntityConnectionImpl.setPoolEnabled(user, enabled);
  }

  /** {@inheritDoc} */
  public int getConnectionPoolCleanupInterval(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getPoolCleanupInterval(user);
  }

  /** {@inheritDoc} */
  public int getMaximumConnectionPoolSize(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getMaximumPoolSize(user);
  }

  /** {@inheritDoc} */
  public int getMinimumConnectionPoolSize(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getMinimumPoolSize(user);
  }

  /** {@inheritDoc} */
  public int getPooledConnectionTimeout(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getPoolConnectionTimeout(user);
  }

  /** {@inheritDoc} */
  public void setMaximumConnectionPoolSize(final User user, final int value) throws RemoteException {
    RemoteEntityConnectionImpl.setMaximumPoolSize(user, value);
  }

  /** {@inheritDoc} */
  public void setMinimumConnectionPoolSize(final User user, final int value) throws RemoteException {
    RemoteEntityConnectionImpl.setMinimumPoolSize(user, value);
  }

  /** {@inheritDoc} */
  public int getPoolConnectionThreshold(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getPoolConnectionThreshold(user);
  }

  /** {@inheritDoc} */
  public void setPoolConnectionThreshold(final User user, final int value) throws RemoteException {
    RemoteEntityConnectionImpl.setPoolConnectionThreshold(user, value);
  }

  /** {@inheritDoc} */
  public void setPooledConnectionTimeout(final User user, final int timeout) throws RemoteException {
    RemoteEntityConnectionImpl.setPoolConnectionTimeout(user, timeout);
  }

  /** {@inheritDoc} */
  public int getMaximumPoolRetryWaitPeriod(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getMaximumPoolRetryWaitPeriod(user);
  }

  /** {@inheritDoc} */
  public void setMaximumPoolRetryWaitPeriod(final User user, final int value) throws RemoteException {
    RemoteEntityConnectionImpl.setMaximumPoolRetryWaitPeriod(user, value);
  }

  /** {@inheritDoc} */
  public int getMaximumPoolCheckOutTime(final User user) throws RemoteException {
    return RemoteEntityConnectionImpl.getMaximumPoolCheckOutTime(user);
  }

  /** {@inheritDoc} */
  public void setMaximumPoolCheckOutTime(final User user, final int value) throws RemoteException {
    RemoteEntityConnectionImpl.setMaximumPoolCheckOutTime(user, value);
  }

  /** {@inheritDoc} */
  public String getMemoryUsage() throws RemoteException {
    return Util.getMemoryUsageString();
  }

  /** {@inheritDoc} */
  public long getAllocatedMemory() throws RemoteException {
    return Util.getAllocatedMemory();
  }

  /** {@inheritDoc} */
  public long getUsedMemory() throws RemoteException {
    return Util.getUsedMemory();
  }

  /** {@inheritDoc} */
  public long getMaxMemory() throws RemoteException {
    return Util.getMaxMemory();
  }

  /** {@inheritDoc} */
  public void performGC() throws RemoteException {
    Runtime.getRuntime().gc();
  }

  /** {@inheritDoc} */
  public int getConnectionCount() {
    return server.getConnectionCount();
  }

  /** {@inheritDoc} */
  public int getConnectionLimit() throws RemoteException {
    return server.getConnectionLimit();
  }

  /** {@inheritDoc} */
  public void setConnectionLimit(final int value) throws RemoteException {
    server.setConnectionLimit(value);
  }

  /** {@inheritDoc} */
  public ServerLog getServerLog(final UUID clientID) {
    return server.getServerLog(clientID);
  }

  /** {@inheritDoc} */
  public boolean isLoggingOn(final UUID clientID) throws RemoteException {
    return server.isLoggingOn(clientID);
  }

  /** {@inheritDoc} */
  public void setLoggingOn(final UUID clientID, final boolean status) {
    server.setLoggingOn(clientID, status);
  }

  /** {@inheritDoc} */
  public int getConnectionTimeout() throws RemoteException {
    return server.getConnectionTimeout();
  }

  /** {@inheritDoc} */
  public void setConnectionTimeout(final int timeout) throws RemoteException {
    server.setConnectionTimeout(timeout);
  }

  /** {@inheritDoc} */
  public Map<String,String> getEntityDefinitions() throws RemoteException {
    return EntityConnectionServer.getEntityDefinitions();
  }

  private Runnable getShutdownHook() {
    return new Runnable() {
      /** {@inheritDoc} */
      public void run() {
        if (server.isShuttingDown()) {
          return;
        }
        try {
          shutdown();
        }
        catch (RemoteException e) {
          EntityConnectionServer.LOG.error("Exception during shutdown", e);
        }
      }
    };
  }

  /**
   * Runs a new EntityConnectionServer with a server admin interface exported.
   * @param arguments no arguments required
   * @throws java.rmi.RemoteException in case of a remote exception during service export
   * @throws ClassNotFoundException in case the domain model classes required for the server is not found
   */
  public static void main(final String[] arguments) throws RemoteException, ClassNotFoundException {
    final EntityConnectionServer server = new EntityConnectionServer(Databases.createInstance());
    new EntityConnectionServerAdminImpl(server);
  }
}
