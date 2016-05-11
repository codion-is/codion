/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import javax.swing.DefaultListModel;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A ClientMonitor
 */
public final class ClientMonitor {

  private static final Comparator<ClientInfo> CLIENT_INFO_COMPARATOR = new Comparator<ClientInfo>() {
    @Override
    public int compare(final ClientInfo c1, final ClientInfo c2) {
      return c1.getUser().getUsername().compareToIgnoreCase(c2.getUser().getUsername());
    }
  };
  private final EntityConnectionServerAdmin server;
  private final String clientTypeID;
  private final User user;

  private final DefaultListModel<ClientInstanceMonitor> clientInstanceListModel = new DefaultListModel<>();

  public ClientMonitor(final EntityConnectionServerAdmin server, final String clientTypeID, final User user) throws RemoteException {
    this.server = server;
    this.clientTypeID = clientTypeID;
    this.user = user;
    refresh();
  }

  public void refresh() throws RemoteException {
    clientInstanceListModel.clear();
    final List<ClientInfo> clients = new ArrayList<>(clientTypeID == null ? server.getClients(user) : server.getClients(clientTypeID));
    Collections.sort(clients, CLIENT_INFO_COMPARATOR);
    for (final ClientInfo client : clients) {
      clientInstanceListModel.addElement(new ClientInstanceMonitor(client, server));
    }
  }

  public DefaultListModel<ClientInstanceMonitor> getClientInstanceListModel() {
    return clientInstanceListModel;
  }

  public String getClientTypeID() {
    return clientTypeID;
  }

  @Override
  public String toString() {
    return clientTypeID == null ? user.toString() : clientTypeID;
  }
}