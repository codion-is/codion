/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.rmi.RemoteException;

/**
 * User: Bj�rn Darri
 * Date: 11.12.2007
 * Time: 12:58:44
 */
public class ClientMonitor extends DefaultMutableTreeNode {

  private final IEntityDbRemoteServerAdmin server;

  public ClientMonitor(final IEntityDbRemoteServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public void refresh() throws RemoteException{
    removeAllChildren();
    for (final String clientType : server.getClientTypes())
      add(new ClientTypeMonitor(server, clientType));
  }

  public String toString() {
    return "Clients" + " (" + getGrandChildCount() + ")";
  }

  public int getGrandChildCount() {
    int ret = 0;
    if (getChildCount() > 0) {
      TreeNode node = getFirstChild();
      while (node != null) {
        ret += node.getChildCount();
        node = getChildAfter(node);
      }
    }

    return ret;
  }
}
