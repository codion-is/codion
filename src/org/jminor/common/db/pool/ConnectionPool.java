/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.User;

import java.sql.SQLException;

/**
 * User: Björn Darri
 * Date: 28.3.2010
 * Time: 13:06:20
 */
public interface ConnectionPool {
  void checkInConnection(final DbConnection dbConnection);
  DbConnection checkOutConnection() throws ClassNotFoundException, SQLException;
  ConnectionPoolSettings getConnectionPoolSettings();
  void setConnectionPoolSettings(final ConnectionPoolSettings settings);
  User getUser();
  ConnectionPoolStatistics getConnectionPoolStatistics(final long since);
  void resetPoolStatistics();
  boolean isCollectFineGrainedStatistics();
  void setCollectFineGrainedStatistics(final boolean value);
}
