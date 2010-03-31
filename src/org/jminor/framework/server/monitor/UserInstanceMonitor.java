/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.server.EntityDbServerAdmin;

import javax.swing.DefaultListModel;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri
 * Date: 11.12.2007
 * Time: 11:28:28
 */
public class UserInstanceMonitor {

  private final EntityDbServerAdmin server;
  private final User user;

  private final DefaultListModel clientListModel = new DefaultListModel();

  public UserInstanceMonitor(final EntityDbServerAdmin server, final User user) throws RemoteException {
    this.server = server;
    this.user = user;
    refresh();
  }

  public DefaultListModel getClientListModel() {
    return clientListModel;
  }

  public void refresh() throws RemoteException {
    clientListModel.clear();
    for (final ClientInfo client : server.getClients(user))
      clientListModel.addElement(new ClientInstanceMonitor(client , server));
  }

  public EntityDbServerAdmin getServer() {
    return server;
  }

  public void shutdown() {
    System.out.println("UserInstanceMonitor shutdown");
  }

  @Override
  public String toString() {
    return user.toString();
  }
}