/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.User;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import javax.swing.DefaultListModel;
import java.rmi.RemoteException;

/**
 * A ClientUserMonitor
 */
public final class ClientUserMonitor {

  private static final int THOUSAND = 1000;

  private final EntityConnectionServerAdmin server;
  private final Event<Integer> connectionTimeoutChangedEvent = Events.event();

  private final DefaultListModel<ClientMonitor> clientTypeListModel = new DefaultListModel<>();
  private final DefaultListModel<ClientMonitor> userListModel = new DefaultListModel<>();

  public ClientUserMonitor(final EntityConnectionServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public DefaultListModel<ClientMonitor> getClientTypeListModel() {
    return clientTypeListModel;
  }

  public DefaultListModel<ClientMonitor> getUserListModel() {
    return userListModel;
  }

  public void refresh() throws RemoteException {
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

  public void setMaintenanceInterval(final int interval) throws RemoteException {
    server.setMaintenanceInterval(interval * THOUSAND);
  }

  public int getMaintenanceInterval() throws RemoteException {
    return server.getMaintenanceInterval() / THOUSAND;
  }

  public int getConnectionTimeout() throws RemoteException {
    return server.getConnectionTimeout() / THOUSAND;
  }

  public void setConnectionTimeout(final int timeout) throws RemoteException {
    server.setConnectionTimeout(timeout * THOUSAND);
    connectionTimeoutChangedEvent.fire(timeout);
  }

  public EventObserver<Integer> getConnectionTimeoutObserver() {
    return connectionTimeoutChangedEvent.getObserver();
  }
}
