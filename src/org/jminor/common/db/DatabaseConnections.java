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
    return new DefaultDatabaseConnection(database, user);
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
    return new DefaultDatabaseConnection(database, connection);
  }

  /**
   * Instantiates a default DatabaseConnectionProvider instance
   * @param database the underlying database
   * @param user the user
   */
  public static DatabaseConnectionProvider connectionProvider(final Database database, final User user) {
    return new ConnectionProvider(database, user);
  }

  /**
   * A connection provider
   */
  private static final class ConnectionProvider implements DatabaseConnectionProvider {

    private final Database database;
    private final User user;

    private ConnectionProvider(final Database database, final User user) {
      this.database = database;
      this.user = user;
    }

    /** {@inheritDoc} */
    @Override
    public DatabaseConnection createConnection() throws DatabaseException {
      return DatabaseConnections.createConnection(database, user);
    }

    /** {@inheritDoc} */
    @Override
    public void destroyConnection(final DatabaseConnection connection) {
      connection.disconnect();
    }

    /** {@inheritDoc} */
    @Override
    public User getUser() {
      return user;
    }
  }
}
