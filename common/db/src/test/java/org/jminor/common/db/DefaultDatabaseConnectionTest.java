/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test relies on the emp/dept schema
 */
public class DefaultDatabaseConnectionTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private final Database DATABASE = Databases.getInstance();
  private DefaultDatabaseConnection dbConnection;

  @BeforeEach
  public void before() throws Exception {
    dbConnection = new DefaultDatabaseConnection(DATABASE, UNIT_TEST_USER);
  }

  @AfterEach
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
    final Connection connection = DATABASE.createConnection(UNIT_TEST_USER);
    new DefaultDatabaseConnection(DATABASE, connection).disconnect();
    assertTrue(connection.isClosed());
  }

  @Test
  public void constructorWithInvalidConnection() throws DatabaseException, SQLException {
    final Connection connection = DATABASE.createConnection(UNIT_TEST_USER);
    connection.close();
    assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, connection));
  }

  @Test
  public void wrongUsername() throws Exception {
    assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, new User("foo", "bar".toCharArray())));
  }

  @Test
  public void wrongPassword() throws Exception {
    assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, new User(UNIT_TEST_USER.getUsername(), "xxxxx".toCharArray())));
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
  public void queryInteger() throws Exception {
    try (final DatabaseConnection connection = new DefaultDatabaseConnection(Databases.getInstance(), UNIT_TEST_USER)) {
      final int qInt = connection.queryInteger("select empno from scott.emp where ename = 'ADAMS'");
      assertEquals(10, qInt);
    }
  }

  @Test
  public void queryLong() throws Exception {
    try (final DatabaseConnection connection = new DefaultDatabaseConnection(Databases.getInstance(), UNIT_TEST_USER)) {
      final long qLong = connection.queryLong("select empno from scott.emp where ename = 'ADAMS'");
      assertEquals(10L, qLong);
    }
  }

  @Test
  public void getDatabase() {
    assertEquals(DATABASE, dbConnection.getDatabase());
  }

  @Test
  public void getUser() {
    assertEquals(dbConnection.getUser(), UNIT_TEST_USER);
  }

  @Test
  public void disconnect() throws Exception {
    dbConnection.disconnect();
    assertFalse(dbConnection.isConnected());
    assertNull(dbConnection.getConnection());
  }

  @Test
  public void getConnection() throws Exception {
    assertNotNull(dbConnection.getConnection());
  }

  @Test
  public void beginTransactionAlreadyOpen() {
    dbConnection.beginTransaction();
    assertThrows(IllegalStateException.class, () -> dbConnection.beginTransaction());
  }

  @Test
  public void commitWithinTransaction() throws SQLException {
    dbConnection.beginTransaction();
    assertThrows(IllegalStateException.class, () -> dbConnection.commit());
  }

  @Test
  public void rollbackWithinTransaction() throws SQLException {
    dbConnection.beginTransaction();
    assertThrows(IllegalStateException.class, () -> dbConnection.rollback());
  }

  @Test
  public void commitTransactionAlreadyCommitted() {
    dbConnection.beginTransaction();
    dbConnection.commitTransaction();
    assertThrows(IllegalStateException.class, () -> dbConnection.commitTransaction());
  }

  @Test
  public void rollbackTransactionAlreadyRollbacked() {
    dbConnection.beginTransaction();
    dbConnection.rollbackTransaction();
    assertThrows(IllegalStateException.class, () -> dbConnection.rollbackTransaction());
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
