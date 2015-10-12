/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.Database;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HSQLDatabaseTest {

  @Test (expected = IllegalArgumentException.class)
  public void getSequenceSQLNullSequence() {
    new HSQLDatabase("host", "1234", "sid").getSequenceSQL(null);
  }

  @Test
  public void supportsIsValid() {
    final HSQLDatabase db = new HSQLDatabase("host", "1234", "sid");
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final HSQLDatabase db = new HSQLDatabase("host", "1234", "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
  }

  @Test
  public void getAutoIncrementValueSQL() {
    final HSQLDatabase db = new HSQLDatabase("host", "1234", "sid");
    assertEquals(HSQLDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementValueSQL(null));
  }

  @Test
  public void getSequenceSQL() {
    final HSQLDatabase db = new HSQLDatabase("host", "1234", "sid");
    final String idSource = "seq";
    assertEquals(HSQLDatabase.SEQUENCE_VALUE_QUERY + idSource, db.getSequenceSQL(idSource));
  }

  @Test
  public void getURL() {
    HSQLDatabase db = new HSQLDatabase("host", "1234", "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    assertEquals("jdbc:hsqldb:hsql//host:1234/sid;user=scott", db.getURL(props));
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("jdbc:hsqldb:hsql//host:1234/sid;user=scott;password=tiger", db.getURL(props));
    db = new HSQLDatabase("dbname");
    assertEquals("jdbc:hsqldb:file:dbname;user=scott;password=tiger", db.getURL(props));
  }

  @Test(expected = RuntimeException.class)
  public void getURLMissingProperties() {
    final HSQLDatabase db = new HSQLDatabase(null, null, null);
    db.getURL(null);
  }
}