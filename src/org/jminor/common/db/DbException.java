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
   * @param sql the sql query which cause the exception
   */
  public DbException(final Throwable cause, final String sql) {
    super(cause);
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
   * @return true if this exception represents a 'insertNullValueException'.
   */
  //todo oracle specific
  public boolean isInsertNullValueException() {
    return getORAErrorCode() == DbUtil.ORA_NULL_VALUE_ERR_CODE;
  }

  /**
   * @return the name of the column which triggered the "cannot insert NULL into" exception
   */
  //todo oracle specific
  public String getNullErrorColumnName() {
    if (getORAErrorCode() == DbUtil.ORA_NULL_VALUE_ERR_CODE) {
      final String errorMsg = getCause().getMessage();

      return errorMsg.substring(errorMsg.lastIndexOf('.')+2, errorMsg.lastIndexOf(')')-1);
    }

    return null;
  }

  /**
   * @return the error code in case the cause was a SQLException, -1 is returned otherwise
   */
  public int getORAErrorCode() {
    final Throwable cause = getCause();
    if (cause instanceof SQLException)
      return ((SQLException)cause).getErrorCode();

    return -1;
  }

  /**
   * @return the message assigned to the given ORACLE error
   * code in case the cause was a SQLException, null is returned otherwise
   */
  public String getORAErrorMessage() {
    int err = getORAErrorCode();
    if (err != -1)
      return DbUtil.oracleSqlErrorCodes.get(Integer.valueOf(err));
    else
      return null;
  }
}