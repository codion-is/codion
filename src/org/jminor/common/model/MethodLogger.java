/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for logging method calls.<br>
 * User: Björn Darri<br>
 * Date: 24.4.2010<br>
 * Time: 10:10:55<br>
 */
public class MethodLogger {

  private int logSize;
  private volatile boolean enabled = false;
  private List<LogEntry> logEntries;
  private int currentLogEntryIndex = 0;

  private long lastAccessDate = System.currentTimeMillis();
  private long lastExitDate = System.currentTimeMillis();
  private String lastAccessedMethod;
  private String lastAccessMessage;
  private String lastExitedMethod;

  public MethodLogger(final int logSize) {
    this(logSize, false);
  }

  public MethodLogger(final int logSize, final boolean enabled) {
    this.logSize = logSize;
    this.logEntries = initializeLogEntryList();
    setEnabled(enabled);
  }

  public final long getLastAccessDate() {
    return lastAccessDate;
  }

  public final String getLastAccessedMethod() {
    return lastAccessedMethod;
  }

  public final String getLastAccessMessage() {
    return lastAccessMessage;
  }

  public final long getLastExitDate() {
    return lastExitDate;
  }

  public final String getLastExitedMethod() {
    return lastExitedMethod;
  }

  public final synchronized void reset() {
    for (final LogEntry entry : logEntries) {
      entry.reset();
    }
    currentLogEntryIndex = 0;
  }

  public final synchronized List<LogEntry> getLogEntries() {
    final ArrayList<LogEntry> entries = new ArrayList<LogEntry>();
    if (!enabled) {
      entries.add(new LogEntry("Logging is not enabled", "", System.currentTimeMillis(), null));
    }
    else {
      for (final LogEntry entry : logEntries) {
        if (entry.isComplete()) {
          entries.add(new LogEntry(entry));
        }
      }
    }

    return entries;
  }

  public final void logAccess(final String method, final Object[] arguments) {
    this.lastAccessDate = System.currentTimeMillis();
    this.lastAccessedMethod = method;
    if (enabled) {
      this.lastAccessMessage = argumentArrayToString(arguments);
      addLogEntry(lastAccessedMethod, lastAccessMessage, lastAccessDate, false, null, null);
    }
  }

  public final LogEntry logExit(final String method, final Throwable exception, final List<LogEntry> subLog) {
    return logExit(method, exception, subLog, null);
  }

  public final LogEntry logExit(final String method, final Throwable exception, final List<LogEntry> subLog,
                          final String exitMessage) {
    this.lastExitDate = System.currentTimeMillis();
    this.lastExitedMethod = method;
    if (enabled) {
      return addLogEntry(lastExitedMethod, exitMessage, lastExitDate, true, exception, subLog);
    }

    return null;
  }

  public final boolean isEnabled() {
    return enabled;
  }

  public final void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    reset();
  }

  private synchronized LogEntry addLogEntry(final String method, final String message, final long time, final boolean isExit,
                                            final Throwable exception, final List<LogEntry> subLog) {
    if (!isExit) {
      if (currentLogEntryIndex > logEntries.size()-1) {
        currentLogEntryIndex = 0;
      }

      final LogEntry entry = logEntries.get(currentLogEntryIndex);
      entry.set(method, message, time, exception);

      return entry;
    }
    else {//add to last log entry
      final LogEntry lastEntry = logEntries.get(currentLogEntryIndex);
      assert lastEntry.getMethod().equals(method);
      lastEntry.setSubLog(subLog);
      currentLogEntryIndex++;
      return lastEntry.setException(exception).setExitMessage(message).setExitTime(time);
    }
  }

  private List<LogEntry> initializeLogEntryList() {
    final List<LogEntry> entries = new ArrayList<LogEntry>(logSize);
    for (int i = 0; i < logSize; i++) {
      entries.add(new LogEntry());
    }

    return entries;
  }

  protected final String argumentArrayToString(final Object[] arguments) {
    if (arguments == null) {
      return "";
    }

    final StringBuilder stringBuilder = new StringBuilder(arguments.length*40);
    for (int i = 0; i < arguments.length; i++) {
      appendArgumentAsString(arguments[i], stringBuilder);
      if (i < arguments.length-1) {
        stringBuilder.append(", ");
      }
    }

    return stringBuilder.toString();
  }

  protected void appendArgumentAsString(final Object argument, final StringBuilder destination) {
    destination.append(String.valueOf(argument));
  }
}
