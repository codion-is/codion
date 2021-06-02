/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.Util;
import is.codion.common.logging.MethodLogger;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.framework.server.EntityServerAdmin;

import javax.swing.ButtonModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.Color;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private final Value<String> searchStringValue = Value.value();
  private final Highlighter logHighlighter = new DefaultHighlighter();
  private final Highlighter.HighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
  private final Highlighter.HighlightPainter selectedHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.GREEN);
  private final List<MatchPosition> searchTextPositions = new ArrayList<>();
  private final Value<Integer> currentSearchTextPositionIndex = Value.value();
  private final Value<Integer> currentSearchTextPosition = Value.value();

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
    final ClientLog log = server.getClientLog(remoteClient.getClientId());

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
    final ClientLog log = server.getClientLog(remoteClient.getClientId());
    try {
      logDocument.remove(0, logDocument.getLength());
      logRootNode.removeAllChildren();
      if (log != null) {
        final StringBuilder logBuilder = new StringBuilder();
        for (final MethodLogger.Entry entry : log.getEntries()) {
          entry.append(logBuilder);
          final DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode(getEntryString(entry));
          if (entry.hasChildEntries()) {
            addChildEntries(entryNode, entry.getChildEntries());
          }
          logRootNode.add(entryNode);
        }
        logDocument.insertString(0, logBuilder.toString(), null);
        logTreeModel.setRoot(logRootNode);
        highlightSearchText();
      }
      else {
        logDocument.insertString(0, "Disconnected!", null);
      }
    }
    catch (final BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  public Highlighter getLogHighlighter() {
    return logHighlighter;
  }

  public Value<String> getSearchStringValue() {
    return searchStringValue;
  }

  public ValueObserver<Integer> getCurrentSearchTextPosition() {
    return currentSearchTextPosition.getObserver();
  }

  public Document getLogDocument() {
    return logDocument;
  }

  public void nextSearchPosition() {
    if (!searchTextPositions.isEmpty()) {
      clearCurrentSearchHighlight();
      if (currentSearchTextPositionIndex.isNull() || currentSearchTextPositionIndex.equalTo(searchTextPositions.size() - 1)) {
        currentSearchTextPositionIndex.set(0);
      }
      else {
        currentSearchTextPositionIndex.set(currentSearchTextPositionIndex.get() + 1);
      }
      setCurrentSearchHighlight();
    }
  }

  public void previousSearchPosition() {
    if (!searchTextPositions.isEmpty()) {
      clearCurrentSearchHighlight();
      if (currentSearchTextPositionIndex.isNull() || currentSearchTextPositionIndex.equalTo(0)) {
        currentSearchTextPositionIndex.set(searchTextPositions.size() - 1);
      }
      else {
        currentSearchTextPositionIndex.set(currentSearchTextPositionIndex.get() - 1);
      }
      setCurrentSearchHighlight();
    }
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

  private void highlightSearchText() {
    currentSearchTextPositionIndex.set(null);
    logHighlighter.removeAllHighlights();
    searchTextPositions.clear();
    if (!Util.nullOrEmpty(searchStringValue.get())) {
      final Pattern pattern = Pattern.compile(searchStringValue.get(), Pattern.CASE_INSENSITIVE);
      try {
        final Matcher matcher = pattern.matcher(logDocument.getText(0, logDocument.getLength()));
        int searchFrom = 0;
        while (matcher.find(searchFrom)) {
          final Object highlightTag = logHighlighter.addHighlight(matcher.start(), matcher.end(), highlightPainter);
          searchTextPositions.add(new MatchPosition(matcher.start(), matcher.end(), highlightTag));
          searchFrom = matcher.end();
        }
        nextSearchPosition();
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void setCurrentSearchHighlight() {
    final MatchPosition matchPosition = searchTextPositions.get(currentSearchTextPositionIndex.get());
    currentSearchTextPosition.set(matchPosition.start);
    try {
      logHighlighter.removeHighlight(matchPosition.highlightTag);
      matchPosition.highlightTag = logHighlighter.addHighlight(matchPosition.start, matchPosition.end, selectedHighlightPainter);
    }
    catch (final BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  private void clearCurrentSearchHighlight() {
    if (currentSearchTextPositionIndex.isNotNull()) {
      final MatchPosition matchPosition = searchTextPositions.get(currentSearchTextPositionIndex.get());
      try {
        logHighlighter.removeHighlight(matchPosition.highlightTag);
        matchPosition.highlightTag = logHighlighter.addHighlight(matchPosition.start, matchPosition.end, highlightPainter);
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void bindEvents() {
    loggingEnabledValue.addDataListener(this::setLoggingEnabled);
    searchStringValue.addListener(this::highlightSearchText);
  }

  private static void addChildEntries(final DefaultMutableTreeNode entryNode, final List<MethodLogger.Entry> childEntries) {
    for (final MethodLogger.Entry entry : childEntries) {
      final DefaultMutableTreeNode subEntry = new DefaultMutableTreeNode(getEntryString(entry));
      if (entry.hasChildEntries()) {
        addChildEntries(subEntry, entry.getChildEntries());
      }
      entryNode.add(subEntry);
    }
  }

  private static String getEntryString(final MethodLogger.Entry entry) {
    final StringBuilder builder = new StringBuilder(entry.getMethod()).append(" [")
            .append(MICROSECOND_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(entry.getDuration())))
            .append(" μs").append("]");
    if (entry.getAccessMessage() != null) {
      builder.append(": ").append(entry.getAccessMessage()).toString();
    }

    return builder.toString();
  }

  private static final class MatchPosition {

    private final int start;
    private final int end;

    private Object highlightTag;

    private MatchPosition(final int start, final int end, final Object highlightTag) {
      this.start = start;
      this.end = end;
      this.highlightTag = highlightTag;
    }
  }
}
