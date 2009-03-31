/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.formats.ExactDateFormat;

import java.io.Serializable;
import java.util.Date;

/**
   * A class encapsulating a server log entry
 */
public class LogEntry implements Serializable, Comparable<LogEntry> {

  public String method;
  public String message;
  public long entryTime;
  public long exitTime;
  public long delta;
  public boolean done = false;

  public LogEntry() {
    this("", "", 0);
  }

  public LogEntry(final String method, final String message, final long time) {
    set(method, message, time);
  }

  public void set(final String method, final String message, final long time) {
    this.method = method;
    this.message = message;
    this.entryTime = time;
    this.exitTime = 0;
    this.delta = 0;
    this.done = false;
  }

  public int compareTo(final LogEntry entry) {
    if (this.entryTime < entry.entryTime)
      return -1;
    else if (this.entryTime > entry.entryTime)
      return 1;
    else
      return 0;
  }

  public String toString() {
    final StringBuffer ret = new StringBuffer();
    if (done) {
      ret.append(getEntryTimeFormatted()).append(" @ ").append(method).append(
              message != null && message.length() > 0 ? (": " + message) : "").append("\n");
      ret.append(getExitTimeFormatted()).append(" > ").append(delta).append(" ms").append("\n");
    }
    else {
      ret.append(getEntryTimeFormatted()).append(" @ ").append(method).append(
              message != null && message.length() > 0 ? (": " + message) : "").append("\n");
    }

    return ret.toString();
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
    this.done = true;

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
    return ExactDateFormat.get().format(entryTime);
  }

  /**
   * @return a formatted exit time
   */
  public String getExitTimeFormatted() {
    return ExactDateFormat.get().format(new Date(exitTime));
  }
}
