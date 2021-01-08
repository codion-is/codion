/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseTest {

  @Test
  public void closeSilently() {
    Database.closeSilently((Statement) null);
    Database.closeSilently((ResultSet) null);
    Database.closeSilently((Connection) null);
  }

  @Test
  public void validateWithQuery() throws DatabaseException, SQLException {
    final Database testDatabase = new TestDatabase();
    final Connection connection = testDatabase.createConnection(User.parseUser("scott:tiger"));
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
