/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.Attribute;

/**
 * Describes a database column
 */
public interface Column extends Attribute {

  /**
   * @return the column name
   */
  String getColumnName();

  /**
   * @return the column data type as per java.sql.Types
   */
  int getType();

  /**
   * @return the index of this property in a select query
   */
  int getSelectIndex();

  /**
   * Specifies whether or not this column is updatable
   * @return true if this attribute is column
   */
  boolean isUpdatable();

  /**
   * @return false if this column should not be searchable
   */
  boolean isSearchable();

  /**
   * @return true if this column is a denormalized column
   */
  boolean isDenormalized();

  /**
   * @return true if this column has a default value
   */
  boolean columnHasDefaultValue();
}
