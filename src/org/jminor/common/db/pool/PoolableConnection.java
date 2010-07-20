/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * User: Björn Darri
 * Date: 18.7.2010
 * Time: 19:49:49
 */
public interface PoolableConnection {

  Connection getConnection();

  long getPoolTime();

  void setPoolTime(final long time);

  void setPoolRetryCount(final int retryCount);

  boolean isConnectionValid();

  void beginTransaction();

  boolean isTransactionOpen();

  void commitTransaction() throws SQLException;

  void rollbackTransaction() throws SQLException;

  void commit() throws SQLException;

  void rollback() throws SQLException;

  void disconnect();
}
