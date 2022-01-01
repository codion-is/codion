/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.connection;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

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

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private final Database DATABASE = DatabaseFactory.getDatabase();
  private DefaultDatabaseConnection dbConnection;

  @BeforeEach
  void before() throws Exception {
    dbConnection = new DefaultDatabaseConnection(DATABASE, UNIT_TEST_USER);
  }

  @AfterEach
  void after() {
    try {
      if (dbConnection != null) {
        dbConnection.close();
      }
    }
    catch (final Exception ignored) {/*ignored*/}
  }

  @Test
  void constructorWithConnection() throws DatabaseException, SQLException {
    final Connection connection = DATABASE.createConnection(UNIT_TEST_USER);
    new DefaultDatabaseConnection(DATABASE, connection).close();
    assertTrue(connection.isClosed());
  }

  @Test
  void constructorWithInvalidConnection() throws DatabaseException, SQLException {
    final Connection connection = DATABASE.createConnection(UNIT_TEST_USER);
    connection.close();
    assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, connection));
  }

  @Test
  void wrongUsername() throws Exception {
    assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, User.user("foo", "bar".toCharArray())));
  }

  @Test
  void wrongPassword() throws Exception {
    assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, User.user(UNIT_TEST_USER.getUsername(), "xxxxx".toCharArray())));
  }

  @Test
  void queryInteger() throws Exception {
    try (final DatabaseConnection connection = new DefaultDatabaseConnection(DatabaseFactory.getDatabase(), UNIT_TEST_USER)) {
      final int qInt = connection.selectInteger("select empno from scott.emp where ename = 'ADAMS'");
      assertEquals(10, qInt);
    }
  }

  @Test
  void queryLong() throws Exception {
    try (final DatabaseConnection connection = new DefaultDatabaseConnection(DatabaseFactory.getDatabase(), UNIT_TEST_USER)) {
      final long qLong = connection.selectLong("select empno from scott.emp where ename = 'ADAMS'");
      assertEquals(10L, qLong);
    }
  }

  @Test
  void getDatabase() {
    assertEquals(DATABASE, dbConnection.getDatabase());
  }

  @Test
  void getUser() {
    assertEquals(dbConnection.getUser(), UNIT_TEST_USER);
  }

  @Test
  void close() throws Exception {
    dbConnection.close();
    assertFalse(dbConnection.isConnected());
    assertNull(dbConnection.getConnection());
    assertThrows(IllegalStateException.class, () -> dbConnection.beginTransaction());
    assertThrows(IllegalStateException.class, () -> dbConnection.commitTransaction());
    assertThrows(IllegalStateException.class, () -> dbConnection.rollbackTransaction());
    assertThrows(IllegalStateException.class, () -> dbConnection.commit());
    assertThrows(IllegalStateException.class, () -> dbConnection.rollback());
  }

  @Test
  void getConnection() throws Exception {
    assertNotNull(dbConnection.getConnection());
  }

  @Test
  void beginTransactionAlreadyOpen() {
    dbConnection.beginTransaction();
    assertThrows(IllegalStateException.class, () -> dbConnection.beginTransaction());
  }

  @Test
  void commitWithinTransaction() throws SQLException {
    dbConnection.beginTransaction();
    assertThrows(IllegalStateException.class, () -> dbConnection.commit());
  }

  @Test
  void rollbackWithinTransaction() throws SQLException {
    dbConnection.beginTransaction();
    assertThrows(IllegalStateException.class, () -> dbConnection.rollback());
  }

  @Test
  void commitTransactionAlreadyCommitted() {
    dbConnection.beginTransaction();
    dbConnection.commitTransaction();
    assertThrows(IllegalStateException.class, () -> dbConnection.commitTransaction());
  }

  @Test
  void rollbackTransactionAlreadyRollbacked() {
    dbConnection.beginTransaction();
    dbConnection.rollbackTransaction();
    assertThrows(IllegalStateException.class, () -> dbConnection.rollbackTransaction());
  }

  @Test
  void commitTransaction() throws Exception {
    dbConnection.beginTransaction();
    assertTrue(dbConnection.isTransactionOpen());
    dbConnection.commitTransaction();
    assertFalse(dbConnection.isTransactionOpen());
  }

  @Test
  void rollbackTransaction() throws Exception {
    dbConnection.beginTransaction();
    assertTrue(dbConnection.isTransactionOpen());
    dbConnection.rollbackTransaction();
    assertFalse(dbConnection.isTransactionOpen());
  }
}
