/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.framework.server.EntityDbServerAdmin;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Bjorn Darri
 * Date: 11.12.2007
 * Time: 12:58:44
 */
public class ClientMonitor {

  private final EntityDbServerAdmin server;

  private Collection<ClientTypeMonitor> clientTypeMonitors = new ArrayList<ClientTypeMonitor>();

  public ClientMonitor(final EntityDbServerAdmin server) throws RemoteException {
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
