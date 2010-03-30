/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.formats.DateFormats;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;

/**
   * A class encapsulating a server log entry
 */
public class ServerLogEntry implements Serializable, Comparable<ServerLogEntry> {

  private static final long serialVersionUID = 1;
  private static final DateFormat TIMESTAMP_FORMAT = DateFormats.getDateFormat(DateFormats.EXACT_TIMESTAMP);

  private String method;
  private String message;
  private long entryTime;
  private long exitTime;
  private long delta;
  private String stackTrace;

  public ServerLogEntry() {
    this("", "", 0, null);
  }

  public ServerLogEntry(final String method, final String message, final long time, final Throwable exception) {
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

  public int compareTo(final ServerLogEntry entry) {
    if (this.entryTime < entry.entryTime)
      return -1;
    else if (this.entryTime > entry.entryTime)
      return 1;
    else
      return 0;
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    if (exitTime > 0) {
      stringBuilder.append(getEntryTimeFormatted()).append(" @ ").append(method).append(
              message != null && message.length() > 0 ? (": " + message) : "").append("\n");
      stringBuilder.append(getExitTimeFormatted()).append(" > ").append(delta).append(" ms").append("\n");
      if (stackTrace != null)
        stringBuilder.append(stackTrace);
    }
    else {
      stringBuilder.append(getEntryTimeFormatted()).append(" @ ").append(method).append(
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

  public ServerLogEntry setException(final Throwable exception) {
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

  private String getStackTrace(final Throwable exception) {
    if (exception == null)
      return null;

    final StringWriter sw = new StringWriter();
    exception.printStackTrace(new PrintWriter(sw));

    return sw.toString();
  }
}
