package org.jminor.common.db;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Properties;

public final class AbstractDatabaseTest {
  private final AbstractDatabase database = new AbstractDatabase("h2") {
    public void loadDriver() throws ClassNotFoundException {}

    public String getAutoIncrementValueSQL(final String idSource) {
      return null;
    }

    public String getURL(final Properties connectionProperties) {
      return null;
    }
  };

  @Test
  public void test() throws Exception {
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
}
