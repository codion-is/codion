/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.connection;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.Databases;
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
        dbConnection.close();
      }
    }
    catch (final Exception ignored) {/*ignored*/}
  }

  @Test
  public void constructorWithConnection() throws DatabaseException, SQLException {
    final Connection connection = DATABASE.createConnection(UNIT_TEST_USER);
    new DefaultDatabaseConnection(DATABASE, connection).close();
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
    assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, User.user("foo", "bar".toCharArray())));
  }

  @Test
  public void wrongPassword() throws Exception {
    assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, User.user(UNIT_TEST_USER.getUsername(), "xxxxx".toCharArray())));
  }

  @Test
  public void queryInteger() throws Exception {
    try (final DatabaseConnection connection = new DefaultDatabaseConnection(Databases.getInstance(), UNIT_TEST_USER)) {
      final int qInt = connection.selectInteger("select empno from scott.emp where ename = 'ADAMS'");
      assertEquals(10, qInt);
    }
  }

  @Test
  public void queryLong() throws Exception {
    try (final DatabaseConnection connection = new DefaultDatabaseConnection(Databases.getInstance(), UNIT_TEST_USER)) {
      final long qLong = connection.selectLong("select empno from scott.emp where ename = 'ADAMS'");
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
  public void close() throws Exception {
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
