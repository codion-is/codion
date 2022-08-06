/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import java.sql.Connection;

/**
 * Provides new {@link Connection} instances.
 */
public interface ConnectionFactory {

  /**
   * Returns the database url for this connection factory.
   * @return the database url for this connection factory.
   */
  String url();

  /**
   * Creates a connection for the given user.
   * @param user the user for which to create a connection
   * @return a new JDBC connection
   * @throws DatabaseException in case of a connection error
   * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
   */
  Connection createConnection(User user) throws DatabaseException;

  /**
   * Validates the given connection.
   * @param connection the connection to validate
   * @return true if the connection is valid
   */
  boolean isConnectionValid(Connection connection);
}
