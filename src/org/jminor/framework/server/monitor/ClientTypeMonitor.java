/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.ClientInfo;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import javax.swing.tree.DefaultMutableTreeNode;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 11.12.2007
 * Time: 11:42:18
 */
public class ClientTypeMonitor extends DefaultMutableTreeNode {

  private final IEntityDbRemoteServerAdmin server;
  private final String clientTypeID;

  public ClientTypeMonitor(final IEntityDbRemoteServerAdmin server, final String clientTypeID) throws RemoteException {
    this.server = server;
    this.clientTypeID = clientTypeID;
    refresh();
  }

  public void refresh() throws RemoteException {
    removeAllChildren();
    for (final ClientInfo client : server.getClients(null))
      if (clientTypeID.equals(client.getClientTypeID()))
        add(new ClientInstanceMonitor(client, server));
  }

  public String toString() {
    return clientTypeID + " (" + getChildCount() + ")";
  }

  public IEntityDbRemoteServerAdmin getServer() {
    return server;
  }

  public void disconnectAll() throws RemoteException {
    server.removeConnections(false);
    refresh();
  }

  public void disconnectTimedOut() throws RemoteException {
    server.removeConnections(true);
    refresh();
  }
}