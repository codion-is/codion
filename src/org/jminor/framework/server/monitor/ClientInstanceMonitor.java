/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.ClientInfo;
import org.jminor.common.db.DbLog;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 18:22:24
 */
public class ClientInstanceMonitor {

  private final ClientInfo client;
  private final IEntityDbRemoteServerAdmin server;

  public ClientInstanceMonitor(final ClientInfo client, final IEntityDbRemoteServerAdmin server) {
    this.client = client;
    this.server = server;
  }

  public long getCreationDate() throws RemoteException {
    return server.getConnectionLog(client.getClientID()).getConnectionCreationDate();
  }

  public DbLog getLog() throws RemoteException {
    return server.getConnectionLog(client.getClientID());
  }

  @Override
  public String toString() {
    return client.toString();
  }
}
