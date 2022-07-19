/*
 * Copyright (c) 2012 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.connection;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.logging.MethodLogger;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static is.codion.common.db.connection.DatabaseConnection.databaseConnection;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Database DATABASE = DatabaseFactory.getDatabase();

  @Test
  void createConnection() throws Exception {
    Connection connection = null;
    try {
      connection = DATABASE.createConnection(UNIT_TEST_USER);
      DatabaseConnection databaseConnection = databaseConnection(DATABASE, connection);
      assertTrue(databaseConnection.isConnected());
      assertNotNull(databaseConnection.getUser());
      assertTrue(UNIT_TEST_USER.getUsername().equalsIgnoreCase(databaseConnection.getUser().getUsername()));
    }
    finally {
      Database.closeSilently(connection);
    }
  }

  @Test
  void createConnectionWithClosedConnection() throws DatabaseException, SQLException {
    assertThrows(DatabaseException.class, () -> {
      Connection connection = null;
      try {
        connection = DATABASE.createConnection(UNIT_TEST_USER);
        connection.close();
        databaseConnection(DATABASE, connection);
      }
      finally {
        Database.closeSilently(connection);
      }
    });
  }

  @Test
  void test() {
    try (DatabaseConnection connection = databaseConnection(DATABASE, UNIT_TEST_USER)) {
      MethodLogger methodLogger = MethodLogger.methodLogger(20);
      methodLogger.setEnabled(true);
      connection.setMethodLogger(methodLogger);
      assertSame(methodLogger, connection.getMethodLogger());
      assertThrows(SQLException.class, () -> connection.selectInteger("select deptno from scott.dept where deptno > 1000"));
      assertEquals(10, connection.selectInteger("select deptno from scott.dept order by deptno"));
      connection.commit();
      assertThrows(SQLException.class, () -> connection.selectLong("select deptno from scott.dept where deptno > 1000"));
      assertEquals(10L, connection.selectLong("select deptno from scott.dept order by deptno"));
      connection.rollback();
      connection.toString();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
