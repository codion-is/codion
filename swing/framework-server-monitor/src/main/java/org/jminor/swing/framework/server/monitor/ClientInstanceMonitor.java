/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.common.MethodLogger;
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.RemoteClient;
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

  private final RemoteClient remoteClient;
  private final EntityConnectionServerAdmin server;
  private final DefaultMutableTreeNode logRootNode = new DefaultMutableTreeNode();
  private final DefaultTreeModel logTreeModel = new DefaultTreeModel(logRootNode);
  private ButtonModel loggingEnabledButtonModel;

  /**
   * Instantiates a new {@link ClientInstanceMonitor}, monitoring the given client
   * @param server the server being monitored
   * @param remoteClient the client info
   */
  public ClientInstanceMonitor(final EntityConnectionServerAdmin server, final RemoteClient remoteClient) {
    this.remoteClient = remoteClient;
    this.server = server;
  }

  /**
   * @return the {@link RemoteClient}
   */
  public RemoteClient getRemoteClient() {
    return remoteClient;
  }

  /**
   * @return the {@link ButtonModel} for controlling whether logging is enabled
   */
  public ButtonModel getLoggingEnabledButtonModel() {
    if (loggingEnabledButtonModel == null) {
      loggingEnabledButtonModel = ValueLinks.toggleValueLink(this, "loggingEnabled", loggingStatusChangedEvent);
    }

    return loggingEnabledButtonModel;
  }

  /**
   * @return the creation date of the client connection
   * @throws RemoteException in case of an exception
   */
  public long getCreationDate() throws RemoteException {
    final ClientLog log = server.getClientLog(remoteClient.getClientId());
    return log != null ? log.getConnectionCreationDate() : 0;
  }

  /**
   * @return the client log
   * @throws RemoteException in case of an exception
   */
  public ClientLog getLog() throws RemoteException {
    return server.getClientLog(remoteClient.getClientId());
  }

  /**
   * @return true if logging is enabled for this client
   * @throws RemoteException in case of an exception
   */
  public boolean isLoggingEnabled() throws RemoteException {
    return server.isLoggingEnabled(remoteClient.getClientId());
  }

  /**
   * @param status true if logging should be enabled, false otherwise
   * @throws RemoteException in case of an exception
   */
  public void setLoggingEnabled(final boolean status) throws RemoteException {
    server.setLoggingEnabled(remoteClient.getClientId(), status);
    loggingStatusChangedEvent.fire(status);
  }

  /**
   * Disconnects the client from the server
   * @throws RemoteException in case of an exception
   */
  public void disconnect() throws RemoteException {
    server.disconnect(remoteClient.getClientId());
  }

  /**
   * Refreshes the log tree model with the most recent log from the server
   * @throws RemoteException in case of an exception
   */
  public void refreshLogTreeModel() throws RemoteException {
    final ClientLog log = server.getClientLog(remoteClient.getClientId());
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

  /**
   * @return the TreeModel for displaying the log in a Tree view
   */
  public DefaultTreeModel getLogTreeModel() {
    return logTreeModel;
  }

  @Override
  public String toString() {
    return remoteClient.toString();
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
                .append(MICROSECOND_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(entry.getDuration())))
                .append(" μs").append("]").append(": ").append(entry.getAccessMessage()).toString();
  }
}
