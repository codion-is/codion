/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * Converts to and from SQL values, such as integers being used
 * to represent booleans in a database.
 * @param <T> the type of the value
 * @param <C> the type of the underlying column
 */
public interface ValueConverter<T, C> {

  /**
   * Translates the given value into a sql value, usually this is not required
   * but for certain types this may be necessary, such as boolean values where
   * the values are represented by a non-boolean data type in the underlying database
   * @param value the value to translate
   * @return the sql value used to represent the given value
   */
  C toColumnValue(final T value);

  /**
   * @param columnValue the SQL value to translate from
   * @return the value of SQL {@code columnValue}
   */
  T fromColumnValue(final C columnValue);
}
