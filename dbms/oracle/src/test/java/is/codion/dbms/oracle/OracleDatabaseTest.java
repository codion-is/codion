/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.dbms.oracle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OracleDatabaseTest {

  public static final String URL = "jdbc:oracle:thin:@host:1234:sid";

  @Test
  public void getName() {
    OracleDatabase database = new OracleDatabase("jdbc:oracle:thin:@host.com:1234:sid");
    assertEquals("sid", database.getName());
    database = new OracleDatabase("jdbc:oracle:thin:@host.com:1234:sid;option=true;option2=false");
    assertEquals("sid", database.getName());
    database = new OracleDatabase("jdbc:oracle:thin:@host.com:1234/sid;option=true;option2=false");
    assertEquals("sid", database.getName());
  }

  @Test
  public void getSequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new OracleDatabase(URL).getSequenceQuery(null));
  }

  @Test
  public void supportsIsValid() {
    final OracleDatabase db = new OracleDatabase(URL);
    assertFalse(db.supportsIsValid());
  }

  @Test
  public void getAutoIncrementQuery() {
    final OracleDatabase db = new OracleDatabase(URL);
    assertEquals("select seq.currval from dual", db.getAutoIncrementQuery("seq"));
  }

  @Test
  public void getSequenceQuery() {
    final OracleDatabase db = new OracleDatabase(URL);
    assertEquals("select seq.nextval from dual", db.getSequenceQuery("seq"));
  }

  @Test
  public void getURL() {
    final OracleDatabase db = new OracleDatabase(URL);
    assertEquals("jdbc:oracle:thin:@host:1234:sid", db.getUrl());
  }

  @Test
  public void getCheckConnectionQuery() {
    final OracleDatabase db = new OracleDatabase(URL);
    assertEquals(OracleDatabase.CHECK_QUERY, db.getCheckConnectionQuery());
  }

  @Test
  public void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new OracleDatabase(null));
  }
}