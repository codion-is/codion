/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;

import java.sql.Connection;

/**
 * Provides {@link Connection} instances.
 */
public interface ConnectionProvider {

  /**
   * Returns the database url for this connection provider.
   * @return the database url for this connection provider
   */
  String getUrl();

  /**
   * Creates a connection for the given user.
   * @param user the user for which to create a connection
   * @return a Connection
   * @throws DatabaseException in case of a connection error
   * @throws org.jminor.common.db.exception.AuthenticationException in case of an authentication error
   */
  Connection createConnection(User user) throws DatabaseException;

  /**
   * Validates the given connection.
   * @param connection the connection to validate
   * @return true if the connection is valid
   */
  boolean isConnectionValid(Connection connection);
}
