/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * This test relies on the emp/dept schema
 */
public class DatabaseConnectionImplTest {

  private static final Database DATABASE = Databases.createInstance();
  private DatabaseConnectionImpl dbConnection;

  @Before
  public void before() throws Exception {
    dbConnection = new DatabaseConnectionImpl(DATABASE, User.UNIT_TEST_USER);
    dbConnection.setLoggingEnabled(true);
  }

  @After
  public void after() {
    try {
      if (dbConnection != null) {
        dbConnection.disconnect();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void construction() throws Exception {
    Connection connection = null;
    try {
      connection = DATABASE.createConnection(User.UNIT_TEST_USER);
      final DatabaseConnectionImpl databaseConnection = new DatabaseConnectionImpl(DATABASE, connection);
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
  public void constructionWithClosedConnection() throws ClassNotFoundException, DatabaseException, SQLException {
    Connection connection = null;
    try {
      connection = DATABASE.createConnection(User.UNIT_TEST_USER);
      connection.close();
      new DatabaseConnectionImpl(DATABASE, connection);
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

  @Test(expected = DatabaseException.class)
  public void wrongUsername() throws Exception {
    new DatabaseConnectionImpl(DATABASE, new User("foo", "bar"));
  }

  @Test(expected = DatabaseException.class)
  public void wrongPassword() throws Exception {
    new DatabaseConnectionImpl(DATABASE, new User(User.UNIT_TEST_USER.getUsername(), "xxxxx"));
  }

  @Test
  public void test() throws Exception {
    dbConnection.toString();
    dbConnection.setPoolTime(10);
    assertEquals(10, dbConnection.getPoolTime());
    dbConnection.setRetryCount(2);
    assertEquals(2, dbConnection.getRetryCount());
  }

  @Test
  public void isLoggingEnabled() {
    assertTrue(dbConnection.isLoggingEnabled());
  }

  @Test
  public void getDatabase() {
    assertEquals(DATABASE, dbConnection.getDatabase());
  }

  @Test
  public void getUser() {
    assertEquals(dbConnection.getUser(), User.UNIT_TEST_USER);
  }

  @Test
  public void disconnect() throws Exception {
    dbConnection.disconnect();
    assertFalse(dbConnection.isConnected());
  }

  @Test
  public void getConnection() throws Exception {
    assertNotNull(dbConnection.getConnection());
  }

  @Test(expected = IllegalStateException.class)
  public void beginTransactionAlreadyOpen() {
    dbConnection.beginTransaction();
    dbConnection.beginTransaction();
  }

  @Test(expected = IllegalStateException.class)
  public void commitWithinTransaction() throws SQLException {
    dbConnection.beginTransaction();
    dbConnection.commit();
  }

  @Test(expected = IllegalStateException.class)
  public void rollbackWithinTransaction() throws SQLException {
    dbConnection.beginTransaction();
    dbConnection.rollback();
  }

  @Test(expected = IllegalStateException.class)
  public void commitTransactionAlreadyCommitted() {
    dbConnection.beginTransaction();
    dbConnection.commitTransaction();
    dbConnection.commitTransaction();
  }

  @Test(expected = IllegalStateException.class)
  public void rollbaclTransactionAlreadyRollbacked() {
    dbConnection.beginTransaction();
    dbConnection.rollbackTransaction();
    dbConnection.rollbackTransaction();
  }

  @Test
  public void commitTransaction() throws Exception {
    dbConnection.beginTransaction();
    assertTrue(dbConnection.isTransactionOpen());
    dbConnection.commitTransaction();
    assertFalse(dbConnection.isTransactionOpen());
  }

  @Test
  public void rollbackTransaction() throws Exception {
    dbConnection.beginTransaction();
    assertTrue(dbConnection.isTransactionOpen());
    dbConnection.rollbackTransaction();
    assertFalse(dbConnection.isTransactionOpen());
  }
}
