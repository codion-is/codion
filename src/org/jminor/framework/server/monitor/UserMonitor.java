/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.User;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import javax.swing.tree.DefaultMutableTreeNode;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 11.12.2007
 * Time: 11:26:21
 */
public class UserMonitor extends DefaultMutableTreeNode {

  private final IEntityDbRemoteServerAdmin server;

  public UserMonitor(final IEntityDbRemoteServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public void refresh() throws RemoteException {
    removeAllChildren();
    for (final User user : server.getUsers())
      add(new UserInstanceMonitor(server, user));
  }

  public String toString() {
    return "Database users" + " (" + getChildCount() + ")";
  }

  public IEntityDbRemoteServerAdmin getServer() {
    return server;
  }
}