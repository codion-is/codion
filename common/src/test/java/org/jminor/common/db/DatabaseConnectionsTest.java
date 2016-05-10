/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;

import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatabaseConnectionsTest {

  private static final Database DATABASE = DatabasesTest.createTestDatabaseInstance();

  public static DatabaseConnectionProvider createTestDatabaseConnectionProvider() {
    return new DatabaseConnectionProvider() {
      @Override
      public DatabaseConnection createConnection() throws DatabaseException {
        return DatabaseConnections.createConnection(DATABASE, getUser());
      }

      @Override
      public void destroyConnection(final DatabaseConnection connection) {
        connection.disconnect();
      }

      @Override
      public User getUser() {
        return User.UNIT_TEST_USER;
      }
    };
  }

  @Test
  public void createConnection() throws Exception {
    Connection connection = null;
    try {
      connection = DATABASE.createConnection(User.UNIT_TEST_USER);
      final DatabaseConnection databaseConnection = DatabaseConnections.createConnection(DATABASE, connection);
      assertTrue(databaseConnection.isConnected());
      assertNotNull(databaseConnection.getUser());
      assertTrue(User.UNIT_TEST_USER.getUsername().equalsIgnoreCase(databaseConnection.getUser().getUsername()));
    }
    finally {
      DatabaseUtil.closeSilently(connection);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void createConnectionWithClosedConnection() throws DatabaseException, SQLException {
    Connection connection = null;
    try {
      connection = DATABASE.createConnection(User.UNIT_TEST_USER);
      connection.close();
      DatabaseConnections.createConnection(DATABASE, connection);
    }
    finally {
      DatabaseUtil.closeSilently(connection);
    }
  }
}
