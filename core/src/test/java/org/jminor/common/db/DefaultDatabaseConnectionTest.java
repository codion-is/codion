/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
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
public class DefaultDatabaseConnectionTest {

  private static final Database DATABASE = DatabasesTest.createTestDatabaseInstance();
  private DefaultDatabaseConnection dbConnection;

  @Before
  public void before() throws Exception {
    dbConnection = new DefaultDatabaseConnection(DATABASE, User.UNIT_TEST_USER);
  }

  @After
  public void after() {
    try {
      if (dbConnection != null) {
        dbConnection.disconnect();
      }
    }
    catch (final Exception ignored) {/*ignored*/}
  }

  @Test
  public void constructorWithConnection() throws DatabaseException, SQLException {
    final Connection connection = DATABASE.createConnection(User.UNIT_TEST_USER);
    new DefaultDatabaseConnection(DATABASE, connection).disconnect();
    assertTrue(connection.isClosed());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorWithInvalidConnection() throws DatabaseException, SQLException {
    final Connection connection = DATABASE.createConnection(User.UNIT_TEST_USER);
    connection.close();
    new DefaultDatabaseConnection(DATABASE, connection);
  }

  @Test(expected = DatabaseException.class)
  public void wrongUsername() throws Exception {
    new DefaultDatabaseConnection(DATABASE, new User("foo", "bar"));
  }

  @Test(expected = DatabaseException.class)
  public void wrongPassword() throws Exception {
    new DefaultDatabaseConnection(DATABASE, new User(User.UNIT_TEST_USER.getUsername(), "xxxxx"));
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
    assertNotNull(dbConnection.getConnection(false));
  }

  @Test(expected = IllegalStateException.class)
  public void getConnectionDisconnected() throws Exception {
    dbConnection.disconnect();
    dbConnection.getConnection();
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
  public void rollbackTransactionAlreadyRollbacked() {
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
