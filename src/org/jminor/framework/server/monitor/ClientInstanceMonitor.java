/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.server.EntityDbServerAdmin;

import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 18:22:24
 */
public class ClientInstanceMonitor {

  private final ClientInfo client;
  private final EntityDbServerAdmin server;

  public ClientInstanceMonitor(final ClientInfo client, final EntityDbServerAdmin server) {
    this.client = client;
    this.server = server;
  }

  public long getCreationDate() throws RemoteException {
    return server.getServerLog(client.getClientID()).getConnectionCreationDate();
  }

  public ServerLog getLog() throws RemoteException {
    return server.getServerLog(client.getClientID());
  }

  @Override
  public String toString() {
    return client.toString();
  }
}
