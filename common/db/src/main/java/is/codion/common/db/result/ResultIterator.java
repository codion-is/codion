/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.db.result;

import java.sql.SQLException;
import java.util.Iterator;

/**
 * Iterates through a ResultSet fetching instances of T.
 * Use try with resources or remember to call {@link #close()} in order to close underlying resources.
 * @param <T> the type to fetch from the result set
 */
public interface ResultIterator<T> extends AutoCloseable {

  /**
   * Returns true if a row is available in the underlying result set.
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