/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseTest {

  @Test
  void closeSilently() {
    Database.closeSilently(null);
    Database.closeSilently(null);
    Database.closeSilently(null);
  }

  @Test
  void validateWithQuery() throws DatabaseException, SQLException {
    Database testDatabase = new TestDatabase();
    Connection connection = testDatabase.createConnection(User.parse("scott:tiger"));
    assertTrue(testDatabase.isConnectionValid(connection));
    connection.close();
    assertFalse(testDatabase.isConnectionValid(connection));
  }

  private static final class TestDatabase extends AbstractDatabase {

    private final Database database;

    public TestDatabase() {
      super("jdbc:h2:mem:h2db");
      this.database = Database.instance();
    }

    @Override
    public String name() {
      return database.name();
    }

    @Override
    public String checkConnectionQuery() {
      return "select 1 from dual";
    }

    @Override
    public boolean supportsIsValid() {
      return false;
    }

    @Override
    public String selectForUpdateClause() {
      return "for update nowait";
    }

    @Override
    public String limitOffsetClause(Integer limit, Integer offset) {
      return createLimitOffsetClause(limit, offset);
    }

    @Override
    public String autoIncrementQuery(String idSource) {
      return database.autoIncrementQuery(idSource);
    }
  }
}
