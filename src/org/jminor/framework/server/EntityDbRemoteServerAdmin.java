/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.ClientInfo;
import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.ConnectionPoolStatistics;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseStatistics;
import org.jminor.common.db.ServerLog;
import org.jminor.common.db.User;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityDbConnection;

import org.apache.log4j.Level;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.RemoteException;
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
public class EntityDbRemoteServerAdmin extends UnicastRemoteObject implements IEntityDbRemoteServerAdmin {

  private final EntityDbRemoteServer server;

  public EntityDbRemoteServerAdmin(final EntityDbRemoteServer server, final int adminPort,
                                   final boolean useSecureConnection) throws RemoteException {
    super(adminPort, useSecureConnection ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            useSecureConnection ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.server = server;
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
    return Database.get().getURL(null);
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
    final Set<String> ret = new HashSet<String>();
    for (final ClientInfo client : getClients(null))
      ret.add(client.getClientTypeID());

    return ret;
  }

  /** {@inheritDoc} */
  public void disconnect(final String connectionKey) throws RemoteException {
    server.disconnect(connectionKey);
  }

  /** {@inheritDoc} */
  public void shutdown() throws RemoteException {
    server.shutdown();
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
  public List<ConnectionPoolSettings> getActiveConnectionPools() throws RemoteException {
    return EntityDbRemoteAdapter.getActiveConnectionPoolSettings();
  }

  /** {@inheritDoc} */
  public void setConnectionPoolSettings(final ConnectionPoolSettings settings) throws RemoteException {
    EntityDbRemoteAdapter.setConnectionPoolSettings(settings.getUser(), settings);
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
}
