/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.server.ClientInfo;
import org.jminor.framework.server.EntityDbServerAdmin;

import javax.swing.DefaultListModel;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri
 * Date: 11.12.2007
 * Time: 11:42:18
 */
public class ClientTypeMonitor {

  private final EntityDbServerAdmin server;
  private final String clientTypeID;

  private final DefaultListModel clientInstanceListModel = new DefaultListModel();

  public ClientTypeMonitor(final EntityDbServerAdmin server, final String clientTypeID) throws RemoteException {
    this.server = server;
    this.clientTypeID = clientTypeID;
    refresh();
  }

  public void refresh() throws RemoteException {
    clientInstanceListModel.clear();
    for (final ClientInfo client : server.getClients(clientTypeID))
      clientInstanceListModel.addElement(new ClientInstanceMonitor(client, server));
  }

  public DefaultListModel getClientInstanceListModel() {
    return clientInstanceListModel;
  }

  public String getClientTypeID() {
    return clientTypeID;
  }

  public EntityDbServerAdmin getServer() {
    return server;
  }

  @Override
  public String toString() {
    return getClientTypeID();
  }

  public void shutdown() {
    System.out.println("ClientTypeMonitor shutdown");
  }
}