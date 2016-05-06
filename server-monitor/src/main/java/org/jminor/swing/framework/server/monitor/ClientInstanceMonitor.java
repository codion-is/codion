/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.tools.MethodLogger;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.framework.server.EntityConnectionServerAdmin;
import org.jminor.swing.common.ui.ValueLinks;

import javax.swing.ButtonModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A ClientInstanceMonitor
 */
public final class ClientInstanceMonitor {

  private static final NumberFormat MICROSECOND_FORMAT = NumberFormat.getIntegerInstance();

  private final Event<Boolean> loggingStatusChangedEvent = Events.event();

  private final ClientInfo clientInfo;
  private final EntityConnectionServerAdmin server;
  private final DefaultMutableTreeNode logRootNode = new DefaultMutableTreeNode();
  private final DefaultTreeModel logTreeModel = new DefaultTreeModel(logRootNode);
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

  public void refreshLogTreeModel() throws RemoteException {
    final ClientLog log = server.getClientLog(clientInfo.getClientID());
    logRootNode.removeAllChildren();
    if (log != null) {
      for (final MethodLogger.Entry entry : log.getEntries()) {
        final DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode(getEntryString(entry));
        if (entry.containsSubLog()) {
          addSubLog(entryNode, entry.getSubLog());
        }
        logRootNode.add(entryNode);
      }
    }
    logTreeModel.setRoot(logRootNode);
  }

  private void addSubLog(final DefaultMutableTreeNode entryNode, final List<MethodLogger.Entry> subLog) {
    for (final MethodLogger.Entry entry : subLog) {
      final DefaultMutableTreeNode subEntry = new DefaultMutableTreeNode(getEntryString(entry));
      if (entry.containsSubLog()) {
        addSubLog(subEntry, entry.getSubLog());
      }
      entryNode.add(subEntry);
    }
  }

  private String getEntryString(final MethodLogger.Entry entry) {
    return new StringBuilder(entry.getMethod()).append(" [")
                .append(MICROSECOND_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(entry.getDeltaNano())))
                .append(" μs").append("]").append(": ").append(entry.getAccessMessage()).toString();
  }

  public DefaultTreeModel getLogTreeModel() {
    return logTreeModel;
  }

  @Override
  public String toString() {
    return clientInfo.toString();
  }
}
