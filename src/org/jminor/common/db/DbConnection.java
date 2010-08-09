/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.pool.PoolableConnection;
import org.jminor.common.model.User;

import java.sql.SQLException;
import java.util.List;

public interface DbConnection extends PoolableConnection {

  /**
   * @return the connection user
   */
  User getUser();

  /**
   * @param enabled true to enable logging on this connection, false to disable
   */
  void setLoggingEnabled(final boolean enabled);

  /**
   * @return true if logging is enabled, false otherwise
   */
  boolean isLoggingEnabled();

  /**
   * Performs the given sql query and returns the result in a List
   * @param sql the query
   * @param resultPacker a ResultPacker instance for creating the return List
   * @param fetchCount the number of records to retrieve, use -1 to retrieve all
   * @return the query result in a List
   * @throws SQLException thrown if anything goes wrong during the query execution
   */
  List query(final String sql, final ResultPacker resultPacker, final int fetchCount) throws SQLException;

  /**
   * @return true if the connection is connected
   */
  boolean isConnected();

  /**
   * @param sql the query
   * @param fetchCount the maximum number of records to return, -1 for all
   * @return the result of this query, in a List of rows represented as Lists
   * @throws SQLException thrown if anything goes wrong during the query execution
   */
  List<List> queryObjects(final String sql, final int fetchCount) throws SQLException;

  /**
   * Performs the given query and returns the result as an integer
   * @param sql the query must select at least a single number column, any other
   * subsequent columns are disregarded
   * @return the first record in the result as a integer
   * @throws SQLException thrown if anything goes wrong during the execution
   * @throws org.jminor.common.db.exception.DbException thrown if no record is found
   */
  int queryInteger(final String sql) throws SQLException, DbException;

  /**
   * Performs the given query and returns the result as a List of Integers
   * @param sql the query, it must select at least a single number column, any other
   * subsequent columns are disregarded
   * @return a List of Integers representing the query result
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  List<Integer> queryIntegers(final String sql) throws SQLException;

  /**
   * Performs the given query and returns the result as a List of Strings
   * @param sql the query, it must select at least a single string column, any other
   * subsequent columns are disregarded
   * @return a List of Strings representing the query result
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  List<String> queryStrings(final String sql) throws SQLException;
}