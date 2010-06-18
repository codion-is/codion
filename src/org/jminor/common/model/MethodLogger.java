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
  private boolean enabled = false;
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

  public long getLastAccessDate() {
    return lastAccessDate;
  }

  public String getLastAccessedMethod() {
    return lastAccessedMethod;
  }

  public String getLastAccessMessage() {
    return lastAccessMessage;
  }

  public long getLastExitDate() {
    return lastExitDate;
  }

  public String getLastExitedMethod() {
    return lastExitedMethod;
  }

  public synchronized void reset() {
    for (final LogEntry entry : logEntries) {
      entry.reset();
    }
    currentLogEntryIndex = 0;
  }

  public synchronized List<LogEntry> getLogEntries() {
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

  public void logAccess(final String method, final Object[] arguments) {
    this.lastAccessDate = System.currentTimeMillis();
    this.lastAccessedMethod = method;
    if (enabled) {
      this.lastAccessMessage = argumentArrayToString(arguments);
      addLogEntry(lastAccessedMethod, lastAccessMessage, lastAccessDate, false, null, null);
    }
  }

  public long logExit(final String method, final Throwable exception, final List<LogEntry> subLog) {
    return logExit(method, exception, subLog, null);
  }

  public long logExit(final String method, final Throwable exception, final List<LogEntry> subLog,
                      final String exitMessage) {
    this.lastExitDate = System.currentTimeMillis();
    this.lastExitedMethod = method;
    if (enabled) {
      return addLogEntry(lastExitedMethod, exitMessage, lastExitDate, true, exception, subLog);
    }

    return -1;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    reset();
  }

  private synchronized long addLogEntry(final String method, final String message, final long time, final boolean isExit,
                                        final Throwable exception, final List<LogEntry> subLog) {
    if (!isExit) {
      if (currentLogEntryIndex > logEntries.size()-1) {
        currentLogEntryIndex = 0;
      }

      logEntries.get(currentLogEntryIndex).set(method, message, time, exception);

      return -1;
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
    final List<LogEntry> logEntries = new ArrayList<LogEntry>(logSize);
    for (int i = 0; i < logSize; i++) {
      logEntries.add(new LogEntry());
    }

    return logEntries;
  }

  protected String argumentArrayToString(final Object[] arguments) {
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
