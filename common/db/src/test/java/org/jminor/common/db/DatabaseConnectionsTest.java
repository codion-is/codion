/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionsTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

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
      Databases.closeSilently(connection);
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
        Databases.closeSilently(connection);
      }
    });
  }
}
