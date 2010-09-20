/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import javax.swing.ButtonModel;
import java.rmi.RemoteException;

/**
 * A ClientInstanceMonitor
 */
public final class ClientInstanceMonitor {

  private final Event evtLogginStatusChanged = Events.event();

  private final ClientInfo client;
  private final EntityConnectionServerAdmin server;
  private ButtonModel loggingEnabledButtonModel;

  public ClientInstanceMonitor(final ClientInfo client, final EntityConnectionServerAdmin server) {
    this.client = client;
    this.server = server;
  }

  public ButtonModel getLoggingEnabledButtonModel() {
    if (loggingEnabledButtonModel == null) {
      loggingEnabledButtonModel = new ToggleBeanValueLink(this, "loggingOn", evtLogginStatusChanged, null).getButtonModel();
    }

    return loggingEnabledButtonModel;
  }

  public long getCreationDate() throws RemoteException {
    final ServerLog log = server.getServerLog(client.getClientID());
    return log != null ? log.getConnectionCreationDate() : 0;
  }

  public ServerLog getLog() throws RemoteException {
    return server.getServerLog(client.getClientID());
  }

  public boolean isLoggingOn() throws RemoteException {
    return server.isLoggingOn(client.getClientID());
  }

  public void setLoggingOn(final boolean status) throws RemoteException {
    server.setLoggingOn(client.getClientID(), status);
    evtLogginStatusChanged.fire();
  }

  public void disconnect() throws RemoteException {
    server.disconnect(client.getClientID());
  }

  @Override
  public String toString() {
    return client.toString();
  }
}
