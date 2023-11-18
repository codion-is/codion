/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.db2database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Db2DatabaseTest {

  private static final String URL = "jdbc:db2://server:6789/database";

  @Test
  void name() {
    Db2Database database = new Db2Database(URL);
    assertEquals("database", database.name());
    database = new Db2Database(URL + ";options");
    assertEquals("database", database.name());
  }

  @Test
  void autoIncrementQuery() {
    Db2Database database = new Db2Database("test");
    assertEquals("SELECT PREVIOUS VALUE FOR seq", database.autoIncrementQuery("seq"));
  }

  @Test
  void sequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new Db2Database(URL).sequenceQuery(null));
  }

  @Test
  void autoIncrementQueryNullIdSource() {
    assertThrows(NullPointerException.class, () -> new Db2Database(URL).autoIncrementQuery(null));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new Db2Database(null));
  }
}
