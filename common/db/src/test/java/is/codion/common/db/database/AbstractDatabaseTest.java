/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  public void test() throws Exception {
    assertEquals(Database.SelectForUpdateSupport.FOR_UPDATE_NOWAIT, database.getSelectForUpdateSupport());
    assertTrue(database.supportsIsValid());
    assertEquals("name", database.getName());
    database.shutdownEmbedded();
    database.getErrorMessage(new SQLException());
  }
}
