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
 * A class encapsulating a call log entry.
 */
public class LogEntry implements Serializable, Comparable<LogEntry> {

  private static final long serialVersionUID = 1;
  private static final DateFormat TIMESTAMP_FORMAT = DateFormats.getDateFormat(DateFormats.EXACT_TIMESTAMP);

  private String method;
  private String message;
  private String exitMessage;
  private long entryTime;
  private long exitTime;
  private long delta;
  private String stackTrace;
  private List<LogEntry> subLog;

  public LogEntry() {
    this("", "", 0, null);
  }

  public LogEntry(final LogEntry entry) {
    this.method = entry.method;
    this.message = entry.message;
    this.exitMessage = entry.exitMessage;
    this.entryTime = entry.entryTime;
    this.exitTime = entry.exitTime;
    this.delta = entry.delta;
    this.stackTrace = entry.stackTrace;
    if (entry.subLog != null) {
      this.subLog = new ArrayList<LogEntry>();
      for (final LogEntry subEntry : entry.subLog)
        this.subLog.add(new LogEntry(subEntry));
    }
  }

  public LogEntry(final String method, final String message, final long time, final Throwable exception) {
    set(method, message, time, exception);
  }

  public void set(final String method, final String message, final long time, final Throwable exception) {
    this.method = method;
    this.message = message;
    this.entryTime = time;
    this.exitTime = 0;
    this.delta = 0;
    setException(exception);
  }

  public void reset() {
    set(null, null, 0, null);
  }

  public long getEntryTime() {
    return entryTime;
  }

  public long getExitTime() {
    return exitTime;
  }

  public String getMessage() {
    return message;
  }

  public String getMethod() {
    return method;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public int compareTo(final LogEntry entry) {
    if (this.entryTime < entry.entryTime)
      return -1;
    else if (this.entryTime > entry.entryTime)
      return 1;
    else
      return 0;
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof LogEntry && this.entryTime == ((LogEntry) obj).entryTime;
  }

  @Override
  public int hashCode() {
    return Long.valueOf(this.entryTime).hashCode();
  }

  @Override
  public String toString() {
    return toString(0);
  }

  public String toString(final int indentation) {
    final String indentString = indentation > 0 ? Util.padString("", indentation, '\t', false) : "";
    final StringBuilder stringBuilder = new StringBuilder();
    if (exitTime > 0) {
      stringBuilder.append(indentString).append(getEntryTimeFormatted()).append(" @ ").append(method).append(
              message != null && message.length() > 0 ? (": " + message) : "").append("\n");
      stringBuilder.append(indentString).append(getExitTimeFormatted()).append(" > ").append(delta).append(" ms")
              .append(exitMessage == null ? "" : " (" + exitMessage + ")").append("\n");
      if (stackTrace != null)
        stringBuilder.append(stackTrace);
    }
    else {
      stringBuilder.append(indentString).append(getEntryTimeFormatted()).append(" @ ").append(method).append(
              message != null && message.length() > 0 ? (": " + message) : "").append("\n");
    }

    return stringBuilder.toString();
  }

  /**
   * @return the log entry key
   */
  public String getEntryKey() {
    return method + (message != null && message.length() > 0 ? ": " + message : "");
  }

  /**
   * @param exitTime the exit time
   * @return the difference between the given exit time and the entry time
   */
  public long setExitTime(final long exitTime) {
    this.exitTime = exitTime;
    this.delta = this.exitTime - this.entryTime;

    return delta;
  }

  public LogEntry setExitMessage(final String message) {
    this.exitMessage = message;
    return this;
  }

  public LogEntry setException(final Throwable exception) {
    this.stackTrace = getStackTrace(exception);
    return this;
  }

  /**
   * @return the duration of the method call this entry represents
   */
  public long getDelta() {
    return delta;
  }

  /**
   * @return a formatted entry time
   */
  public String getEntryTimeFormatted() {
    return TIMESTAMP_FORMAT.format(entryTime);
  }

  /**
   * @return a formatted exit time
   */
  public String getExitTimeFormatted() {
    return TIMESTAMP_FORMAT.format(new Date(exitTime));
  }

  public List<LogEntry> getSubLog() {
    return subLog;
  }

  public void setSubLog(List<LogEntry> subLog) {
    this.subLog = subLog;
  }

  public boolean isValid() {
    return exitTime > 0;
  }

  private String getStackTrace(final Throwable exception) {
    if (exception == null)
      return null;

    final StringWriter sw = new StringWriter();
    exception.printStackTrace(new PrintWriter(sw));

    return sw.toString();
  }
}
