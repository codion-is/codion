/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.result;

import java.sql.SQLException;
import java.util.Iterator;

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
   * @throws java.util.NoSuchElementException in case no more rows are available
   */
  T next() throws SQLException;

  /**
   * Closes the underlying result set and other resources held by this iterator
   */
  void close();

  /**
   * Wraps this {@link ResultIterator} in a {@link Iterator}. Any SQLExceptions
   * that occur are rethrown as RuntimeExceptions.
   * @return a {@link Iterator} instance based on this {@link ResultIterator}
   */
  default Iterator<T> iterator() {
    return new DefaultIterator<>(this);
  }
}