/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.postgresql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PostgreSQLDatabaseTest {

  private static final String URL = "jdbc:postgresql://host:1234/sid";

  @Test
  public void getName() {
    PostgreSQLDatabase database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid");
    assertEquals("host.db:1234/sid", database.getName());
    database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid;options");
    assertEquals("host.db:1234/sid", database.getName());
  }

  @Test
  public void getSequenceQueryNullSequence() {
    assertThrows(NullPointerException.class, () -> new PostgreSQLDatabase(URL).getSequenceQuery(null));
  }

  @Test
  public void supportsIsValid() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertFalse(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertNull(db.getAuthenticationInfo(null));
  }

  @Test
  public void getAutoIncrementQuery() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertEquals("select currval('seq')", db.getAutoIncrementQuery("seq"));
  }

  @Test
  public void getSequenceQuery() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertEquals("select nextval('seq')", db.getSequenceQuery("seq"));
  }

  @Test
  public void getCheckConnectionQuery() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertEquals(PostgreSQLDatabase.CHECK_QUERY, db.getCheckConnectionQuery());
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new PostgreSQLDatabase(null));
  }
}