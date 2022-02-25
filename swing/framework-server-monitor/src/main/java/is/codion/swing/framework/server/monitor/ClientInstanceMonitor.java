/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.logging.MethodLogger;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.value.Value;
import is.codion.framework.server.EntityServerAdmin;

import javax.swing.ButtonModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
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
  private final StyledDocument logDocument = new DefaultStyledDocument();
  private final DefaultMutableTreeNode logRootNode = new DefaultMutableTreeNode();
  private final DefaultTreeModel logTreeModel = new DefaultTreeModel(logRootNode);

  /**
   * Instantiates a new {@link ClientInstanceMonitor}, monitoring the given client
   * @param server the server being monitored
   * @param remoteClient the client info
   * @throws RemoteException in case of an exception
   */
  public ClientInstanceMonitor(final EntityServerAdmin server, final RemoteClient remoteClient) throws RemoteException {
    this.remoteClient = remoteClient;
    this.server = server;
    this.loggingEnabledValue = Value.value(server.isLoggingEnabled(remoteClient.getClientId()));
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
  public Value<Boolean> getLoggingEnabledValue() {
    return loggingEnabledValue;
  }

  /**
   * @return the creation date of the client connection
   * @throws RemoteException in case of an exception
   */
  public LocalDateTime getCreationDate() throws RemoteException {
    ClientLog log = server.getClientLog(remoteClient.getClientId());

    return log == null ? null : log.getConnectionCreationDate();
  }

  /**
   * Disconnects the client from the server
   * @throws RemoteException in case of an exception
   */
  public void disconnect() throws RemoteException {
    server.disconnect(remoteClient.getClientId());
  }

  /**
   * Refreshes the log document and tree model with the most recent log from the server
   * @throws RemoteException in case of an exception
   */
  public void refreshLog() throws RemoteException {
    ClientLog log = server.getClientLog(remoteClient.getClientId());
    try {
      logDocument.remove(0, logDocument.getLength());
      logRootNode.removeAllChildren();
      if (log != null) {
        StringBuilder logBuilder = new StringBuilder();
        for (final MethodLogger.Entry entry : log.getEntries()) {
          entry.append(logBuilder);
          DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode(getEntryString(entry));
          if (entry.hasChildEntries()) {
            addChildEntries(entryNode, entry.getChildEntries());
          }
          logRootNode.add(entryNode);
        }
        logDocument.insertString(0, logBuilder.toString(), null);
        logTreeModel.setRoot(logRootNode);
      }
      else {
        logDocument.insertString(0, "Disconnected!", null);
      }
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  public Document getLogDocument() {
    return logDocument;
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
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  private void bindEvents() {
    loggingEnabledValue.addDataListener(this::setLoggingEnabled);
  }

  private static void addChildEntries(final DefaultMutableTreeNode entryNode, final List<MethodLogger.Entry> childEntries) {
    for (final MethodLogger.Entry entry : childEntries) {
      DefaultMutableTreeNode subEntry = new DefaultMutableTreeNode(getEntryString(entry));
      if (entry.hasChildEntries()) {
        addChildEntries(subEntry, entry.getChildEntries());
      }
      entryNode.add(subEntry);
    }
  }

  private static String getEntryString(final MethodLogger.Entry entry) {
    StringBuilder builder = new StringBuilder(entry.getMethod()).append(" [")
            .append(MICROSECOND_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(entry.getDuration())))
            .append(" μs").append("]");
    if (entry.getAccessMessage() != null) {
      builder.append(": ").append(entry.getAccessMessage()).toString();
    }

    return builder.toString();
  }
}
