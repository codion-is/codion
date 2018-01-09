/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.sql.SQLException;

/**
 * Iterates through a ResultSet fetching instances of T.
 * Closes the ResultSet when iteration is finished or in case of an exception.
 * @param <T> the type to fetch from the result set
 */
public interface ResultIterator<T> extends AutoCloseable {

  /**
   * Returns true if a row is available in the underlying result set.
   * Calls {@link #close()} before returning false when iteration has been completed.
   * @return true if a row is available in the underlying result set
   * @throws SQLException in case of an exception
   */
  boolean hasNext() throws SQLException;

  /**
   * @return an instance of T fetched from the result set
   * @throws SQLException in case of an exception
   */
  T next() throws SQLException;

  /**
   * Closes the underlying result set and other resources held by this iterator
   */
  void close();
}