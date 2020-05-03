/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.database;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.Users;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabasesTest {

  @Test
  public void closeSilently() {
    Databases.closeSilently((Statement) null);
    Databases.closeSilently((ResultSet) null);
    Databases.closeSilently((Connection) null);
  }

  @Test
  public void validateWithQuery() throws DatabaseException, SQLException {
    final Database testDatabase = new TestDatabase();
    final Connection connection = testDatabase.createConnection(Users.parseUser("scott:tiger"));
    assertTrue(testDatabase.isConnectionValid(connection));
    connection.close();
    assertFalse(testDatabase.isConnectionValid(connection));
  }

  private static final class TestDatabase extends AbstractDatabase {

    private final Database database;

    public TestDatabase() {
      super("jdbc:h2:mem:h2db");
      this.database = Databases.getInstance();
    }

    @Override
    public String getName() {
      return database.getName();
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
  }
}
