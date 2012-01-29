/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.Database;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class H2DatabaseTest {

  @Test (expected = IllegalArgumentException.class)
  public void getSequenceSQLNullSequence() {
    new H2Database("host", "1234", "sid").getSequenceSQL(null);
  }
  
  @Test
  public void supportsIsValid() {
    final H2Database db = new H2Database("host", "1234", "sid");
    assertTrue(db.supportsIsValid());
  }
  
  @Test
  public void getAuthenticationInfo() {
    final H2Database db = new H2Database("host", "1234", "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
  }
  
  @Test
  public void getAutoIncrementValueSQL()  {
    final H2Database db = new H2Database("host", "1234", "sid");
    assertEquals(H2Database.AUTO_INCREMENT_QUERY, db.getAutoIncrementValueSQL(null));
  }
  
  @Test
  public void getSequenceSQL()  {
    final H2Database db = new H2Database("host", "1234", "sid");
    final String idSource = "seq";
    assertEquals(H2Database.SEQUENCE_VALUE_QUERY + idSource, db.getSequenceSQL(idSource));
  }

  @Test
  public void getURL() {
    H2Database db = new H2Database("host", "1234", "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals(H2Database.URL_PREFIX + "//host:1234/sid;user=scott;password=tiger", db.getURL(props));
    db = new H2Database("dbname");
    assertEquals(H2Database.URL_PREFIX + "dbname;user=scott;password=tiger", db.getURL(props));
  }

  @Test(expected = RuntimeException.class)
  public void getURLMissingProperties() {
    final H2Database db = new H2Database(null, null, null);
    db.getURL(null);
  }
}