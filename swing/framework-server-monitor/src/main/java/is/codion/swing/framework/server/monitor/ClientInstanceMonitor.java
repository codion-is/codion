/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.logging.MethodLogger;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.state.State;
import is.codion.framework.server.EntityServerAdmin;

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
  private final State loggingEnabledState;
  private final StyledDocument logDocument = new DefaultStyledDocument();
  private final DefaultMutableTreeNode logRootNode = new DefaultMutableTreeNode();
  private final DefaultTreeModel logTreeModel = new DefaultTreeModel(logRootNode);

  /**
   * Instantiates a new {@link ClientInstanceMonitor}, monitoring the given client
   * @param server the server being monitored
   * @param remoteClient the client info
   * @throws RemoteException in case of an exception
   */
  public ClientInstanceMonitor(EntityServerAdmin server, RemoteClient remoteClient) throws RemoteException {
    this.remoteClient = remoteClient;
    this.server = server;
    this.loggingEnabledState = State.state(server.isLoggingEnabled(remoteClient.clientId()));
    bindEvents();
  }

  /**
   * @return the {@link RemoteClient}
   */
  public RemoteClient remoteClient() {
    return remoteClient;
  }

  /**
   * @return the {@link State} for controlling whether logging is enabled
   */
  public State loggingEnabledState() {
    return loggingEnabledState;
  }

  /**
   * @return the creation date of the client connection
   * @throws RemoteException in case of an exception
   */
  public LocalDateTime creationDate() throws RemoteException {
    ClientLog log = server.clientLog(remoteClient.clientId());

    return log == null ? null : log.connectionCreationDate();
  }

  /**
   * Disconnects the client from the server
   * @throws RemoteException in case of an exception
   */
  public void disconnect() throws RemoteException {
    server.disconnect(remoteClient.clientId());
  }

  /**
   * Refreshes the log document and tree model with the most recent log from the server
   * @throws RemoteException in case of an exception
   */
  public void refreshLog() throws RemoteException {
    ClientLog log = server.clientLog(remoteClient.clientId());
    try {
      logDocument.remove(0, logDocument.getLength());
      logRootNode.removeAllChildren();
      if (log != null) {
        StringBuilder logBuilder = new StringBuilder();
        for (MethodLogger.Entry entry : log.entries()) {
          entry.append(logBuilder);
          DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode(entryString(entry));
          if (entry.hasChildEntries()) {
            addChildEntries(entryNode, entry.childEntries());
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

  public Document logDocument() {
    return logDocument;
  }

  /**
   * @return the TreeModel for displaying the log in a Tree view
   */
  public DefaultTreeModel logTreeModel() {
    return logTreeModel;
  }

  @Override
  public String toString() {
    return remoteClient.toString();
  }

  /**
   * @param status true if logging should be enabled, false otherwise
   */
  private void setLoggingEnabled(boolean status) {
    try {
      server.setLoggingEnabled(remoteClient.clientId(), status);
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  private void bindEvents() {
    loggingEnabledState.addDataListener(this::setLoggingEnabled);
  }

  private static void addChildEntries(DefaultMutableTreeNode entryNode, List<MethodLogger.Entry> childEntries) {
    for (MethodLogger.Entry entry : childEntries) {
      DefaultMutableTreeNode subEntry = new DefaultMutableTreeNode(entryString(entry));
      if (entry.hasChildEntries()) {
        addChildEntries(subEntry, entry.childEntries());
      }
      entryNode.add(subEntry);
    }
  }

  private static String entryString(MethodLogger.Entry entry) {
    StringBuilder builder = new StringBuilder(entry.method()).append(" [")
            .append(MICROSECOND_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(entry.duration())))
            .append(" μs").append("]");
    if (entry.accessMessage() != null) {
      builder.append(": ").append(entry.accessMessage()).toString();
    }

    return builder.toString();
  }
}
