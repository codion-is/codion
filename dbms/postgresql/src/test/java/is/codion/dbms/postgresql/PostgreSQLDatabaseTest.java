/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.postgresql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PostgreSQLDatabaseTest {

  private static final String URL = "jdbc:postgresql://host:1234/sid";

  @Test
  void getName() {
    PostgreSQLDatabase database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid");
    assertEquals("sid", database.getName());
    database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid;options");
    assertEquals("sid", database.getName());
    database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid?parameters");
    assertEquals("sid", database.getName());
    database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid?parameters;options");
    assertEquals("sid", database.getName());
  }

  @Test
  void getSequenceQueryNullSequence() {
    assertThrows(NullPointerException.class, () -> new PostgreSQLDatabase(URL).getSequenceQuery(null));
  }

  @Test
  void supportsIsValid() {
    PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertFalse(db.supportsIsValid());
  }

  @Test
  void getAutoIncrementQuery() {
    PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertEquals("select currval('seq')", db.getAutoIncrementQuery("seq"));
  }

  @Test
  void getSequenceQuery() {
    PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertEquals("select nextval('seq')", db.getSequenceQuery("seq"));
  }

  @Test
  void getCheckConnectionQuery() {
    PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertEquals(PostgreSQLDatabase.CHECK_QUERY, db.getCheckConnectionQuery());
  }

  @Test
  void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new PostgreSQLDatabase(null));
  }
}