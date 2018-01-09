/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A ResultPacker packs the contents of a ResultSet into a List.
 * @param <T> the type of object resulting from the packing
 */
public interface ResultPacker<T> {

  /**
   * Iterates through the given ResultSet, packing its contents into a List using {@link #fetch(ResultSet)} in the order they appear.
   * Items are skipped if {@link #fetch(ResultSet)} returns null.
   * This method does not close or modify the ResultSet in any way.
   * @param resultSet the ResultSet instance containing the query result to process
   * @param fetchCount the maximum number of records to fetch from the result set,
   * a negative value means all should be fetched.
   * @return a List containing the data from the query result
   * @throws SQLException thrown if anything goes wrong during the packing
   * @throws NullPointerException in case resultSet is null
   */
  default List<T> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
    Objects.requireNonNull(resultSet, "resultSet");
    final List<T> result = fetchCount > 0 ? new ArrayList<>(fetchCount) : new ArrayList<>();
    int counter = 0;
    while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
      final T item = fetch(resultSet);
      if (item != null) {
        result.add(fetch(resultSet));
      }
    }

    return result;
  }

  /**
   * Fetches a single instance from the given result set, assumes {@link ResultSet#next()} has been called
   * @param resultSet the result set
   * @return the instance fetched from the ResultSet, null if the item should not be fetched for some reason
   * @throws SQLException in case of failure
   */
  T fetch(final ResultSet resultSet) throws SQLException;
}