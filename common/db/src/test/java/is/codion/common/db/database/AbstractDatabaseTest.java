/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractDatabaseTest {

  private final AbstractDatabase database = new AbstractDatabase("jdbc:h2:mem:h2db") {
    @Override
    public String getName() {
      return "name";
    }
    @Override
    public String getAutoIncrementQuery(String idSource) {
      return null;
    }
    @Override
    public String getSelectForUpdateClause() {
      return "for update nowait";
    }
    @Override
    public String getLimitOffsetClause(Integer limit, Integer offset) {
      return createLimitOffsetClause(limit, offset);
    }
  };

  @Test
  void test() throws Exception {
    assertEquals("for update nowait", database.getSelectForUpdateClause());
    assertTrue(database.supportsIsValid());
    assertEquals("name", database.getName());
    database.shutdownEmbedded();
    database.getErrorMessage(new SQLException());
  }

  @Test
  void connectionProvider() throws Exception {
    User sa = User.user("sa");
    Connection connection = database.createConnection(sa);
    database.setConnectionProvider(new ConnectionProvider() {
      @Override
      public Connection getConnection(User user, String jdbcUrl) throws SQLException {
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
    assertEquals("", database.getLimitOffsetClause(null, null));
    assertEquals("offset 5", database.getLimitOffsetClause(null, 5));
    assertEquals("limit 10", database.getLimitOffsetClause(10, null));
    assertEquals("limit 10 offset 5", database.getLimitOffsetClause(10, 5));
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
