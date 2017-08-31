/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;

import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DatabaseUtilTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  @Test
  public void closeSilently() {
    DatabaseUtil.closeSilently((Connection[]) null);
    DatabaseUtil.closeSilently((Statement) null);
    DatabaseUtil.closeSilently((ResultSet) null);

    DatabaseUtil.closeSilently((Statement[]) null);
    DatabaseUtil.closeSilently((ResultSet[]) null);

    DatabaseUtil.closeSilently(new Statement[]{null, null});
    DatabaseUtil.closeSilently(new ResultSet[]{null, null});
  }

  @Test
  public void getDatabaseStatistics() {
    DatabaseUtil.getDatabaseStatistics();
  }

  @Test
  public void queryInteger() throws DatabaseException, SQLException {
    DatabaseConnection connection = null;
    try {
      connection = DatabaseConnections.createConnection(Databases.getInstance(), UNIT_TEST_USER);
      final int qInt = DatabaseUtil.queryInteger(connection, "select empno from scott.emp where ename = 'ADAMS'");
      assertTrue(qInt == 10);
    }
    finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  @Test
  public void queryLong() throws DatabaseException, SQLException {
    DatabaseConnection connection = null;
    try {
      connection = DatabaseConnections.createConnection(Databases.getInstance(), UNIT_TEST_USER);
      final long qLong = DatabaseUtil.queryLong(connection, "select empno from scott.emp where ename = 'ADAMS'");
      assertTrue(qLong == 10L);
    }
    finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  @Test
  public void validateWithQuery() throws DatabaseException, SQLException {
    final Database testDatabase = new TestDatabase();
    final Connection connection = testDatabase.createConnection(new User("scott", "tiger"));
    assertTrue(DatabaseUtil.isValid(connection, testDatabase, 2));
    connection.close();
    assertFalse(DatabaseUtil.isValid(connection, testDatabase, 2));
  }

  private static final class TestDatabase extends AbstractDatabase {

    private final Database database;

    public TestDatabase() {
      super(Type.H2, "org.h2.Driver");
      this.database = Databases.getInstance();
    }

    @Override
    public String getCheckConnectionQuery() {
      return "select 1 from dual";
    }

    @Override
    public boolean supportsIsValid() {
      return false;
    }

    @Override
    public String getAutoIncrementQuery(final String idSource) {
      return database.getAutoIncrementQuery(idSource);
    }

    @Override
    public String getURL(final Properties connectionProperties) {
      return database.getURL(connectionProperties);
    }
  }
}
