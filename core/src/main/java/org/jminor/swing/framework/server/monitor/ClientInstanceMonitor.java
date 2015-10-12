/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.framework.server.EntityConnectionServerAdmin;
import org.jminor.swing.common.ui.ValueLinks;

import javax.swing.ButtonModel;
import java.rmi.RemoteException;

/**
 * A ClientInstanceMonitor
 */
public final class ClientInstanceMonitor {

  private final Event<Boolean> loggingStatusChangedEvent = Events.event();

  private final ClientInfo clientInfo;
  private final EntityConnectionServerAdmin server;
  private ButtonModel loggingEnabledButtonModel;

  public ClientInstanceMonitor(final ClientInfo clientInfo, final EntityConnectionServerAdmin server) {
    this.clientInfo = clientInfo;
    this.server = server;
  }

  public ClientInfo getClientInfo() {
    return clientInfo;
  }

  public ButtonModel getLoggingEnabledButtonModel() {
    if (loggingEnabledButtonModel == null) {
      loggingEnabledButtonModel = ValueLinks.toggleValueLink(this, "loggingEnabled", loggingStatusChangedEvent);
    }

    return loggingEnabledButtonModel;
  }

  public long getCreationDate() throws RemoteException {
    final ClientLog log = server.getClientLog(clientInfo.getClientID());
    return log != null ? log.getConnectionCreationDate() : 0;
  }

  public ClientLog getLog() throws RemoteException {
    return server.getClientLog(clientInfo.getClientID());
  }

  public boolean isLoggingEnabled() throws RemoteException {
    return server.isLoggingEnabled(clientInfo.getClientID());
  }

  public void setLoggingEnabled(final boolean status) throws RemoteException {
    server.setLoggingEnabled(clientInfo.getClientID(), status);
    loggingStatusChangedEvent.fire(status);
  }

  public void disconnect() throws RemoteException {
    server.disconnect(clientInfo.getClientID());
  }

  @Override
  public String toString() {
    return clientInfo.toString();
  }
}
