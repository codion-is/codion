/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.db.exception;

import java.sql.SQLException;

/**
 * An exception coming from a database-layer.
 */
public class DatabaseException extends Exception {

  private static final long serialVersionUID = 1483224395022566393L;

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
   * The underlying sql state, if any, transient so that it's not
   * available client side if running in a server/client environment
   */
  private final transient String sqlState;

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
    this.sqlState = null;
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
      sqlState = cause.getSQLState();
      setStackTrace(cause.getStackTrace());
    }
    else {
      errorCode = -1;
      sqlState = null;
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

  /**
   * Returns the underlying sql state, note that this is only available when running with
   * a local database connection.
   * @return the underlying sql state, null if not available
   */
  public final String sqlState() {
    return sqlState;
  }

  @Override
  public final void setStackTrace(StackTraceElement[] stackTrace) {
    super.setStackTrace(stackTrace);
  }
}