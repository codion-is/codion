/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.formats.DateFormats;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A class encapsulating a simple collection of server access log entries and basic connection access info.
 * User: Bjorn Darri
 * Date: 7.12.2007
 * Time: 12:07:44
 */
public class ServerLog implements Serializable {

  private static final long serialVersionUID = 1;

  private final Date logCreationDate = new Date();
  private final List<ServerLogEntry> log;
  private final String connectionKey;
  private final long lastAccessDate;
  private final long lastExitDate;
  private final String lastAccessedMethod;
  private final String lastAccessMessage;
  private final String lastExitedMethod;
  private final long connectionCreationDate;

  private static final DateFormat TIMESTAMP_FORMAT = DateFormats.getDateFormat(DateFormats.EXACT_TIMESTAMP);

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
   * @return the log entry list
   */
  public List<ServerLogEntry> getLog() {
    return log;
  }

  /**
   * @return the date this log was created
   */
  public Date getLogCreationDate() {
    return logCreationDate;
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
    return TIMESTAMP_FORMAT.format(getLastAccessDate());
  }

  /**
   * @return a formatted last exit date
   */
  public String getLastExitDateFormatted() {
    return TIMESTAMP_FORMAT.format(getLastExitDate());
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
