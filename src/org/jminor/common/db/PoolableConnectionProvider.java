/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.User;

import java.sql.SQLException;

/**
 * Defines an interface responsible for creating PoolableConnection instances.
 */
public interface PoolableConnectionProvider {

  /**
   * Creates a new PoolableConnection instance based on the given user.
   * @param user the user
   * @return a new PoolableConnection instance
   * @throws ClassNotFoundException in case the JDBC driver class was not found
   * @throws SQLException in case of a database exception
   */
  PoolableConnection createConnection(final User user) throws ClassNotFoundException, SQLException;

  /**
   * Disconnects the given connection and disposes of any resources it holds.
   * @param connection the connection to destroy
   */
  void destroyConnection(final PoolableConnection connection);
}
