/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.pool.PoolableConnection;

import java.sql.SQLException;
import java.util.List;

public interface DbConnection extends PoolableConnection {

  List query(final String sql, final ResultPacker resultPacker, final int fetchCount) throws SQLException;

  boolean isConnected();

  List<List> queryObjects(final String sql, final int fetchCount) throws SQLException;

  int queryInteger(final String sql) throws SQLException, DbException;

  List<Integer> queryIntegers(final String sql) throws SQLException;

  List<String> queryStrings(final String sql) throws SQLException;
}