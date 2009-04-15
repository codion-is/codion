/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.User;
import org.jminor.common.model.ClientInfo;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import javax.swing.tree.DefaultMutableTreeNode;
import java.rmi.RemoteException;

/**
 * User: Bj�rn Darri
 * Date: 11.12.2007
 * Time: 11:28:28
 */
public class UserInstanceMonitor extends DefaultMutableTreeNode {

  private final IEntityDbRemoteServerAdmin server;
  private final User user;

  public UserInstanceMonitor(final IEntityDbRemoteServerAdmin server, final User user) throws RemoteException {
    this.server = server;
    this.user = user;
    refresh();
  }

  public void refresh() throws RemoteException {
    removeAllChildren();
    for (final ClientInfo client : server.getClients(user))
      add(new ClientInstanceMonitor(client , server));
  }

  public String toString() {
    return user.toString() + " (" + getChildCount() + ")";
  }

  public IEntityDbRemoteServerAdmin getServer() {
    return server;
  }
}