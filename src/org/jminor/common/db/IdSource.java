/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * A enum representing the possible ways of retrieving a new  ID value
 * User: Björn Darri
 * Date: 12.1.2008
 * Time: 20:42:34
 */
public enum IdSource {
  /**
   * the id value is set automatically from a sequence (e.g. by trigger) or is automatically incremented
   */
  AUTO_INCREMENT,
  /**
   * the id value is derived from the max id value in the table
   */
  MAX_PLUS_ONE,
  /**
   * the id is set manually or can be disregarded
   */
  NONE,
  /**
   * the id value should be selected from a sequence
   */
  SEQUENCE,
  /**
   * the id source is a query
   */
  QUERY;

  public boolean isQueried() {
    return this == MAX_PLUS_ONE || this == SEQUENCE || this == QUERY;
  }

  public boolean isAutoIncrement() {
    return this == AUTO_INCREMENT;
  }
}
