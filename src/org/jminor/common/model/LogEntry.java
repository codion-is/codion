/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.model.formats.DateFormats;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A class encapsulating a log entry for logging method calls.
 */
public final class LogEntry implements Serializable, Comparable<LogEntry> {

  private static final long serialVersionUID = 1;
  private static final ThreadLocal<DateFormat> TIMESTAMP_FORMAT = DateUtil.getThreadLocalDateFormat(DateFormats.EXACT_TIMESTAMP);

  private String method;
  private String entryMessage;
  private String exitMessage;
  private long entryTime;
  private long exitTime;
  private long delta;
  private String stackTrace;
  private List<LogEntry> subLog;

  /**
   * Instantiates a new empty log entry.
   */
  public LogEntry() {
    this("", "", 0, null);
  }

  /**
   * A copy constructor
   * @param entry the log entry to copy
   */
  public LogEntry(final LogEntry entry) {
    this.method = entry.method;
    this.entryMessage = entry.entryMessage;
    this.exitMessage = entry.exitMessage;
    this.entryTime = entry.entryTime;
    this.exitTime = entry.exitTime;
    this.delta = entry.delta;
    this.stackTrace = entry.stackTrace;
    if (entry.subLog != null) {
      this.subLog = new ArrayList<LogEntry>();
      for (final LogEntry subEntry : entry.subLog) {
        this.subLog.add(new LogEntry(subEntry));
      }
    }
  }

  /**
   * Initializes a new LogEntry instance
   * @param method the method being logged
   * @param entryMessage a message describing for example the method arguments
   * @param time the time at which to log the event
   * @param exception the exception thrown by the method execution if any
   */
  public LogEntry(final String method, final String entryMessage, final long time, final Throwable exception) {
    set(method, entryMessage, time, exception);
  }

  /**
   * Initializes this LogEntry instance
   * @param method the method being logged
   * @param entryMessage a message describing for example the method arguments
   * @param time the time at which to log the event
   * @param exception the exception thrown by the method execution if any
   */
  public void set(final String method, final String entryMessage, final long time, final Throwable exception) {
    this.method = method;
    this.entryMessage = entryMessage;
    this.entryTime = time;
    this.exitTime = 0;
    this.delta = 0;
    setException(exception);
  }

  /**
   * Clears all info from this entry
   */
  public void reset() {
    set(null, null, 0, null);
    this.exitMessage = null;
    this.subLog = null;
  }

  /**
   * @return the time this entry represents
   */
  public long getEntryTime() {
    return entryTime;
  }

  /**
   * @param exitTime the exit time
   * @return the difference between the given exit time and the entry time
   */
  public LogEntry setExitTime(final long exitTime) {
    this.exitTime = exitTime;
    this.delta = this.exitTime - this.entryTime;

    return this;
  }

  /**
   * @return the exit time
   */
  public long getExitTime() {
    return exitTime;
  }

  /**
   * @return the duration of the method call this entry represents
   */
  public long getDelta() {
    return delta;
  }

  /**
   * @return the entry message
   */
  public String getEntryMessage() {
    return entryMessage;
  }

  /**
   * @return the method name
   */
  public String getMethod() {
    return method;
  }

  /**
   * @return the stack trace, if any
   */
  public String getStackTrace() {
    return stackTrace;
  }

  /**
   * @param message the exit message
   * @return this log entry
   */
  public LogEntry setExitMessage(final String message) {
    this.exitMessage = message;
    return this;
  }

  /**
   * @return the exit message
   */
  public String getExitMessage() {
    return exitMessage;
  }

  /**
   * @param exception the exception
   * @return this log entry
   */
  public LogEntry setException(final Throwable exception) {
    if (exception == null) {
      this.stackTrace = null;
    }
    else {
      this.stackTrace = getStackTrace(exception);
    }
    return this;
  }

  /**
   * @return a formatted entry time
   */
  public String getEntryTimeFormatted() {
    return TIMESTAMP_FORMAT.get().format(entryTime);
  }

  /**
   * @return a formatted exit time
   */
  public String getExitTimeFormatted() {
    return TIMESTAMP_FORMAT.get().format(new Date(exitTime));
  }

  /**
   * @return the sub log, if any
   */
  public List<LogEntry> getSubLog() {
    return subLog;
  }

  /**
   * @param subLog the sub log
   */
  public void setSubLog(final List<LogEntry> subLog) {
    this.subLog = subLog;
  }

  /**
   * @return true if this entry is complete, that is, has an exit time
   */
  public boolean isComplete() {
    return exitTime > 0;
  }

  /** {@inheritDoc} */
  public int compareTo(final LogEntry o) {
    if (this.entryTime < o.entryTime) {
      return -1;
    }
    else if (this.entryTime > o.entryTime) {
      return 1;
    }
    else {
      return 0;
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object obj) {
    return obj instanceof LogEntry && this.entryTime == ((LogEntry) obj).entryTime;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Long.valueOf(this.entryTime).hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return toString(0);
  }

  /**
   * Returns a string representation of this log entry.
   * @param indentation the number of tab indents to prefix the string with
   * @return a string representation of this log entry
   */
  public String toString(final int indentation) {
    final String indentString = indentation > 0 ? Util.padString("", indentation, '\t', false) : "";
    final StringBuilder stringBuilder = new StringBuilder();
    if (exitTime > 0) {
      stringBuilder.append(indentString).append(getEntryTimeFormatted()).append(" @ ").append(method).append(
              !Util.nullOrEmpty(entryMessage) ? (": " + entryMessage) : "").append("\n");
      stringBuilder.append(indentString).append(getExitTimeFormatted()).append(" > ").append(delta).append(" ms ")
              .append(exitMessage == null ? "" : " (" + exitMessage + ")");
      if (stackTrace != null) {
        stringBuilder.append(stackTrace);
      }
    }
    else {
      stringBuilder.append(indentString).append(getEntryTimeFormatted()).append(" @ ").append(method).append(
              !Util.nullOrEmpty(entryMessage) ? (": " + entryMessage) : "");
    }

    return stringBuilder.toString();
  }

  private static String getStackTrace(final Throwable exception) {
    final StringWriter sw = new StringWriter();
    exception.printStackTrace(new PrintWriter(sw));

    return sw.toString();
  }
}
