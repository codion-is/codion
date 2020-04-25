/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.oracle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OracleDatabaseTest {

  @Test
  public void getSequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> OracleDatabase.oracleDatabase("host", 1234, "sid").getSequenceQuery(null));
  }

  @Test
  public void supportsIsValid() {
    final OracleDatabase db = OracleDatabase.oracleDatabase("host", 1234, "sid");
    assertFalse(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final OracleDatabase db = OracleDatabase.oracleDatabase("host", 1234, "sid");
    assertNull(db.getAuthenticationInfo(null));
  }

  @Test
  public void getAutoIncrementQuery() {
    final OracleDatabase db = OracleDatabase.oracleDatabase("host", 1234, "sid");
    assertEquals("select seq.currval from dual", db.getAutoIncrementQuery("seq"));
  }

  @Test
  public void getSequenceQuery() {
    final OracleDatabase db = OracleDatabase.oracleDatabase("host", 1234, "sid");
    assertEquals("select seq.nextval from dual", db.getSequenceQuery("seq"));
  }

  @Test
  public void getURL() {
    final OracleDatabase db = OracleDatabase.oracleDatabase("host", 1234, "sid");
    db.setUseLegacySid(true);
    assertEquals("jdbc:oracle:thin:@host:1234:sid", db.getURL(null));
    db.setUseLegacySid(false);
    assertEquals("jdbc:oracle:thin:@host:1234/sid", db.getURL(null));
  }

  @Test
  public void getCheckConnectionQuery() {
    final OracleDatabase db = OracleDatabase.oracleDatabase("host", 1234, "sid");
    assertEquals(OracleDatabase.CHECK_QUERY, db.getCheckConnectionQuery());
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> OracleDatabase.oracleDatabase(null, null, null));
  }
}