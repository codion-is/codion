package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;

import java.sql.Connection;

public final class DatabaseConnections {

  private DatabaseConnections() {}

  /**
   * Constructs a new DatabaseConnection instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param user the user for the db-connection
   * @return a new DatabaseConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws ClassNotFoundException in case the JDBC driver class was not found
   */
  public static DatabaseConnection createConnection(final Database database, final User user) throws ClassNotFoundException, DatabaseException {
    return new DatabaseConnectionImpl(database, user, database.createConnection(user));
  }

  /**
   * Constructs a new DatabaseConnection instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param user the user for the db-connection
   * @param connection the Connection object to base this DatabaseConnection on
   * @throws DatabaseException in case there is a problem connecting to the database
   * @return a new DatabaseConnection instance
   */
  public static DatabaseConnection createConnection(final Database database, final User user, final Connection connection) throws DatabaseException {
    return new DatabaseConnectionImpl(database, user, connection);
  }
}
