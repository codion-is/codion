/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.User;
import org.jminor.framework.server.EntityDbServerAdmin;

import javax.swing.DefaultListModel;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri<br>
 * Date: 11.12.2007<br>
 * Time: 12:58:44<br>
 */
public final class ClientUserMonitor {

  private final EntityDbServerAdmin server;
  private final Event evtConnectionTimeoutChanged = Events.event();

  private final DefaultListModel clientTypeListModel = new DefaultListModel();
  private final DefaultListModel userListModel = new DefaultListModel();

  public ClientUserMonitor(final EntityDbServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public DefaultListModel getClientTypeListModel() {
    return clientTypeListModel;
  }

  public DefaultListModel getUserListModel() {
    return userListModel;
  }

  public void refresh() throws RemoteException{
    clientTypeListModel.clear();
    for (final String clientType : server.getClientTypes()) {
      clientTypeListModel.addElement(new ClientMonitor(server, clientType, null));
    }
    userListModel.clear();
    for (final User user : server.getUsers()) {
      userListModel.addElement(new ClientMonitor(server, null, user));
    }
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

  public int getConnectionTimeout() throws RemoteException {
    return server.getConnectionTimeout();
  }

  public void setConnectionTimeout(final int timeout) throws RemoteException {
    server.setConnectionTimeout(timeout);
    evtConnectionTimeoutChanged.fire();
  }

  public void addConnectionTimeoutListener(final ActionListener listener) {
    evtConnectionTimeoutChanged.addListener(listener);
  }

  public void removeConnectionTimeoutListener(final ActionListener listener) {
    evtConnectionTimeoutChanged.removeListener(listener);
  }

  public EventObserver getConnectionTimeoutObserver() {
    return evtConnectionTimeoutChanged.getObserver();
  }
}
