/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.server.EntityDbServerAdmin;

import javax.swing.DefaultListModel;
import java.rmi.RemoteException;
import java.util.Collection;

/**
 * A ClientMonitor
 */
public final class ClientMonitor {

  private final EntityDbServerAdmin server;
  private final String clientTypeID;
  private final User user;

  private final DefaultListModel clientInstanceListModel = new DefaultListModel();

  public ClientMonitor(final EntityDbServerAdmin server, final String clientTypeID, final User user) throws RemoteException {
    this.server = server;
    this.clientTypeID = clientTypeID;
    this.user = user;
    refresh();
  }

  public void refresh() throws RemoteException {
    clientInstanceListModel.clear();
    final Collection<ClientInfo> clients = clientTypeID == null ? server.getClients(user) : server.getClients(clientTypeID);
    for (final ClientInfo client : clients) {
      clientInstanceListModel.addElement(new ClientInstanceMonitor(client, server));
    }
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
    return clientTypeID == null ? user.toString() : clientTypeID;
  }
}