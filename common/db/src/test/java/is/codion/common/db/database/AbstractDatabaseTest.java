/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
    public String getAutoIncrementQuery(final String idSource) {
      return null;
    }
  };

  @Test
  void test() throws Exception {
    assertEquals(Database.SelectForUpdateSupport.FOR_UPDATE_NOWAIT, database.getSelectForUpdateSupport());
    assertTrue(database.supportsIsValid());
    assertEquals("name", database.getName());
    database.shutdownEmbedded();
    database.getErrorMessage(new SQLException());
  }

  @Test
  void connectionProvider() throws Exception {
    final User sa = User.user("sa");
    final Connection connection = database.createConnection(sa);
    database.setConnectionProvider(new ConnectionProvider() {
      @Override
      public Connection getConnection(final User user, final String jdbcUrl) throws SQLException {
        return connection;
      }
    });
    assertSame(connection, database.createConnection(sa));
    database.setConnectionProvider(null);
    final Connection newConnection = database.createConnection(sa);
    assertNotSame(connection, newConnection);
    connection.close();
    newConnection.close();
  }
}
