/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.postgresql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PostgreSQLDatabaseTest {

  @Test
  public void getSequenceQueryNullSequence() {
    assertThrows(NullPointerException.class, () -> PostgreSQLDatabase.postgreSqlDatabase("host", 1234, "sid").getSequenceQuery(null));
  }

  @Test
  public void supportsIsValid() {
    final PostgreSQLDatabase db = PostgreSQLDatabase.postgreSqlDatabase("host", 1234, "sid");
    assertFalse(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final PostgreSQLDatabase db = PostgreSQLDatabase.postgreSqlDatabase("host", 1234, "sid");
    assertNull(db.getAuthenticationInfo(null));
  }

  @Test
  public void getAutoIncrementQuery() {
    final PostgreSQLDatabase db = PostgreSQLDatabase.postgreSqlDatabase("host", 1234, "sid");
    assertEquals("select currval('seq')", db.getAutoIncrementQuery("seq"));
  }

  @Test
  public void getSequenceQuery() {
    final PostgreSQLDatabase db = PostgreSQLDatabase.postgreSqlDatabase("host", 1234, "sid");
    assertEquals("select nextval('seq')", db.getSequenceQuery("seq"));
  }

  @Test
  public void getURL() {
    final PostgreSQLDatabase db = PostgreSQLDatabase.postgreSqlDatabase("host", 1234, "sid");
    assertEquals("jdbc:postgresql://host:1234/sid", db.getURL(null));
  }

  @Test
  public void getCheckConnectionQuery() {
    final PostgreSQLDatabase db = PostgreSQLDatabase.postgreSqlDatabase("host", 1234, "sid");
    assertEquals(PostgreSQLDatabase.CHECK_QUERY, db.getCheckConnectionQuery());
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> PostgreSQLDatabase.postgreSqlDatabase(null, null, null));
  }
}