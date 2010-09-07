/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A cyclycal method logger.
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

  /**
   * Instantiates a new MethodLogger.
   * @param logSize the log size
   */
  public MethodLogger(final int logSize) {
    this(logSize, false);
  }

  /**
   * Instantiates a new MethodLogger.
   * @param logSize the log size
   * @param enabled true if this logger should be enabled
   */
  public MethodLogger(final int logSize, final boolean enabled) {
    this.logSize = logSize;
    this.logEntries = initializeLogEntryList();
    setEnabled(enabled);
  }

  /**
   * @return last access date
   */
  public final long getLastAccessDate() {
    return lastAccessDate;
  }

  /**
   * @return the last accessed method
   */
  public final String getLastAccessedMethod() {
    return lastAccessedMethod;
  }

  /**
   * @return the last access message
   */
  public final String getLastAccessMessage() {
    return lastAccessMessage;
  }

  /**
   * @return the last exit message
   */
  public final long getLastExitDate() {
    return lastExitDate;
  }

  /**
   * @return the last exited method
   */
  public final String getLastExitedMethod() {
    return lastExitedMethod;
  }

  /**
   * Resets this log
   */
  public final synchronized void reset() {
    for (final LogEntry entry : logEntries) {
      entry.reset();
    }
    currentLogEntryIndex = 0;
  }

  /**
   * @return the log entries
   */
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

  /**
   * @param method the method being accessed
   * @param arguments the method arguments
   */
  public final void logAccess(final String method, final Object[] arguments) {
    logAccess(method, arguments, System.currentTimeMillis());
  }

  /**
   * @param method the method being accessed
   * @param arguments the method arguments
   * @param timestamp the method access timestamp
   */
  public final void logAccess(final String method, final Object[] arguments, final long timestamp) {
    this.lastAccessDate = timestamp;
    this.lastAccessedMethod = method;
    if (enabled) {
      this.lastAccessMessage = argumentArrayToString(arguments);
      addLogEntry(lastAccessedMethod, lastAccessMessage, lastAccessDate, false, null, null);
    }
  }

  /**
   * @param method the method being exited
   * @param exception the exception, if any
   * @param subLog the sub-log, if any
   * @return the LogEntry
   */
  public final LogEntry logExit(final String method, final Throwable exception, final List<LogEntry> subLog) {
    return logExit(method, exception, System.currentTimeMillis(), subLog);
  }

  /**
   * @param method the method being exited
   * @param exception the exception, if any
   * @param timestamp the method exit timestamp
   * @param subLog the sub-log, if any
   * @return the LogEntry
   */
  public final LogEntry logExit(final String method, final Throwable exception, final long timestamp,
                                final List<LogEntry> subLog) {
    return logExit(method, exception, timestamp, subLog, null);
  }

  /**
   * @param method the method being exited
   * @param exception the exception, if any
   * @param timestamp the method exit timestamp
   * @param subLog the sub-log, if any
   * @param exitMessage the exit message
   * @return the LogEntry
   */
  public final LogEntry logExit(final String method, final Throwable exception, final long timestamp,
                                final List<LogEntry> subLog, final String exitMessage) {
    this.lastExitDate = timestamp;
    this.lastExitedMethod = method;
    if (enabled) {
      return addLogEntry(lastExitedMethod, exitMessage, lastExitDate, true, exception, subLog);
    }

    return null;
  }

  /**
   * @return true if this logger is enabled
   */
  public final boolean isEnabled() {
    return enabled;
  }

  /**
   * @param enabled true to enable this logger
   */
  public final void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    reset();
  }

  /**
   * @param argument the argument
   * @return a String representation of the given argument
   */
  protected String getMethodArgumentAsString(final Object argument) {
    return String.valueOf(argument);
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
      stringBuilder.append(getMethodArgumentAsString(arguments[i]));
      if (i < arguments.length-1) {
        stringBuilder.append(", ");
      }
    }

    return stringBuilder.toString();
  }
}
