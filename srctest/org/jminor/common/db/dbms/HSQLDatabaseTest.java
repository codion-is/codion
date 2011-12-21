/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.Database;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class HSQLDatabaseTest {

  @Test (expected = IllegalArgumentException.class)
  public void getSequenceSQLNullSequence() {
    new HSQLDatabase("host", "1234", "sid").getSequenceSQL(null);
  }

  @Test
  public void test() {
    HSQLDatabase db = new HSQLDatabase("host", "1234", "sid");
    final Properties props = new Properties();
    props.put("user", "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertTrue(db.supportsIsValid());
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
    assertEquals(HSQLDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementValueSQL(null));
    final String idSource = "seq";
    assertEquals(HSQLDatabase.SEQUENCE_VALUE_QUERY + idSource, db.getSequenceSQL(idSource));
    assertEquals("jdbc:hsqldb:hsql//host:1234/sid;user=scott;password=tiger", db.getURL(props));

    db = new HSQLDatabase("dbname");
    assertEquals("jdbc:hsqldb:file:dbname;user=scott;password=tiger", db.getURL(props));

    db = new HSQLDatabase(null, null, null);
    try {
      db.getURL(null);
      fail();
    }
    catch (RuntimeException e) {}
  }
}