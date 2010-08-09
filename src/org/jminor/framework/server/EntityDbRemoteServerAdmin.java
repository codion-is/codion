/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.DatabaseStatistics;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.RemoteServer;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDbConnection;

import org.apache.log4j.Level;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.net.URI;
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
 * Implements the EntityDbServerAdmin interface, providing admin access to a EntityDbRemoteServer instance.<br>
 * User: Bjorn Darri<br>
 * Date: 28.6.2009<br>
 * Time: 16:17:14<br>
 */
public final class EntityDbRemoteServerAdmin extends UnicastRemoteObject implements EntityDbServerAdmin {

  private static final int SERVER_ADMIN_PORT;

  static {
    System.setSecurityManager(new RMISecurityManager());
    final String serverAdminPortProperty = System.getProperty(Configuration.SERVER_ADMIN_PORT);

    if (serverAdminPortProperty == null) {
      throw new RuntimeException("Required server property missing: " + Configuration.SERVER_ADMIN_PORT);
    }

    SERVER_ADMIN_PORT = Integer.parseInt(serverAdminPortProperty);
  }

  private final EntityDbRemoteServer server;

  public EntityDbRemoteServerAdmin(final EntityDbRemoteServer server, final boolean useSecureConnection) throws RemoteException {
    super(SERVER_ADMIN_PORT, useSecureConnection ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            useSecureConnection ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.server = server;
    server.getRegistry().rebind(server.getServerName() + RemoteServer.SERVER_ADMIN_SUFFIX, this);
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
    return server.getServerDbPort();
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
    return Util.getLoggingLevel();
  }

  /** {@inheritDoc} */
  public void setLoggingLevel(final Level level) throws RemoteException {
    Util.setLoggingLevel(level);
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
      server.getRegistry().unbind(server.getServerName() + RemoteServer.SERVER_ADMIN_SUFFIX);
    }
    catch (NotBoundException e) {/**/}
    server.shutdown();
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (NoSuchObjectException e) {/**/}
  }

  /** {@inheritDoc} */
  public int getActiveConnectionCount() throws RemoteException {
    return EntityDbRemoteAdapter.getActiveCount();
  }

  /** {@inheritDoc} */
  public int getCheckMaintenanceInterval() {
    return server.getCheckMaintenanceInterval();
  }

  /** {@inheritDoc} */
  public void setCheckMaintenanceInterval(final int interval) {
    server.setCheckMaintenanceInterval(interval);
  }

  /** {@inheritDoc} */
  public void removeConnections(final boolean inactiveOnly) throws RemoteException {
    server.removeConnections(inactiveOnly);
  }

  /** {@inheritDoc} */
  public void resetConnectionPoolStatistics(final User user) throws RemoteException {
    EntityDbRemoteAdapter.resetConnectionPoolStatistics(user);
  }

  /** {@inheritDoc} */
  public boolean isCollectFineGrainedPoolStatistics(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.isCollectFineGrainedPoolStatistics(user);
  }

  /** {@inheritDoc} */
  public void setCollectFineGrainedPoolStatistics(final User user, final boolean value) throws RemoteException {
    EntityDbRemoteAdapter.setCollectFineGrainedPoolStatistics(user, value);
  }

  /** {@inheritDoc} */
  public int getRequestsPerSecond() throws RemoteException {
    return EntityDbRemoteAdapter.getRequestsPerSecond();
  }

  /** {@inheritDoc} */
  public int getWarningTimeThreshold() throws RemoteException {
    return EntityDbRemoteAdapter.getWarningThreshold();
  }

  /** {@inheritDoc} */
  public void setWarningTimeThreshold(final int threshold) throws RemoteException {
    EntityDbRemoteAdapter.setWarningThreshold(threshold);
  }

  /** {@inheritDoc} */
  public int getWarningTimeExceededPerSecond() throws RemoteException {
    return EntityDbRemoteAdapter.getWarningTimeExceededPerSecond();
  }

  /** {@inheritDoc} */
  public ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) throws RemoteException {
    return EntityDbRemoteAdapter.getConnectionPoolStatistics(user, since);
  }

  /** {@inheritDoc} */
  public DatabaseStatistics getDatabaseStatistics() throws RemoteException {
    return EntityDbConnection.getDatabaseStatistics();
  }

  /** {@inheritDoc} */
  public List<User> getEnabledConnectionPools() throws RemoteException {
    return EntityDbRemoteAdapter.getEnabledConnectionPoolSettings();
  }

  /** {@inheritDoc} */
  public boolean isConnectionPoolEnabled(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.isConnectionPoolEnabled(user);
  }

  /** {@inheritDoc} */
  public void setConnectionPoolCleanupInterval(final User user, final int poolCleanupInterval) throws RemoteException {
    EntityDbRemoteAdapter.setConnectionPoolCleanupInterval(user, poolCleanupInterval);
  }

  /** {@inheritDoc} */
  public void setConnectionPoolEnabled(final User user, final boolean enabled) throws RemoteException {
    EntityDbRemoteAdapter.setConnectionPoolEnabled(user, enabled);
  }

  /** {@inheritDoc} */
  public int getConnectionPoolCleanupInterval(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.getConnectionPoolCleanupInterval(user);
  }

  /** {@inheritDoc} */
  public int getMaximumConnectionPoolSize(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.getMaximumConnectionPoolSize(user);
  }

  /** {@inheritDoc} */
  public int getMinimumConnectionPoolSize(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.getMinimumConnectionPoolSize(user);
  }

  /** {@inheritDoc} */
  public int getPooledConnectionTimeout(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.getPooledConnectionTimeout(user);
  }

  /** {@inheritDoc} */
  public void setMaximumConnectionPoolSize(final User user, final int value) throws RemoteException {
    EntityDbRemoteAdapter.setMaximumConnectionPoolSize(user, value);
  }

  /** {@inheritDoc} */
  public void setMinimumConnectionPoolSize(final User user, final int value) throws RemoteException {
    EntityDbRemoteAdapter.setMinimumConnectionPoolSize(user, value);
  }

  /** {@inheritDoc} */
  public void setPooledConnectionTimeout(final User user, final int timeout) throws RemoteException {
    EntityDbRemoteAdapter.setPooledConnectionTimeout(user, timeout);
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
  public void loadDomainModel(final URI location, final String domainClassName) throws RemoteException, ClassNotFoundException,
          IllegalAccessException, InstantiationException {
    EntityDbRemoteServer.loadDomainModel(location, domainClassName);
  }

  /** {@inheritDoc} */
  public Map<String,String> getEntityDefinitions() throws RemoteException {
    return EntityDbRemoteServer.getEntityDefinitions();
  }

  private Runnable getShutdownHook() {
    return new Runnable() {
      public void run() {
        if (server.isShuttingDown()) {
          return;
        }
        try {
          EntityDbRemoteServer.LOG.info("Shuting down server on VM shutdown");
          server.shutdown();
        }
        catch (RemoteException e) {
          EntityDbRemoteServer.LOG.error("Exception on shutdown", e);
        }
      }
    };
  }

  public static void main(final String[] arguments) throws Exception {
    new EntityDbRemoteServerAdmin(new EntityDbRemoteServer(DatabaseProvider.createInstance()),
            EntityDbRemoteServer.SSL_CONNECTION_ENABLED);
  }
}
