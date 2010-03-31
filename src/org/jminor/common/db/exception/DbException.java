/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

import java.sql.SQLException;

/**
 * An exception coming from a database-layer.
 */
public class DbException extends Exception  {

  /**
   * The sql statement being run when this exception occurred
   */
  private String statement;

  /**
   * Constructs a new DbException instance
   * @param message the exception message
   */
  public DbException(final String message) {
    super(message);
  }

  /**
   * Constructs a new DbException instance
   * @param message the exception message
   * @param statement the sql statement which caused the exception
   */
  public DbException(final String message, final String statement) {
    super(message);
    this.statement = statement;
  }

  /**
   * Constructs a new DbException instance
   * @param cause the cause of the exception
   * @param statement the sql statement which caused the exception
   * @param message the exception message
   */
  public DbException(final SQLException cause, final String statement, final String message) {
    super(message, cause);
    this.statement = statement;
  }

  /**
   * @return the sql query which caused the exception
   */
  public String getStatement() {
    return this.statement;
  }
}