package org.jminor.common.db.dbms;

import org.jminor.common.model.User;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class AbstractDatabaseTest {
  private final AbstractDatabase database = new H2Database("h2db/h2");

  @Test
  public void test() throws Exception {
    Connection conn = null;
    try {
      conn = database.createConnection(User.UNIT_TEST_USER);
      assertTrue(conn.isValid(50));
      assertTrue(database.supportsIsValid());
      assertTrue(database.supportsNowait());
      assertNull(database.getCheckConnectionQuery());
      database.shutdownEmbedded(null);
      database.getErrorMessage(new SQLException());
      new AbstractDatabase("db") {
        public String getAutoIncrementValueSQL(final String idSource) {
          return null;
        }
        @Override
        public String getSequenceSQL(final String sequenceName) {
          return null;
        }
        public String getURL(final Properties connectionProperties) {
          return null;
        }
        public void loadDriver() throws ClassNotFoundException {}
      };
    }
    finally {
      try {
        if (conn != null) {
          conn.close();
        }
      }
      catch (Exception e) {}
    }
  }
}
