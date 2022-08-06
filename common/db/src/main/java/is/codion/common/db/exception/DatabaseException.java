/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

import java.sql.SQLException;

/**
 * An exception coming from a database-layer.
 */
public class DatabaseException extends Exception {

  /**
   * The sql statement being run when this exception occurred, if any, transient
   * so that it's not available client side if running in a server/client environment
   */
  private final transient String statement;

  /**
   * The underlying error code, if any, transient so that it's not
   * available client side if running in a server/client environment
   */
  private final transient int errorCode;

  /**
   * Constructs a new DatabaseException instance
   * @param message the exception message
   */
  public DatabaseException(String message) {
    this(message, null);
  }

  /**
   * Constructs a new DatabaseException instance
   * @param message the exception message
   * @param statement the sql statement which caused the exception
   */
  public DatabaseException(String message, String statement) {
    super(message);
    this.statement = statement;
    this.errorCode = -1;
  }

  /**
   * Constructs a new DatabaseException instance
   * @param cause the root cause, the stack trace is copied and used
   */
  public DatabaseException(SQLException cause) {
    this(cause, cause.getMessage());
  }

  /**
   * Constructs a new DatabaseException instance
   * @param cause the root cause, the stack trace is copied and used
   * @param message the exception message
   */
  public DatabaseException(SQLException cause, String message) {
    this(cause, message, null);
  }

  /**
   * Constructs a new DatabaseException instance
   * @param cause the root cause, the stack trace is copied and used
   * @param message the exception message
   * @param statement the sql statement which caused the exception
   */
  public DatabaseException(SQLException cause, String message, String statement) {
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
  public final String statement() {
    return this.statement;
  }

  /**
   * Returns the underlying error code, note that this is only available when running with
   * a local database connection.
   * @return the underlying error code, -1 if not available
   */
  public final int errorCode() {
    return errorCode;
  }

  @Override
  public final void setStackTrace(StackTraceElement[] stackTrace) {
    super.setStackTrace(stackTrace);
  }
}