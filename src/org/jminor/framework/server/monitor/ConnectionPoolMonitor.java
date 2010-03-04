/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.User;
import org.jminor.framework.server.EntityDbServerAdmin;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 10.12.2007
 * Time: 15:27:24
 */
public class ConnectionPoolMonitor {

  private final EntityDbServerAdmin server;

  private Collection<ConnectionPoolInstanceMonitor> connectionPoolInstanceMonitors = new ArrayList<ConnectionPoolInstanceMonitor>();

  public ConnectionPoolMonitor(final EntityDbServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public void refresh() throws RemoteException {
    for (final ConnectionPoolSettings settings : server.getEnabledConnectionPools())
      connectionPoolInstanceMonitors.add(new ConnectionPoolInstanceMonitor(settings.getUser(), server));
  }

  public void setConnectionPoolSettings(final ConnectionPoolSettings settings) throws RemoteException {
    server.setConnectionPoolSettings(settings);
    refresh();
  }

  public void addConnectionPools(final String[] strings) throws RemoteException {
    for (final String username : strings)
      setConnectionPoolSettings(ConnectionPoolSettings.getDefault(new User(username.trim(), null)));
  }

  public Collection<ConnectionPoolInstanceMonitor> getConnectionPoolInstanceMonitors() {
    return connectionPoolInstanceMonitors;
  }

  public void shutdown() {
    System.out.println("ConnectionPoolMonitor shutdown");
    for (final ConnectionPoolInstanceMonitor monitor : connectionPoolInstanceMonitors)
      monitor.shutdown();
  }
}
