/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.sql.SQLException;

/**
 * Represents an exception coming from a database-layer
 */
public class DbException extends Exception  {

  private String sql;
  private String table;
  private TableDependencies dependencyInfo;
  private boolean isDeleteException = false;
  private Object userObject;

  public DbException(final String message) {
    super(message);
  }

  public DbException(final String message, final String sql) {
    super(message);
    this.sql = sql;
  }

  public DbException(final String message, final String table, final boolean isDelete) {
    super(message);
    this.table = table;
    this.isDeleteException = isDelete;
  }

  public DbException(final String message, final String sql, final Object userObject) {
    super(message);
    this.isDeleteException = true;
    this.sql = sql;
    this.userObject = userObject;
  }

  public DbException(final String message, final String sql, final TableDependencies deps) {
    super(message);
    this.isDeleteException = true;
    this.sql = sql;
    this.dependencyInfo = deps;
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

  public DbException(final Throwable cause, final String table, final boolean isDelete) {
    super(cause);
    this.table = table;
    this.isDeleteException = isDelete;
  }

  public DbException(final Throwable cause, final String sql, final Object userObject) {
    super(cause);
    this.isDeleteException = true;
    this.sql = sql;
    this.userObject = userObject;
  }

  public DbException(final Throwable cause, final String sql, final TableDependencies deps) {
    super(cause);
    this.isDeleteException = true;
    this.sql = sql;
    this.dependencyInfo = deps;
  }

  /**
   * @return the sql query which caused the exception
   */
  public String getSql() {
    return this.sql;
  }

  /**
   * @return Value for property 'entity'.
   */
  public Object getUserObject() {
    return this.userObject;
  }

  /**
   * @return Value for property 'table'.
   */
  public String getTable() {
    return table;
  }

  /**
   * @return Value for property 'deleteException'.
   */
  public boolean isDeleteException() {
    return isDeleteException;
  }

  /**
   * @return Value for property 'insertNullValueException'.
   */
  public boolean isInsertNullValueException() {
    return getORAErrorCode() == DbUtil.NULL_VALUE_ERR_CODE;
  }

  /**
   * @return Value for property 'tableDependencyInfo'.
   */
  public TableDependencies getTableDependencyInfo() {
    return this.dependencyInfo;
  }

  /**
   * @return the name of the column which triggered the "cannot insert NULL into" exception
   */
  public String getNullErrorColumnName() {
    if (getORAErrorCode() == DbUtil.NULL_VALUE_ERR_CODE) {
      final String errorMsg = getCause().getMessage();

      return errorMsg.substring(errorMsg.lastIndexOf('.')+2, errorMsg.lastIndexOf(')')-1);
    }

    return null;
  }

  /**
   * @return the ORACLE error code in case the cause was a SQLException, -1 is returned otherwise
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