/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * A ResultPacker packs the contents of a ResultSet into a List.
 * @param <T> the type of object resulting from the packing
 */
public interface ResultPacker<T> {

  /**
   * Iterates through the given ResultSet, packing its contents into a List
   * @param resultSet the object containing the query result to process
   * @param fetchCount the number of records to fetch from the result set,
   * a negative value means all should be fetched.
   * @return a List containing the data from the query result
   * @throws SQLException thrown if anything goes wrong during the packing
   */
  List<T> pack(final ResultSet resultSet, final int fetchCount) throws SQLException;
}