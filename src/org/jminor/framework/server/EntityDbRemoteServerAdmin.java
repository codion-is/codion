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
import org.jminor.framework.domain.EntityDefinition;

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

/**
 * Implements the EntityDbServerAdmin interface, providing admin access to a EntityDbRemoteServer instance.<br>
 * User: Bjorn Darri<br>
 * Date: 28.6.2009<br>
 * Time: 16:17:14<br>
 */
public class EntityDbRemoteServerAdmin extends UnicastRemoteObject implements EntityDbServerAdmin {

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
  }

  public String getServerName() {
    return server.getServerName();
  }

  public int getServerPort() throws RemoteException {
    return server.getServerPort();
  }

  public String getSystemProperties() {
    return Util.getSystemProperties();
  }

  public int getServerDbPort() throws RemoteException {
    return server.getServerDbPort();
  }

  public long getStartDate() {
    return server.getStartDate();
  }

  public String getDatabaseURL() {
    return server.getDatabase().getURL(null);
  }

  public Level getLoggingLevel() throws RemoteException {
    return Util.getLoggingLevel();
  }

  public void setLoggingLevel(final Level level) throws RemoteException {
    Util.setLoggingLevel(level);
  }

  public Collection<User> getUsers() throws RemoteException {
    return server.getUsers();
  }

  public Collection<ClientInfo> getClients(final User user) throws RemoteException {
    return server.getClients(user);
  }

  public Collection<ClientInfo> getClients(final String clientTypeID) throws RemoteException {
    return server.getClients(clientTypeID);
  }

  public Collection<ClientInfo> getClients() throws RemoteException {
    return server.getClients();
  }

  public Collection<String> getClientTypes() throws RemoteException {
    final Set<String> clientTypes = new HashSet<String>();
    for (final ClientInfo client : getClients()) {
      clientTypes.add(client.getClientTypeID());
    }

    return clientTypes;
  }

  public void disconnect(final String connectionKey) throws RemoteException {
    server.disconnect(connectionKey);
  }

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

  public int getActiveConnectionCount() throws RemoteException {
    return EntityDbRemoteAdapter.getActiveCount();
  }

  public int getCheckMaintenanceInterval() {
    return server.getCheckMaintenanceInterval();
  }

  public void setCheckMaintenanceInterval(final int interval) {
    server.setCheckMaintenanceInterval(interval);
  }

  public void removeConnections(final boolean inactiveOnly) throws RemoteException {
    server.removeConnections(inactiveOnly);
  }

  public void resetConnectionPoolStatistics(final User user) throws RemoteException {
    EntityDbRemoteAdapter.resetConnectionPoolStatistics(user);
  }

  public boolean isCollectFineGrainedPoolStatistics(User user) throws RemoteException {
    return EntityDbRemoteAdapter.isCollectFineGrainedPoolStatistics(user);
  }

  public void setCollectFineGrainedPoolStatistics(final User user, final boolean value) throws RemoteException {
    EntityDbRemoteAdapter.setCollectFineGrainedPoolStatistics(user, value);
  }

  public int getRequestsPerSecond() throws RemoteException {
    return EntityDbRemoteAdapter.getRequestsPerSecond();
  }

  public int getWarningTimeThreshold() throws RemoteException {
    return EntityDbRemoteAdapter.getWarningThreshold();
  }

  public void setWarningTimeThreshold(int threshold) throws RemoteException {
    EntityDbRemoteAdapter.setWarningThreshold(threshold);
  }

  public int getWarningTimeExceededPerSecond() throws RemoteException {
    return EntityDbRemoteAdapter.getWarningTimeExceededPerSecond();
  }

  public ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) throws RemoteException {
    return EntityDbRemoteAdapter.getConnectionPoolStatistics(user, since);
  }

  public DatabaseStatistics getDatabaseStatistics() throws RemoteException {
    return EntityDbConnection.getDatabaseStatistics();
  }

  public List<User> getEnabledConnectionPools() throws RemoteException {
    return EntityDbRemoteAdapter.getEnabledConnectionPoolSettings();
  }

  public boolean isConnectionPoolEnabled(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.isConnectionPoolEnabled(user);
  }

  public void setConnectionPoolCleanupInterval(final User user, final int poolCleanupInterval) throws RemoteException {
    EntityDbRemoteAdapter.setConnectionPoolCleanupInterval(user, poolCleanupInterval);
  }

  public void setConnectionPoolEnabled(final User user, final boolean enabled) throws RemoteException {
    EntityDbRemoteAdapter.setConnectionPoolEnabled(user, enabled);
  }

  public int getConnectionPoolCleanupInterval(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.getConnectionPoolCleanupInterval(user);
  }

  public int getMaximumConnectionPoolSize(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.getMaximumConnectionPoolSize(user);
  }

  public int getMinimumConnectionPoolSize(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.getMinimumConnectionPoolSize(user);
  }

  public int getPooledConnectionTimeout(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.getPooledConnectionTimeout(user);
  }

  public void setMaximumConnectionPoolSize(final User user, final int value) throws RemoteException {
    EntityDbRemoteAdapter.setMaximumConnectionPoolSize(user, value);
  }

  public void setMinimumConnectionPoolSize(final User user, final int value) throws RemoteException {
    EntityDbRemoteAdapter.setMinimumConnectionPoolSize(user, value);
  }

  public void setPooledConnectionTimeout(final User user, final int timeout) throws RemoteException {
    EntityDbRemoteAdapter.setPooledConnectionTimeout(user, timeout);
  }

  public String getMemoryUsage() throws RemoteException {
    return Util.getMemoryUsageString();
  }

  public long getAllocatedMemory() throws RemoteException {
    return Util.getAllocatedMemory();
  }

  public long getUsedMemory() throws RemoteException {
    return Util.getUsedMemory();
  }

  public long getMaxMemory() throws RemoteException {
    return Util.getMaxMemory();
  }

  public void performGC() throws RemoteException {
    Runtime.getRuntime().gc();
  }

  public int getConnectionCount() {
    return server.getConnectionCount();
  }

  public ServerLog getServerLog(final String connectionKey) {
    return server.getServerLog(connectionKey);
  }

  public boolean isLoggingOn(final String connectionKey) throws RemoteException {
    return server.isLoggingOn(connectionKey);
  }

  public void setLoggingOn(final String connectionKey, final boolean status) {
    server.setLoggingOn(connectionKey, status);
  }

  public int getConnectionTimeout() throws RemoteException {
    return server.getConnectionTimeout();
  }

  public void setConnectionTimeout(final int timeout) throws RemoteException {
    server.setConnectionTimeout(timeout);
  }

  public void loadDomainModel(final URI location, final String domainClassName) throws RemoteException, ClassNotFoundException,
          IllegalAccessException, InstantiationException {
    EntityDbRemoteServer.loadDomainModel(location, domainClassName);
  }

  public Map<String, EntityDefinition> getEntityDefinitions() throws RemoteException {
    return EntityDbRemoteServer.getEntityDefinitions();
  }

  public static void main(String[] arguments) throws Exception {
    new EntityDbRemoteServerAdmin(new EntityDbRemoteServer(DatabaseProvider.createInstance()),
            EntityDbRemoteServer.SSL_CONNECTION_ENABLED);
  }
}
