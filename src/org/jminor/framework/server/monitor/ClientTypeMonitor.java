/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.ClientInfo;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 11.12.2007
 * Time: 11:42:18
 */
public class ClientTypeMonitor {

  private final IEntityDbRemoteServerAdmin server;
  private final String clientTypeID;

  private final Collection<ClientInstanceMonitor> clientInstanceMonitors = new ArrayList<ClientInstanceMonitor>();

  public ClientTypeMonitor(final IEntityDbRemoteServerAdmin server, final String clientTypeID) throws RemoteException {
    this.server = server;
    this.clientTypeID = clientTypeID;
    refresh();
  }

  public void refresh() throws RemoteException {
    for (final ClientInfo client : server.getClients(null))
      if (clientTypeID.equals(client.getClientTypeID()))
        clientInstanceMonitors.add(new ClientInstanceMonitor(client, server));
  }

  public String getClientTypeID() {
    return clientTypeID;
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

  public void shutdown() throws RemoteException {
    System.out.println("ClientTypeMonitor shutdown");
  }
}