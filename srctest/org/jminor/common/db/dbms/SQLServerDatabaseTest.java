/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.Test;

import static org.junit.Assert.*;

public class SQLServerDatabaseTest {

  @Test(expected = UnsupportedOperationException.class)
  public void getSequenceSQL() {
    new SQLServerDatabase("host", "1234", "sid").getSequenceSQL("seq");
  }

  @Test
  public void test() {
    SQLServerDatabase db = new SQLServerDatabase("host", "1234", "sid");
    assertTrue(db.supportsIsValid());
    assertNull(db.getAuthenticationInfo(null));
    assertEquals(SQLServerDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementValueSQL(null));
    assertEquals("jdbc:sqlserver://host:1234;databaseName=sid", db.getURL(null));

    db = new SQLServerDatabase(null, null, null);
    try {
      db.getURL(null);
      fail();
    }
    catch (RuntimeException e) {}
  }
}