/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * A ResultPacker packs the contents of a ResultSet into a List
 */
public interface ResultPacker<T> extends Serializable {

  /**
   * Iterates through the given ResultSet, packing its contents into a List
   * @param resultSet the object containing the query result to process
   * @param fetchCount the number of records to retrieve from the result set,
   * a value of -1 means all should be retrieved
   * @return a List containing the data from the query result
   * @throws SQLException thrown if anyhing goes wrong during the packing
   */
  public List<T> pack(final ResultSet resultSet, final int fetchCount) throws SQLException;
}