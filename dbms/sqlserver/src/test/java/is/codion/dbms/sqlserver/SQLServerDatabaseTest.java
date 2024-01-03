/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.sqlserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SQLServerDatabaseTest {

  private static final String URL = "jdbc:sqlserver://host:1234;databaseName=sid";

  @Test
  void name() {
    SQLServerDatabase database = new SQLServerDatabase("jdbc:sqlserver://host.db\\instance:1234");
    assertEquals("instance", database.name());
    database = new SQLServerDatabase("jdbc:sqlserver://host.db\\instance:1234;options");
    assertEquals("instance", database.name());
  }

  @Test
  void sequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new SQLServerDatabase(URL).sequenceQuery("seq"));
  }

  @Test
  void autoIncrementQuery() {
    SQLServerDatabase db = new SQLServerDatabase(URL);
    assertEquals(SQLServerDatabase.AUTO_INCREMENT_QUERY, db.autoIncrementQuery(null));
  }

  @Test
  void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new SQLServerDatabase(null));
  }
}