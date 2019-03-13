/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OracleDatabaseTest {

  @Test
  public void getSequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new OracleDatabase("host", 1234, "sid").getSequenceQuery(null));
  }

  @Test
  public void supportsIsValid() {
    final OracleDatabase db = new OracleDatabase("host", 1234, "sid");
    assertFalse(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final OracleDatabase db = new OracleDatabase("host", 1234, "sid");
    assertNull(db.getAuthenticationInfo(null));
  }

  @Test
  public void getAutoIncrementQuery() {
    final OracleDatabase db = new OracleDatabase("host", 1234, "sid");
    assertEquals("select seq.currval from dual", db.getAutoIncrementQuery("seq"));
  }

  @Test
  public void getSequenceQuery() {
    final OracleDatabase db = new OracleDatabase("host", 1234, "sid");
    assertEquals("select seq.nextval from dual", db.getSequenceQuery("seq"));
  }

  @Test
  public void getURL() {
    final OracleDatabase db = new OracleDatabase("host", 1234, "sid");
    OracleDatabase.USE_LEGACY_SID.set(true);
    assertEquals("jdbc:oracle:thin:@host:1234:sid", db.getURL(null));
    OracleDatabase.USE_LEGACY_SID.set(false);
    assertEquals("jdbc:oracle:thin:@host:1234/sid", db.getURL(null));
  }

  @Test
  public void getCheckConnectionQuery() {
    final OracleDatabase db = new OracleDatabase("host", 1234, "sid");
    assertEquals(OracleDatabase.CHECK_QUERY, db.getCheckConnectionQuery());
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new OracleDatabase(null, null, null));
  }
}