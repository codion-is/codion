/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

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
}
