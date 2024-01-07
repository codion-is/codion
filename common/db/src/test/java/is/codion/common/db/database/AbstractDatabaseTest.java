/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractDatabaseTest {

  private final AbstractDatabase database = new TestDatabase();

  @Test
  void test() {
    assertEquals(AbstractDatabase.FOR_UPDATE_NOWAIT, database.selectForUpdateClause());
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
    assertEquals("OFFSET 5 ROWS", database.createOffsetFetchNextClause(null, 5));
    assertEquals("FETCH NEXT 10 ROWS ONLY", database.createOffsetFetchNextClause(10, null));
    assertEquals("OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", database.createOffsetFetchNextClause(10, 5));
    assertEquals("", database.limitOffsetClause(null, null));
    assertEquals("OFFSET 5", database.limitOffsetClause(null, 5));
    assertEquals("LIMIT 10", database.limitOffsetClause(10, null));
    assertEquals("LIMIT 10 OFFSET 5", database.limitOffsetClause(10, 5));
  }

  @Test
  void transactionIsolation() throws DatabaseException, SQLException {
    Database db = new TestDatabase();
    User sa = User.user("sa");
    Connection connection = db.createConnection(sa);
    assertEquals(Connection.TRANSACTION_READ_COMMITTED, connection.getTransactionIsolation());
    connection.close();
    Database.TRANSACTION_ISOLATION.set(Connection.TRANSACTION_SERIALIZABLE);
    db = new TestDatabase();
    connection = db.createConnection(sa);
    assertEquals(Connection.TRANSACTION_SERIALIZABLE, connection.getTransactionIsolation());
    connection.close();
    Database.TRANSACTION_ISOLATION.set(null);
  }

  @Test
  void connectionPool() throws DatabaseException {
    Database db = new TestDatabase();
    db.createConnectionPool(ConnectionPoolFactory.instance(), User.parse("scott:tiger"));
    assertTrue(db.containsConnectionPool("ScotT"));
    assertThrows(IllegalArgumentException.class, () -> db.connectionPool("john"));
    assertNotNull(db.connectionPool("scott"));
    db.closeConnectionPool("scott");
    assertFalse(db.containsConnectionPool("ScotT"));
  }

  private static final class TestDatabase extends AbstractDatabase {

    private TestDatabase() {
      super("jdbc:h2:mem:h2db");
    }

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
      return FOR_UPDATE_NOWAIT;
    }
    @Override
    public String limitOffsetClause(Integer limit, Integer offset) {
      return createLimitOffsetClause(limit, offset);
    }
  }
}
