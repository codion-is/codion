/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.postgresql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PostgreSQLDatabaseTest {

  private static final String URL = "jdbc:postgresql://host:1234/sid";

  @Test
  void name() {
    PostgreSQLDatabase database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid", true);
    assertEquals("sid", database.name());
    database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid;options", true);
    assertEquals("sid", database.name());
    database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid?parameters", true);
    assertEquals("sid", database.name());
    database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid?parameters;options", true);
    assertEquals("sid", database.name());
  }

  @Test
  void sequenceQueryNullSequence() {
    assertThrows(NullPointerException.class, () -> new PostgreSQLDatabase(URL, true).sequenceQuery(null));
  }

  @Test
  void autoIncrementQuery() {
    PostgreSQLDatabase db = new PostgreSQLDatabase(URL, true);
    assertEquals("SELECT CURRVAL('seq')", db.autoIncrementQuery("seq"));
  }

  @Test
  void sequenceQuery() {
    PostgreSQLDatabase db = new PostgreSQLDatabase(URL, true);
    assertEquals("SELECT NEXTVAL('seq')", db.sequenceQuery("seq"));
  }

  @Test
  void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new PostgreSQLDatabase(null, true));
  }
}