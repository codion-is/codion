/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.ConnectionPoolStatistics;
import org.jminor.common.db.DatabaseStatistics;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDbConnection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.NoSuchObjectException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: Björn Darri
 * Date: 28.6.2009
 * Time: 16:17:14
 */
public class EntityDbRemoteServerAdmin extends UnicastRemoteObject implements EntityDbServerAdmin {

  private static final Logger log = Util.getLogger(EntityDbRemoteServerAdmin.class);

  private static final int SERVER_ADMIN_PORT;

  static {
    final String serverAdminPortProperty = System.getProperty(Configuration.SERVER_ADMIN_PORT);

    if (serverAdminPortProperty == null)
      throw new RuntimeException("Required server property missing: " + Configuration.SERVER_ADMIN_PORT);

    SERVER_ADMIN_PORT = Integer.parseInt(serverAdminPortProperty);
  }

  private final EntityDbRemoteServer server;

  public EntityDbRemoteServerAdmin(final EntityDbRemoteServer server, final int adminPort,
                                   final boolean useSecureConnection) throws RemoteException {
    super(adminPort, useSecureConnection ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            useSecureConnection ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.server = server;
    server.getRegistry().rebind(server.getServerName() + EntityDbServer.SERVER_ADMIN_SUFFIX, this);
  }

  /** {@inheritDoc} */
  public String getServerName() {
    return server.getServerName();
  }

  /** {@inheritDoc} */
  public int getServerPort() throws RemoteException {
    return server.getServerPort();
  }

  public String getSystemProperties() {
    return Util.getSystemProperties();
  }

  /** {@inheritDoc} */
  public int getServerDbPort() throws RemoteException {
    return server.getServerDbPort();
  }

  /** {@inheritDoc} */
  public Date getStartDate() {
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

  public Collection<String> getClientTypes() throws RemoteException {
    final Set<String> clientTypes = new HashSet<String>();
    for (final ClientInfo client : getClients(null))
      clientTypes.add(client.getClientTypeID());

    return clientTypes;
  }

  /** {@inheritDoc} */
  public void disconnect(final String connectionKey) throws RemoteException {
    server.disconnect(connectionKey);
  }

  /** {@inheritDoc} */
  public void shutdown() throws RemoteException {
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
  public void setCheckMaintenanceInterval(final int checkTimerInterval) {
    server.setCheckMaintenanceInterval(checkTimerInterval);
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
  public boolean isCollectPoolStatistics(User user) throws RemoteException {
    return EntityDbRemoteAdapter.isCollectPoolStatistics(user);
  }

  /** {@inheritDoc} */
  public void setCollectPoolStatistics(final User user, final boolean value) throws RemoteException {
    EntityDbRemoteAdapter.setCollectPoolStatistics(user, value);
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
  public void setWarningTimeThreshold(int threshold) throws RemoteException {
    EntityDbRemoteAdapter.setWarningThreshold(threshold);
  }

  /** {@inheritDoc} */
  public int getWarningTimeExceededPerSecond() throws RemoteException {
    return EntityDbRemoteAdapter.getWarningTimeExceededPerSecond();
  }

  /** {@inheritDoc} */
  public ConnectionPoolSettings getConnectionPoolSettings(final User user) throws RemoteException {
    return EntityDbRemoteAdapter.getConnectionPoolSettings(user);
  }

  /** {@inheritDoc} */
  public ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) throws RemoteException {
    return EntityDbRemoteAdapter.getConnectionPoolStatistics(user, since);
  }

  /** {@inheritDoc} */
  public DatabaseStatistics getDatabaseStatistics() throws RemoteException {
    return new DatabaseStatistics(EntityDbConnection.getQueriesPerSecond(), EntityDbConnection.getCachedQueriesPerSecond());
  }

  /** {@inheritDoc} */
  public List<ConnectionPoolSettings> getEnabledConnectionPools() throws RemoteException {
    return EntityDbRemoteAdapter.getEnabledConnectionPoolSettings();
  }

  /** {@inheritDoc} */
  public void setConnectionPoolSettings(final ConnectionPoolSettings settings) throws RemoteException {
    EntityDbRemoteAdapter.setConnectionPoolSettings(server.getDatabase(), settings.getUser(), settings);
  }

  /** {@inheritDoc} */
  public String getMemoryUsage() throws RemoteException {
    return Util.getMemoryUsageString();
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
  public ServerLog getServerLog(final String connectionKey) {
    return server.getServerLog(connectionKey);
  }

  /** {@inheritDoc} */
  public boolean isLoggingOn(final String connectionKey) throws RemoteException {
    return server.isLoggingOn(connectionKey);
  }

  /** {@inheritDoc} */
  public void setLoggingOn(final String connectionKey, final boolean status) {
    server.setLoggingOn(connectionKey, status);
  }

  /** {@inheritDoc} */
  public int getConnectionTimeout() throws RemoteException {
    return server.getConnectionTimeout();
  }

  /** {@inheritDoc} */
  public void setConnectionTimeout(final int timeout) throws RemoteException {
    server.setConnectionTimeout(timeout);
  }

  private static void initializeRegistry() throws RemoteException {
    Registry localRegistry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
    try {
      localRegistry.list();
    }
    catch (Exception e) {
      log.info("Server creating registry on port: " + Registry.REGISTRY_PORT);
      LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    }
  }

  public static void main(String[] arguments) {
    try {
      System.setSecurityManager(new RMISecurityManager());
      initializeRegistry();
      new EntityDbRemoteServerAdmin(new EntityDbRemoteServer(DatabaseProvider.createInstance()),
              SERVER_ADMIN_PORT, EntityDbRemoteServer.SECURE_CONNECTION);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
