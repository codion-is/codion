/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.formats.ExactDateFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A class encapsulating a simple collection of db access log entries and basic connection access info
 * User: Björn Darri
 * Date: 7.12.2007
 * Time: 12:07:44
 */
public class DbLog implements Serializable {

  public final Date logCreationDate = new Date();

  public final List<LogEntry> log;

  private final String connectionKey;

  public final long lastAccessDate;
  public final long lastExitDate;
  public final String lastAccessedMethod;
  public final String lastAccessMessage;
  public final String lastExitedMethod;
  public final long connectionCreationDate;

  public DbLog(final String connectionKey, final long connectionCreationDate, final List<LogEntry> log,
               final long lastAccessDate, final long lastExitDate, final String lastAccessedMethod,
               final String lastAccessedMessage, final String lastExitedMethod) {
    this.connectionKey = connectionKey;
    this.connectionCreationDate = connectionCreationDate;
    this.log = log == null ? new ArrayList<LogEntry>(0) : log;
    this.lastAccessDate = lastAccessDate;
    this.lastExitDate = lastExitDate;
    this.lastAccessedMethod = lastAccessedMethod;
    this.lastAccessMessage = lastAccessedMessage;
    this.lastExitedMethod = lastExitedMethod;
  }

  /**
   * @return Value for property 'connectionKey'.
   */
  public String getConnectionKey() {
    return connectionKey;
  }

  /**
   * @return Value for property 'creationDate'.
   */
  public long getConnectionCreationDate() {
    return connectionCreationDate;
  }

  /**
   * @return Value for property 'lastExitedMethod'.
   */
  public String getLastExitedMethod() {
    return lastExitedMethod;
  }

  /**
   * @return Value for property 'lastAccessedMethod'.
   */
  public String getLastAccessedMethod() {
    return lastAccessedMethod;
  }

  /**
   * @return Value for property 'lastAccessMessage'.
   */
  public String getLastAccessMessage() {
    return lastAccessMessage;
  }

  /**
   * @return Value for property 'lastAccessDate'.
   */
  public long getLastAccessDate() {
    return lastAccessDate;
  }

  /**
   * @return Value for property 'lastExitDate'.
   */
  public long getLastExitDate() {
    return lastExitDate;
  }

  /**
   * @return Value for property 'timeSinceLastAccess'.
   */
  public long getTimeSinceLastAccess() {
    return System.currentTimeMillis() - getLastAccessDate();
  }

  /**
   * @return Value for property 'lastDelta'.
   */
  public long getLastDelta() {
    return getLastExitDate() - getLastAccessDate();
  }

  /**
   * @return Value for property 'lastAccessDateFormatted'.
   */
  public String getLastAccessDateFormatted() {
    return ExactDateFormat.get().format(getLastAccessDate());
  }

  /**
   * @return Value for property 'lastExitDateFormatted'.
   */
  public String getLastExitDateFormatted() {
    return ExactDateFormat.get().format(getLastExitDate());
  }

  /** {@inheritDoc} */
  public boolean equals(Object obj) {
    return this == obj || !((obj == null) || (obj.getClass() != this.getClass()))
            && connectionKey.equals(((DbLog) obj).connectionKey);
  }

  /** {@inheritDoc} */
  public int hashCode() {
    return connectionKey.hashCode();
  }
}
