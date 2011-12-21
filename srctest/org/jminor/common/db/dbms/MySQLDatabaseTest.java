/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.Test;

import static org.junit.Assert.*;

public class MySQLDatabaseTest {

  @Test(expected = UnsupportedOperationException.class)
  public void getSequenceSQL() {
    new MySQLDatabase("host", "1234", "sid").getSequenceSQL("seq");
  }

  @Test
  public void test() {
    MySQLDatabase db = new MySQLDatabase("host", "1234", "sid");
    assertTrue(db.supportsIsValid());
    assertNull(db.getAuthenticationInfo(null));
    assertEquals(MySQLDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementValueSQL(null));
    assertEquals("jdbc:mysql://host:1234/sid", db.getURL(null));

    db = new MySQLDatabase(null, null, null);
    try {
      db.getURL(null);
      fail();
    }
    catch (RuntimeException e) {}
  }
}