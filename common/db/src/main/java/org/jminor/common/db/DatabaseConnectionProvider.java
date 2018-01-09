/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;

/**
 * Defines an interface responsible for creating and disposing of DatabaseConnection instances.
 */
public interface DatabaseConnectionProvider {

  /**
   * @return the underlying {@link Database} implementation
   */
  Database getDatabase();

  /**
   * Creates a new DatabaseConnection instance based on the given user.
   * @return a new DatabaseConnection instance
   * @throws DatabaseException in case of a database exception
   */
  DatabaseConnection createConnection() throws DatabaseException;

  /**
   * Disconnects the given connection and disposes of any resources it holds.
   * @param connection the connection to destroy
   */
  void destroyConnection(final DatabaseConnection connection);

  /**
   * @return the User this connection provider is based on
   */
  User getUser();
}
