/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * A ClientInstanceMonitor
 */
public final class ClientInstanceMonitor {

  private static final NumberFormat MICROSECOND_FORMAT = NumberFormat.getIntegerInstance();

  private final RemoteClient remoteClient;
  private final EntityServerAdmin server;
  private final State loggingEnabled;
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
    this.remoteClient = requireNonNull(remoteClient);
    this.server = requireNonNull(server);
    this.loggingEnabled = State.state(server.isLoggingEnabled(remoteClient.clientId()));
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
  public State loggingEnabled() {
    return loggingEnabled;
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
          entry.appendTo(logBuilder);
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
    loggingEnabled.addDataListener(this::setLoggingEnabled);
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
    if (entry.enterMessage() != null) {
      builder.append(": ").append(entry.enterMessage());
    }

    return builder.toString();
  }
}
