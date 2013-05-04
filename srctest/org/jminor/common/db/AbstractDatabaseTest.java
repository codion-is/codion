package org.jminor.common.db;

import org.junit.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class AbstractDatabaseTest {
  private final AbstractDatabase database = new AbstractDatabase("h2") {
    @Override
    public String getAutoIncrementValueSQL(final String idSource) {
      return null;
    }
    @Override
    public String getURL(final Properties connectionProperties) {
      return null;
    }
    @Override
    public String getDriverClassName() {
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
      @Override
      public String getAutoIncrementValueSQL(final String idSource) {
        return null;
      }
      @Override
      public String getSequenceSQL(final String sequenceName) {
        return null;
      }
      @Override
      public String getURL(final Properties connectionProperties) {
        return null;
      }
      @Override
      public String getDriverClassName() {
        return null;
      }
    };
  }
}
