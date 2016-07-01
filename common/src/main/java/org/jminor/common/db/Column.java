/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * Describes a database column
 */
public interface Column extends Attribute {

  /**
   * @return the column name
   */
  String getColumnName();

  /**
   * @return the column data type
   * @see java.sql.Types
   */
  int getType();

  /**
   * Indicates whether or not this column is updatable
   * @return true if this column is updatable
   */
  boolean isUpdatable();

  /**
   * @return true if this column can be used in search condition, or where clauses
   */
  boolean isSearchable();
}
