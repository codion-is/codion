/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.MethodLogger;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.value.Value;
import is.codion.common.value.Values;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.swing.common.ui.value.BooleanValues;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A ClientInstanceMonitor
 */
public final class ClientInstanceMonitor {

  private static final NumberFormat MICROSECOND_FORMAT = NumberFormat.getIntegerInstance();

  private final RemoteClient remoteClient;
  private final EntityServerAdmin server;
  private final Value<Boolean> loggingEnabledValue;
  private final DefaultMutableTreeNode logRootNode = new DefaultMutableTreeNode();
  private final DefaultTreeModel logTreeModel = new DefaultTreeModel(logRootNode);
  private final ButtonModel loggingEnabledButtonModel = new JToggleButton.ToggleButtonModel();

  /**
   * Instantiates a new {@link ClientInstanceMonitor}, monitoring the given client
   * @param server the server being monitored
   * @param remoteClient the client info
   * @throws RemoteException in case of an exception
   */
  public ClientInstanceMonitor(final EntityServerAdmin server, final RemoteClient remoteClient) throws RemoteException {
    this.remoteClient = remoteClient;
    this.server = server;
    this.loggingEnabledValue = Values.value(server.isLoggingEnabled(remoteClient.getClientId()));
    BooleanValues.booleanButtonModelValue(loggingEnabledButtonModel).link(loggingEnabledValue);
    bindEvents();
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
    return loggingEnabledButtonModel;
  }

  /**
   * @return the creation date of the client connection
   * @throws RemoteException in case of an exception
   */
  public LocalDateTime getCreationDate() throws RemoteException {
    final ClientLog log = server.getClientLog(remoteClient.getClientId());
    return log == null ? null : log.getConnectionCreationDate() ;
  }

  /**
   * @return the client log
   * @throws RemoteException in case of an exception
   */
  public ClientLog getLog() throws RemoteException {
    return server.getClientLog(remoteClient.getClientId());
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

  /**
   * @param status true if logging should be enabled, false otherwise
   */
  private void setLoggingEnabled(final boolean status) {
    try {
      server.setLoggingEnabled(remoteClient.getClientId(), status);
    }
    catch (final RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  private void bindEvents() {
    loggingEnabledValue.addDataListener(this::setLoggingEnabled);
  }

  private static void addSubLog(final DefaultMutableTreeNode entryNode, final List<MethodLogger.Entry> subLog) {
    for (final MethodLogger.Entry entry : subLog) {
      final DefaultMutableTreeNode subEntry = new DefaultMutableTreeNode(getEntryString(entry));
      if (entry.containsSubLog()) {
        addSubLog(subEntry, entry.getSubLog());
      }
      entryNode.add(subEntry);
    }
  }

  private static String getEntryString(final MethodLogger.Entry entry) {
    return new StringBuilder(entry.getMethod()).append(" [")
            .append(MICROSECOND_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(entry.getDuration())))
            .append(" μs").append("]").append(": ").append(entry.getAccessMessage()).toString();
  }
}
