/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.framework.server.EntityDbServerAdmin;

import javax.swing.DefaultListModel;
import java.rmi.RemoteException;
import java.util.Enumeration;

/**
 * User: Bjorn Darri<br>
 * Date: 11.12.2007<br>
 * Time: 12:58:44<br>
 */
public class ClientMonitor {

  private final EntityDbServerAdmin server;

  private final DefaultListModel clientTypeListModel = new DefaultListModel();

  public ClientMonitor(final EntityDbServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public DefaultListModel getClientTypeListModel() {
    return clientTypeListModel;
  }

  public void refresh() throws RemoteException{
    clientTypeListModel.clear();
    for (final String clientType : server.getClientTypes())
      clientTypeListModel.addElement(new ClientTypeMonitor(server, clientType));
  }

  public void disconnectAll() throws RemoteException {
    server.removeConnections(false);
    refresh();
  }

  public void disconnectTimedOut() throws RemoteException {
    server.removeConnections(true);
    refresh();
  }

  public void setCheckMaintenanceInterval(final int interval) throws RemoteException {
    server.setCheckMaintenanceInterval(interval);
  }

  public int getCheckMaintenanceInterval() throws RemoteException {
    return server.getCheckMaintenanceInterval();
  }

  public void shutdown() {
    System.out.println("ClientMonitor shutdown");
    final Enumeration enumeration = clientTypeListModel.elements();
    while (enumeration.hasMoreElements())
      ((ClientTypeMonitor) enumeration.nextElement()).shutdown();
  }
}
