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
      connection = DatabaseConnections.createConnection(Databases.createInstance(), UNIT_TEST_USER);
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
      connection = DatabaseConnections.createConnection(Databases.createInstance(), UNIT_TEST_USER);
      final long qLong = DatabaseUtil.queryLong(connection, "select empno from scott.emp where ename = 'ADAMS'");
      assertTrue(qLong == 10L);
    }
    finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
