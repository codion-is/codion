/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.connection;

import dev.codion.common.db.database.Database;
import dev.codion.common.db.database.Databases;
import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionsTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Database DATABASE = Databases.getInstance();

  @Test
  public void createConnection() throws Exception {
    Connection connection = null;
    try {
      connection = DATABASE.createConnection(UNIT_TEST_USER);
      final DatabaseConnection databaseConnection = DatabaseConnections.createConnection(DATABASE, connection);
      assertTrue(databaseConnection.isConnected());
      assertNotNull(databaseConnection.getUser());
      assertTrue(UNIT_TEST_USER.getUsername().equalsIgnoreCase(databaseConnection.getUser().getUsername()));
    }
    finally {
      Database.closeSilently(connection);
    }
  }

  @Test
  public void createConnectionWithClosedConnection() throws DatabaseException, SQLException {
    assertThrows(DatabaseException.class, () -> {
      Connection connection = null;
      try {
        connection = DATABASE.createConnection(UNIT_TEST_USER);
        connection.close();
        DatabaseConnections.createConnection(DATABASE, connection);
      }
      finally {
        Database.closeSilently(connection);
      }
    });
  }
}
