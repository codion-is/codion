/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractDatabaseTest {

  private static final String DRIVER_CLASS = "some.driver.Class";

  private final AbstractDatabase database = new AbstractDatabase(Database.Type.H2, DRIVER_CLASS, "host", 1234, "sid", false) {
    @Override
    public String getAutoIncrementQuery(final String idSource) {
      return null;
    }
    @Override
    public String getURL(final Properties connectionProperties) {
      return null;
    }
  };

  @Test
  public void test() throws Exception {
    assertEquals("host", database.getHost());
    assertEquals(1234, database.getPort());
    assertEquals("sid", database.getSid());
    assertFalse(database.isEmbedded());
    assertTrue(database.supportsIsValid());
    assertTrue(database.supportsNowait());
    assertEquals(DRIVER_CLASS, database.getDriverClassName());
    database.shutdownEmbedded(null);
    database.getErrorMessage(new SQLException());
  }
}
