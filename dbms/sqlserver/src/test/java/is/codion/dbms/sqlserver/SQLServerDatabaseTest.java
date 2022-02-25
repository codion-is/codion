/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.sqlserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SQLServerDatabaseTest {

  private static final String URL = "jdbc:sqlserver://host:1234;databaseName=sid";

  @Test
  void getName() {
    SQLServerDatabase database = new SQLServerDatabase("jdbc:sqlserver://host.db\\instance:1234");
    assertEquals("instance", database.getName());
    database = new SQLServerDatabase("jdbc:sqlserver://host.db\\instance:1234;options");
    assertEquals("instance", database.getName());
  }

  @Test
  void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new SQLServerDatabase(URL).getSequenceQuery("seq"));
  }

  @Test
  void supportsIsValid() {
    SQLServerDatabase db = new SQLServerDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  void getAutoIncrementQuery() {
    SQLServerDatabase db = new SQLServerDatabase(URL);
    assertEquals(SQLServerDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new SQLServerDatabase(null));
  }
}