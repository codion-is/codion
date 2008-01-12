/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.DbLog;
import org.jminor.common.model.Event;
import org.jminor.common.remote.RemoteClient;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import javax.swing.tree.DefaultMutableTreeNode;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 18:22:24
 */
public class ClientInstanceMonitor extends DefaultMutableTreeNode {

  public final Event evtRefreshed = new Event("ClientInstanceMonitor.evtRefreshed");

  private final RemoteClient client;
  private final IEntityDbRemoteServerAdmin server;

  public ClientInstanceMonitor(final RemoteClient client, final IEntityDbRemoteServerAdmin server) {
    this.client = client;
    this.server = server;
  }

  public String toString() {
    return client.toString();
  }

  public long getCreationDate() throws RemoteException {
    return server.getConnectionLog(client.getUser(), client.getClientID()).getConnectionCreationDate();
  }

  public DbLog getLog() throws RemoteException {
    return server.getConnectionLog(client.getUser(), client.getClientID());
  }
}
