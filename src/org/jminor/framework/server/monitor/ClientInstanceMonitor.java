/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.common.ui.ValueLinks;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import javax.swing.ButtonModel;
import java.rmi.RemoteException;

/**
 * A ClientInstanceMonitor
 */
public final class ClientInstanceMonitor {

  private final Event loggingStatusChangedEvent = Events.event();

  private final ClientInfo client;
  private final EntityConnectionServerAdmin server;
  private ButtonModel loggingEnabledButtonModel;

  public ClientInstanceMonitor(final ClientInfo client, final EntityConnectionServerAdmin server) {
    this.client = client;
    this.server = server;
  }

  public ButtonModel getLoggingEnabledButtonModel() {
    if (loggingEnabledButtonModel == null) {
      loggingEnabledButtonModel = ValueLinks.toggleValueLink(this, "loggingEnabled", loggingStatusChangedEvent);
    }

    return loggingEnabledButtonModel;
  }

  public long getCreationDate() throws RemoteException {
    final ClientLog log = server.getClientLog(client.getClientID());
    return log != null ? log.getConnectionCreationDate() : 0;
  }

  public ClientLog getLog() throws RemoteException {
    return server.getClientLog(client.getClientID());
  }

  public boolean isLoggingEnabled() throws RemoteException {
    return server.isLoggingEnabled(client.getClientID());
  }

  public void setLoggingEnabled(final boolean status) throws RemoteException {
    server.setLoggingEnabled(client.getClientID(), status);
    loggingStatusChangedEvent.fire();
  }

  public void disconnect() throws RemoteException {
    server.disconnect(client.getClientID());
  }

  @Override
  public String toString() {
    return client.toString();
  }
}
