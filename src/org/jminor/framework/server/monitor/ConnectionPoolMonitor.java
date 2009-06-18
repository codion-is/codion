/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.User;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import javax.swing.tree.DefaultMutableTreeNode;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 10.12.2007
 * Time: 15:27:24
 */
public class ConnectionPoolMonitor extends DefaultMutableTreeNode {

  private final IEntityDbRemoteServerAdmin server;
  private boolean shutdown = false;

  public ConnectionPoolMonitor(final IEntityDbRemoteServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public void refresh() throws RemoteException {
    removeAllChildren();
    if (!shutdown) {
      for (final ConnectionPoolSettings settings : server.getActiveConnectionPools())
        add(new ConnectionPoolInstanceMonitor(settings.getUser(), server));
    }
  }

  public void setConnectionPoolSettings(final ConnectionPoolSettings settings) throws RemoteException {
    server.setConnectionPoolSettings(settings);
    refresh();
  }

  @Override
  public String toString() {
    return "Connection pools" + " (" + getChildCount() + ")";
  }

  public void addConnectionPools(final String[] strings) throws RemoteException {
    for (final String username : strings)
      setConnectionPoolSettings(ConnectionPoolSettings.getDefault(new User(username.trim(), null)));
  }

  public void shutdown() throws RemoteException {
    System.out.println("ConnectionPoolMonitor shutdown");
    shutdown = true;
    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++)
      ((ConnectionPoolInstanceMonitor) getChildAt(i)).shutdown();
    removeAllChildren();
  }
}
