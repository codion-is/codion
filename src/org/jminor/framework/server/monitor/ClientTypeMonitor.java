/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.server.ClientInfo;
import org.jminor.framework.server.EntityDbServerAdmin;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 11.12.2007
 * Time: 11:42:18
 */
public class ClientTypeMonitor {

  private final EntityDbServerAdmin server;
  private final String clientTypeID;

  private final Collection<ClientInstanceMonitor> clientInstanceMonitors = new ArrayList<ClientInstanceMonitor>();

  public ClientTypeMonitor(final EntityDbServerAdmin server, final String clientTypeID) throws RemoteException {
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

  public EntityDbServerAdmin getServer() {
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

  public void shutdown() {
    System.out.println("ClientTypeMonitor shutdown");
  }
}