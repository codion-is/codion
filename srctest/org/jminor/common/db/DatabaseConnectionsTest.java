/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
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

  private static final Database DATABASE = Databases.createInstance();

  @Test
  public void createConnection() throws Exception {
    Connection connection = null;
    try {
      connection = DATABASE.createConnection(User.UNIT_TEST_USER);
      final DatabaseConnection databaseConnection = DatabaseConnections.createConnection(DATABASE, connection);
      assertTrue(databaseConnection.isConnected());
      assertTrue(databaseConnection.isValid());
      assertNotNull(databaseConnection.getUser());
      assertTrue(User.UNIT_TEST_USER.getUsername().equalsIgnoreCase(databaseConnection.getUser().getUsername()));
    }
    finally {
      if (connection != null) {
        try {
          connection.close();
        }
        catch (Exception ignored) {}
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void createConnectionWithClosedConnection() throws ClassNotFoundException, DatabaseException, SQLException {
    Connection connection = null;
    try {
      connection = DATABASE.createConnection(User.UNIT_TEST_USER);
      connection.close();
      DatabaseConnections.createConnection(DATABASE, connection);
    }
    finally {
      if (connection != null) {
        try {
          connection.close();
        }
        catch (Exception ignored) {}
      }
    }
  }
}
