/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
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
  void validateWithQuery() throws DatabaseException, SQLException {
    Database testDatabase = new TestDatabase();
    Connection connection = testDatabase.createConnection(User.parse("scott:tiger"));
    assertTrue(testDatabase.connectionValid(connection));
    connection.close();
    assertFalse(testDatabase.connectionValid(connection));
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
    public String selectForUpdateClause() {
      return FOR_UPDATE_NOWAIT;
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
