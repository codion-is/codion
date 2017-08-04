/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.Test;

import static org.junit.Assert.*;

public class PostgreSQLDatabaseTest {

  @Test (expected = NullPointerException.class)
  public void getSequenceSQLNullSequence() {
    new PostgreSQLDatabase("host", 1234, "sid").getSequenceSQL(null);
  }
  
  @Test
  public void supportsIsValid() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase("host", 1234, "sid");
    assertFalse(db.supportsIsValid());
  }
  
  @Test
  public void getAuthenticationInfo() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase("host", 1234, "sid");
    assertNull(db.getAuthenticationInfo(null));
  }
  
  @Test
  public void getAutoIncrementValueSQL() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase("host", 1234, "sid");
    assertEquals("select currval('seq')", db.getAutoIncrementValueSQL("seq"));
  }
  
  @Test
  public void getSequenceSQL() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase("host", 1234, "sid");
    assertEquals("select nextval('seq')", db.getSequenceSQL("seq"));
  }
  
  @Test
  public void getURL() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase("host", 1234, "sid");
    assertEquals("jdbc:postgresql://host:1234/sid", db.getURL(null));
  }
  
  @Test
  public void getCheckConnectionQuery() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase("host", 1234, "sid");
    assertEquals(PostgreSQLDatabase.CHECK_QUERY, db.getCheckConnectionQuery());
  }

  @Test(expected = RuntimeException.class)
  public void test() {
    final PostgreSQLDatabase db = new PostgreSQLDatabase(null, null, null);
    db.getURL(null);
  }
}