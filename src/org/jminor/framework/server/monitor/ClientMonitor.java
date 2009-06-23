/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 11.12.2007
 * Time: 12:58:44
 */
public class ClientMonitor {

  private final IEntityDbRemoteServerAdmin server;

  private Collection<ClientTypeMonitor> clientTypeMonitors = new ArrayList<ClientTypeMonitor>();

  public ClientMonitor(final IEntityDbRemoteServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public Collection<ClientTypeMonitor> getClientTypeMonitors() {
    return clientTypeMonitors;
  }

  public void refresh() throws RemoteException{
    for (final String clientType : server.getClientTypes())
      clientTypeMonitors.add(new ClientTypeMonitor(server, clientType));
  }

  public void shutdown() {
    System.out.println("ClientMonitor shutdown");
    for (final ClientTypeMonitor monitor : clientTypeMonitors)
      monitor.shutdown();
  }
}
