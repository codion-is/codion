/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import static org.junit.Assert.*;
import org.junit.Test;

public class MySQLDatabaseTest {

  @Test
  public void test() {
    final MySQLDatabase db = new MySQLDatabase("host", "1234", "sid");
    assertTrue(db.supportsIsValid());
    assertNull(db.getAuthenticationInfo(null));
    assertEquals(MySQLDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementValueSQL(null));
    try {
      db.getSequenceSQL("seq");
      fail();
    }
    catch (RuntimeException e) {}
    assertEquals("jdbc:mysql://host:1234/sid", db.getURL(null));
  }
}