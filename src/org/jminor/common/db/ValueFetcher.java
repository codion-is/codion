/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Fetches a single value from a result set
 */
public interface ValueFetcher {

  /**
   * Fetches a single value from a ResultSet
   * @param resultSet the ResultSet
   * @return a single value fetched from the given ResultSet
   * @throws java.sql.SQLException in case of an exception
   */
  Object fetchValue(final ResultSet resultSet) throws SQLException;
}
