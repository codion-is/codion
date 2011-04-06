/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.Test;

import static org.junit.Assert.*;

public class PostgreSQLDatabaseTest {

  @Test
  public void test() {
    PostgreSQLDatabase db = new PostgreSQLDatabase("host", "1234", "sid");
    assertFalse(db.supportsIsValid());
    assertNull(db.getAuthenticationInfo(null));
    assertEquals("select currval(seq)", db.getAutoIncrementValueSQL("seq"));
    assertEquals("select nextval(seq)", db.getSequenceSQL("seq"));
    assertEquals("jdbc:postgresql://host:1234/sid", db.getURL(null));
    assertEquals(PostgreSQLDatabase.CHECK_QUERY, db.getCheckConnectionQuery());

    db = new PostgreSQLDatabase(null, null, null);
    try {
      db.getURL(null);
      fail();
    }
    catch (RuntimeException e) {}
  }
}