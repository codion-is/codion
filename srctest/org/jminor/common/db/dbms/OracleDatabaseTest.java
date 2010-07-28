/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import static org.junit.Assert.*;
import org.junit.Test;

public class OracleDatabaseTest {

  @Test
  public void test() {
    OracleDatabase db = new OracleDatabase("host", "1234", "sid");
    assertFalse(db.supportsIsValid());
    assertNull(db.getAuthenticationInfo(null));
    assertEquals("select seq.currval from dual", db.getAutoIncrementValueSQL("seq"));
    assertEquals("select seq.nextval from dual", db.getSequenceSQL("seq"));
    assertEquals("jdbc:oracle:thin:@host:1234:sid", db.getURL(null));
    assertEquals("select 1 from dual", db.getCheckConnectionQuery());
  }
}