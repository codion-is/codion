/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.sql.SQLException;

/**
 * Represents an exception coming from a database-layer
 */
public class DbException extends Exception  {

  /**
   * The query string being run when this exception occurred
   */
  private String sql;
  private Object userObject;

  public DbException(final String message) {
    super(message);
  }

  public DbException(final String message, final String sql) {
    super(message);
    this.sql = sql;
  }

  public DbException(final String message, final String sql, final Object userObject) {
    super(message);
    this.sql = sql;
    this.userObject = userObject;
  }

  /**
   * Constructs a new DbException instance
   * @param cause the Throwable cause of the exception
   */
  public DbException(final Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new DbException instance
   * @param message the exception message
   * @param cause the Throwable cause of the exception
   */
  public DbException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new DbException instance
   * @param cause the Throwable cause of the exception
   * @param sql the sql query which cause the exception
   */
  public DbException(final Throwable cause, final String sql) {
    super(cause);
    this.sql = sql;
  }

  /**
   * Constructs a new DbException instance
   * @param message the exception message
   * @param cause the Throwable cause of the exception
   * @param sql the sql query which cause the exception
   */
  public DbException(final String message, final Throwable cause, final String sql) {
    super(message, cause);
    this.sql = sql;
  }

  /**
   * @return the sql query which caused the exception
   */
  public String getSql() {
    return this.sql;
  }

  /**
   * @return the user object associated with this exception
   */
  public Object getUserObject() {
    return this.userObject;
  }

  /**
   * @return the error code in case the cause was a SQLException, -1 is returned otherwise
   */
  public int getErrorCode() {
    final Throwable cause = getCause();
    if (cause instanceof SQLException)
      return ((SQLException)cause).getErrorCode();

    return -1;
  }
}