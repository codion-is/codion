/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.Test;

import static org.junit.Assert.*;

public class OracleDatabaseTest {

  @Test (expected = NullPointerException.class)
  public void getSequenceSQLNullSequence() {
    new OracleDatabase("host", "1234", "sid").getSequenceSQL(null);
  }
  
  @Test
  public void supportsIsValid() {
    final OracleDatabase db = new OracleDatabase("host", "1234", "sid");
    assertFalse(db.supportsIsValid());
  }
  
  @Test
  public void getAuthenticationInfo() {
    final OracleDatabase db = new OracleDatabase("host", "1234", "sid");
    assertNull(db.getAuthenticationInfo(null));
  }
  
  @Test
  public void getAutoIncrementValueSQL() {
    final OracleDatabase db = new OracleDatabase("host", "1234", "sid");
    assertEquals("select seq.currval from dual", db.getAutoIncrementValueSQL("seq"));
  }
  
  @Test
  public void getSequenceSQL() {
    final OracleDatabase db = new OracleDatabase("host", "1234", "sid");
    assertEquals("select seq.nextval from dual", db.getSequenceSQL("seq"));
  }
  
  @Test
  public void getURL() {
    final OracleDatabase db = new OracleDatabase("host", "1234", "sid");
    assertEquals("jdbc:oracle:thin:@host:1234:sid", db.getURL(null));
  }
  
  @Test
  public void getCheckConnectionQuery() {
    final OracleDatabase db = new OracleDatabase("host", "1234", "sid");
    assertEquals(OracleDatabase.CHECK_QUERY, db.getCheckConnectionQuery());
  }

  @Test(expected = RuntimeException.class)
  public void getURLMissingProperties() {
    final OracleDatabase db = new OracleDatabase(null, null, null);
    db.getURL(null);
  }
}