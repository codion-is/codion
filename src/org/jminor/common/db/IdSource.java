/*
 * Copyright (c) 2008, Your Corporation. All Rights Reserved.
 */

package org.jminor.common.db;

/**
 * User: Björn Darri
 * Date: 12.1.2008
 * Time: 20:42:34
 */
public enum IdSource {
  /**
   * the id value is set automatically from a sequnce or is automatically incremented
   */
  ID_AUTO_INCREMENT,
  /**
   * the id value is derived from the max id value in the table
   */
  ID_MAX_PLUS_ONE,
  /**
   * the source of the id value can be safely disregarded
   */
  ID_NONE,
  /**
   * the id value should be selected from a sequence
   */
  ID_SEQUENCE,
  /**
   * the id value should be queried
   */
  ID_QUERY
}
