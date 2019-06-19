/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

import java.sql.SQLException;

/**
 * An exception coming from a database-layer.
 */
public class DatabaseException extends Exception {

  /**
   * The sql statement being run when this exception occurred, if any, transient
   * so it's not available client side if running in a server/client environment
   */
  private final transient String statement;

  /**
   * The underlying error code, if any, transient so it's not
   * available client side if running in a server/client environment
   */
  private final transient int errorCode;

  /**
   * Constructs a new DatabaseException instance
   * @param message the exception message
   */
  public DatabaseException(final String message) {
    this(message, null);
  }

  /**
   * Constructs a new DatabaseException instance
   * @param message the exception message
   * @param statement the sql statement which caused the exception
   */
  public DatabaseException(final String message, final String statement) {
    super(message);
    this.statement = statement;
    this.errorCode = -1;
  }

  /**
   * Constructs a new DatabaseException instance
   * @param cause the root cause, the stack trace is copied and used
   * @param message the exception message
   */
  public DatabaseException(final SQLException cause, final String message) {
    this(cause, message, null);
  }

  /**
   * Constructs a new DatabaseException instance
   * @param cause the root cause, the stack trace is copied and used
   * @param message the exception message
   * @param statement the sql statement which caused the exception
   */
  public DatabaseException(final SQLException cause, final String message, final String statement) {
    super(message);
    this.statement = statement;
    if (cause != null) {
      errorCode = cause.getErrorCode();
      setStackTrace(cause.getStackTrace());
    }
    else {
      errorCode = -1;
    }
  }

  /**
   * Returns the sql statement causing this exception, if available, note that this is only
   * available when running with a local database connection.
   * @return the sql query which caused the exception, null if not applicable
   */
  public final String getStatement() {
    return this.statement;
  }

  /**
   * Returns the underlying error code, note that this is only available when running with
   * a local database connection.
   * @return the underlying error code, -1 if not available
   */
  public final int getErrorCode() {
    return errorCode;
  }
}