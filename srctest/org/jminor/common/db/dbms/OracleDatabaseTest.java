/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.Test;

import static org.junit.Assert.*;

public class OracleDatabaseTest {

  @Test
  public void test() {
    OracleDatabase db = new OracleDatabase("host", "1234", "sid");
    assertFalse(db.supportsIsValid());
    assertNull(db.getAuthenticationInfo(null));
    assertEquals("select seq.currval from dual", db.getAutoIncrementValueSQL("seq"));
    assertEquals("select seq.nextval from dual", db.getSequenceSQL("seq"));
    assertEquals("jdbc:oracle:thin:@host:1234:sid", db.getURL(null));
    assertEquals(OracleDatabase.CHECK_QUERY, db.getCheckConnectionQuery());

    db = new OracleDatabase(null, null, null);
    try {
      db.getURL(null);
      fail();
    }
    catch (RuntimeException e) {}
  }
}