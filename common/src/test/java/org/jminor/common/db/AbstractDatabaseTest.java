/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.junit.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.*;

public final class AbstractDatabaseTest {

  private static final String DRIVER_CLASS = "some.driver.Class";

  private final AbstractDatabase database = new AbstractDatabase(Database.Type.H2, DRIVER_CLASS) {
    @Override
    public String getAutoIncrementValueSQL(final String idSource) {
      return null;
    }
    @Override
    public String getURL(final Properties connectionProperties) {
      return null;
    }
  };

  @Test
  public void test() throws Exception {
    assertTrue(database.supportsIsValid());
    assertTrue(database.supportsNowait());
    assertNull(database.getCheckConnectionQuery());
    assertEquals(DRIVER_CLASS, database.getDriverClassName());
    database.shutdownEmbedded(null);
    database.getErrorMessage(new SQLException());
  }
}
