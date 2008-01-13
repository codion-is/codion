/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * A IResultPacker packs the contents of a ResultSet into a List
 */
public interface IResultPacker<T> extends Serializable {

  /**
   * Iterates through the given ResultSet, packing its contents into a List
   * @param resultSet the object containing the query result to process
   * @return a List containing the data from the query result
   * @throws SQLException thrown if anyhing goes wrong during the execution
   */
  public List<? extends T> pack(final ResultSet resultSet) throws SQLException;
}