/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.formats.ExactTimestampFormat;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

/**
   * A class encapsulating a server log entry
 */
public class ServerLogEntry implements Serializable, Comparable<ServerLogEntry> {

  private static final long serialVersionUID = 1;
  private static final DateFormat TIMESTAMP_FORMAT = new ExactTimestampFormat();

  public String method;
  public String message;
  public long entryTime;
  public long exitTime;
  public long delta;

  public ServerLogEntry() {
    this("", "", 0);
  }

  public ServerLogEntry(final String method, final String message, final long time) {
    set(method, message, time);
  }

  public void set(final String method, final String message, final long time) {
    this.method = method;
    this.message = message;
    this.entryTime = time;
    this.exitTime = 0;
    this.delta = 0;
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
}
