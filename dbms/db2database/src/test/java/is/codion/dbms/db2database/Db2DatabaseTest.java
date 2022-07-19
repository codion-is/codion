/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.db2database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Db2DatabaseTest {

  private static final String URL = "jdbc:db2://server:6789/database";

  @Test
  void getName() {
    Db2Database database = new Db2Database(URL);
    assertEquals("database", database.getName());
    database = new Db2Database( URL + ";options");
    assertEquals("database", database.getName());
  }

  @Test
  void getAutoIncrementQuery() {
    Db2Database database = new Db2Database("test");
    assertEquals("select previous value for seq", database.getAutoIncrementQuery("seq"));
  }

  @Test
  void getSequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new Db2Database(URL).getSequenceQuery(null));
  }

  @Test
  void getAutoIncrementQueryNullIdSource() {
    assertThrows(NullPointerException.class, () -> new Db2Database(URL).getAutoIncrementQuery(null));
  }

  @Test
  void supportsIsValid() {
    Db2Database db = new Db2Database(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new Db2Database(null));
  }
}
