/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * Converts to and from SQL values, such as integers being used
 * to represent booleans in a database
 */
public interface ValueConverter {

  /**
   * Translates the given value into a sql value, usually this is not required
   * but for certain types this may be necessary, such as boolean values where
   * the values are represented by a non-boolean data type in the underlying database
   * @param value the value to translate
   * @return the sql value used to represent the given value
   */
  Object toColumnValue(final Object value);

  /**
   * @param columnValue the SQL value to translate from
   * @return the value of SQL <code>columnValue</code>
   */
  Object fromColumnValue(final Object columnValue);
}
