/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.formats.ExactTimestampFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A class encapsulating a simple collection of server access log entries and basic connection access info
 * User: Björn Darri
 * Date: 7.12.2007
 * Time: 12:07:44
 */
public class ServerLog implements Serializable {

  private static final long serialVersionUID = 1;

  public final Date logCreationDate = new Date();

  public final List<ServerLogEntry> log;

  private final String connectionKey;

  public final long lastAccessDate;
  public final long lastExitDate;
  public final String lastAccessedMethod;
  public final String lastAccessMessage;
  public final String lastExitedMethod;
  public final long connectionCreationDate;

  public ServerLog(final String connectionKey, final long connectionCreationDate, final List<ServerLogEntry> log,
               final long lastAccessDate, final long lastExitDate, final String lastAccessedMethod,
               final String lastAccessedMessage, final String lastExitedMethod) {
    this.connectionKey = connectionKey;
    this.connectionCreationDate = connectionCreationDate;
    this.log = log == null ? new ArrayList<ServerLogEntry>(0) : log;
    this.lastAccessDate = lastAccessDate;
    this.lastExitDate = lastExitDate;
    this.lastAccessedMethod = lastAccessedMethod;
    this.lastAccessMessage = lastAccessedMessage;
    this.lastExitedMethod = lastExitedMethod;
  }

  /**
   * @return the connection key identifying this log
   */
  public String getConnectionKey() {
    return connectionKey;
  }

  /**
   * @return the log creation date
   */
  public long getConnectionCreationDate() {
    return connectionCreationDate;
  }

  /**
   * @return the name of the last exited method
   */
  public String getLastExitedMethod() {
    return lastExitedMethod;
  }

  /**
   * @return the name of the last accessed method
   */
  public String getLastAccessedMethod() {
    return lastAccessedMethod;
  }

  /**
   * @return the message from the last access
   */
  public String getLastAccessMessage() {
    return lastAccessMessage;
  }

  /**
   * @return the last access date
   */
  public long getLastAccessDate() {
    return lastAccessDate;
  }

  /**
   * @return the last exit date
   */
  public long getLastExitDate() {
    return lastExitDate;
  }

  /**
   * @return the time since last access
   */
  public long getTimeSinceLastAccess() {
    return System.currentTimeMillis() - getLastAccessDate();
  }

  /**
   * @return the duration of the last method call
   */
  public long getLastDelta() {
    return getLastExitDate() - getLastAccessDate();
  }

  /**
   * @return a formatted last access date
   */
  public String getLastAccessDateFormatted() {
    return ExactTimestampFormat.get().format(getLastAccessDate());
  }

  /**
   * @return a formatted last exit date
   */
  public String getLastExitDateFormatted() {
    return ExactTimestampFormat.get().format(getLastExitDate());
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object object) {
    return this == object || !((object == null) || (object.getClass() != this.getClass()))
            && connectionKey.equals(((ServerLog) object).connectionKey);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return connectionKey.hashCode();
  }
}
