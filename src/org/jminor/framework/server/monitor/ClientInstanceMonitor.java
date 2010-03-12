/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.common.ui.control.ToggleBeanPropertyLink;
import org.jminor.framework.server.EntityDbServerAdmin;

import javax.swing.ButtonModel;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 18:22:24
 */
public class ClientInstanceMonitor {

  public final Event evtLogginStatusChanged = new Event();

  private final ClientInfo client;
  private final EntityDbServerAdmin server;
  private final ButtonModel loggingEnabledButtonModel;

  public ClientInstanceMonitor(final ClientInfo client, final EntityDbServerAdmin server) {
    this.client = client;
    this.server = server;
    loggingEnabledButtonModel = new ToggleBeanPropertyLink(this, "loggingOn", evtLogginStatusChanged, null).getButtonModel();
  }

  public ButtonModel getLoggingEnabledButtonModel() {
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

  @Override
  public String toString() {
    return client.toString();
  }
}
