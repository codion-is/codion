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

import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractDatabaseTest {

  private final AbstractDatabase database = new AbstractDatabase("jdbc:h2:mem:h2db") {
    @Override
    public String name() {
      return "name";
    }
    @Override
    public String autoIncrementQuery(String idSource) {
      return null;
    }
    @Override
    public String selectForUpdateClause() {
      return "for update nowait";
    }
    @Override
    public String limitOffsetClause(Integer limit, Integer offset) {
      return createLimitOffsetClause(limit, offset);
    }
  };

  @Test
  void test() {
    assertEquals("for update nowait", database.selectForUpdateClause());
    assertEquals("name", database.name());
    database.errorMessage(new SQLException(), Operation.OTHER);
  }

  @Test
  void connectionProvider() throws Exception {
    User sa = User.user("sa");
    Connection connection = database.createConnection(sa);
    database.setConnectionProvider(new ConnectionProvider() {
      @Override
      public Connection connection(User user, String url) {
        return connection;
      }
    });
    assertSame(connection, database.createConnection(sa));
    database.setConnectionProvider(null);
    Connection newConnection = database.createConnection(sa);
    assertNotSame(connection, newConnection);
    connection.close();
    newConnection.close();
  }

  @Test
  void limitOffset() {
    assertEquals("", database.createOffsetFetchNextClause(null, null));
    assertEquals("offset 5 rows", database.createOffsetFetchNextClause(null, 5));
    assertEquals("fetch next 10 rows only", database.createOffsetFetchNextClause(10, null));
    assertEquals("offset 5 rows fetch next 10 rows only", database.createOffsetFetchNextClause(10, 5));
    assertEquals("", database.limitOffsetClause(null, null));
    assertEquals("offset 5", database.limitOffsetClause(null, 5));
    assertEquals("limit 10", database.limitOffsetClause(10, null));
    assertEquals("limit 10 offset 5", database.limitOffsetClause(10, 5));
  }

  @Test
  void transactionIsolation() throws DatabaseException, SQLException {
    User sa = User.user("sa");
    Connection connection = database.createConnection(sa);
    assertEquals(Connection.TRANSACTION_READ_COMMITTED, connection.getTransactionIsolation());
    connection.close();
    Database.TRANSACTION_ISOLATION.set(Connection.TRANSACTION_SERIALIZABLE);
    connection = database.createConnection(sa);
    assertEquals(Connection.TRANSACTION_SERIALIZABLE, connection.getTransactionIsolation());
    connection.close();
    Database.TRANSACTION_ISOLATION.set(null);
  }
}
