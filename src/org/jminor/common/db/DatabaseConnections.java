/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;

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

  /**
   * Instantiates a default DatabaseConnectionProvider instance
   * @param database the underlying database
   * @param user the user
   */
  public static DatabaseConnectionProvider connectionProvider(final Database database, final User user) {
    return connectionProvider(database, user, 0);
  }

  /**
   * Instantiates a default DatabaseConnectionProvider instance
   * @param database the underlying database
   * @param user the user
   * @param validityCheckTimeout the number of seconds specified when checking if a connection is valid
   */
  public static DatabaseConnectionProvider connectionProvider(final Database database, final User user, final int validityCheckTimeout) {
    return new ConnectionProvider(database, user, validityCheckTimeout);
  }

  /**
   * A connection provider
   */
  private static final class ConnectionProvider implements DatabaseConnectionProvider {

    private final Database database;
    private final User user;
    private final int validityCheckTimeout;

    private ConnectionProvider(final Database database, final User user, final int validityCheckTimeout) {
      this.database = database;
      this.user = user;
      this.validityCheckTimeout = validityCheckTimeout;
    }

    @Override
    public DatabaseConnection createConnection() throws DatabaseException {
      return DatabaseConnections.createConnection(database, user, validityCheckTimeout);
    }

    @Override
    public void destroyConnection(final DatabaseConnection connection) {
      connection.disconnect();
    }

    @Override
    public User getUser() {
      return user;
    }
  }
}
