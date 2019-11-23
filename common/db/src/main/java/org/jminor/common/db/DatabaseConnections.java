/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;

import java.sql.Connection;

/**
 * A factory class for DatabaseConnections
 */
public final class DatabaseConnections {

  private DatabaseConnections() {}

  /**
   * Constructs a new DatabaseConnection instance, based on the given Database and User
   * @param database the database
   * @param user the user for the db-connection
   * @return a new DatabaseConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   */
  public static DatabaseConnection createConnection(final Database database, final User user) throws DatabaseException {
    return createConnection(database, user, 0);
  }

  /**
   * Constructs a new DatabaseConnection instance, based on the given Database and User
   * @param database the database
   * @param user the user for the db-connection
   * @param validityCheckTimeout the number of seconds specified when checking if the connection is valid
   * @return a new DatabaseConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws org.jminor.common.db.exception.AuthenticationException in case of an authentication error
   */
  public static DatabaseConnection createConnection(final Database database, final User user,
                                                    final int validityCheckTimeout) throws DatabaseException {
    return new DefaultDatabaseConnection(database, user, validityCheckTimeout);
  }

  /**
   * Constructs a new DatabaseConnection instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param connection the Connection object to base this DatabaseConnection on
   * @throws DatabaseException in case there is a problem connecting to the database
   * @return a new DatabaseConnection instance
   */
  public static DatabaseConnection createConnection(final Database database, final Connection connection) throws DatabaseException {
    return createConnection(database, connection, 0);
  }

  /**
   * Constructs a new DatabaseConnection instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param connection the Connection object to base this DatabaseConnection on, it is assumed to be in a valid state
   * @param validityCheckTimeout the number of seconds specified when checking if the connection is valid
   * @throws DatabaseException in case there is a problem connecting to the database
   * @return a new DatabaseConnection instance
   */
  public static DatabaseConnection createConnection(final Database database, final Connection connection,
                                                    final int validityCheckTimeout) throws DatabaseException {
    return new DefaultDatabaseConnection(database, connection, validityCheckTimeout);
  }
}
