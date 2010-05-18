/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import static org.junit.Assert.*;
import org.junit.Test;

public class SQLServerDatabaseTest {

  @Test
  public void test() {
    final SQLServerDatabase db = new SQLServerDatabase("host", "1234", "sid");
    assertTrue(db.supportsIsValid());
    assertNull(db.getAuthenticationInfo(null));
    assertEquals("SELECT SCOPE_IDENTITY()", db.getAutoIncrementValueSQL(null));
    try {
      db.getSequenceSQL("seq");
      fail();
    }
    catch (RuntimeException e) {}
    assertEquals("jdbc:sqlserver://host:1234;databaseName=sid", db.getURL(null));
  }
}