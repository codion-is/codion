/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.database;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AbstractDatabaseTest {

  private final AbstractDatabase database = new AbstractDatabase(Database.Type.H2, "jdbc:h2:mem:h2db") {
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
  public void test() throws Exception {
    assertEquals(Database.SelectForUpdateSupport.FOR_UPDATE_NOWAIT, database.getSelectForUpdateSupport());
    assertTrue(database.supportsIsValid());
    assertEquals("name", database.getName());
    database.shutdownEmbedded(null);
    database.getErrorMessage(new SQLException());
  }
}
