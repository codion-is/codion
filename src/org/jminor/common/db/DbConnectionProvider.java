/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.User;

import java.sql.SQLException;

/**
 * Defines an interface responsible for creating DbConnection instances.
 * User: Bjorn Darri
 * Date: 28.3.2010
 * Time: 13:19:41
 */
public interface DbConnectionProvider {

  /**
   * Creates a new DbConnection instance based on the given user.
   * @param user the user
   * @return a new DbConnection instance
   * @throws ClassNotFoundException in case the JDBC driver class was not found
   * @throws SQLException in case of a database exception
   */
  DbConnection createConnection(final User user) throws ClassNotFoundException, SQLException;

  /**
   * Disconnects the given connection and disposes of any resources it holds.
   * @param connection the connection to destroy
   */
  void destroyConnection(final DbConnection connection);
}
