/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.result;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A ResultPacker packs the contents of a ResultSet into a List.
 * @param <T> the type of object resulting from the packing
 */
public interface ResultPacker<T> {

  /**
   * Iterates through the given ResultSet, packing its contents into a List using {@link #get(ResultSet)} in the order they appear.
   * Items are skipped if {@link #get(ResultSet)} returns null.
   * This method does not close or modify the ResultSet in any way.
   * @param resultSet the ResultSet instance containing the query result to process
   * @return a List containing the data from the query result
   * @throws SQLException thrown if anything goes wrong during the packing
   * @throws NullPointerException in case resultSet is null
   */
  default List<T> pack(ResultSet resultSet) throws SQLException {
    requireNonNull(resultSet, "resultSet");
    List<T> result = new ArrayList<>();
    while (resultSet.next()) {
      T item = get(resultSet);
      if (item != null) {
        result.add(item);
      }
    }

    return result;
  }

  /**
   * Fetches a single instance from the given result set, assuming {@link ResultSet#next()} has been called
   * @param resultSet the result set
   * @return the instance fetched from the ResultSet
   * @throws SQLException in case of failure
   */
  T get(ResultSet resultSet) throws SQLException;
}