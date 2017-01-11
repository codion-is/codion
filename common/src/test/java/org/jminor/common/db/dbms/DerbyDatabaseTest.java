/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.Database;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class DerbyDatabaseTest {

  @Test(expected = UnsupportedOperationException.class)
  public void getSequenceSQL() {
    new DerbyDatabase("host", "1234", "sid").getSequenceSQL("seq");
  }

  @Test
  public void supportsIsValid() {
    final DerbyDatabase db = new DerbyDatabase("host", "1234", "sid");
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void supportsNoWait() {
    final DerbyDatabase db = new DerbyDatabase("host", "1234", "sid");
    assertFalse(db.supportsNowait());
  }

  @Test
  public void getAuthenticationInfo() {
    final DerbyDatabase db = new DerbyDatabase("host", "1234", "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
  }

  @Test
  public void getAutoIncrementValueSQL() {
    final DerbyDatabase db = new DerbyDatabase("host", "1234", "sid");
    final String idSource = "id_source";
    assertEquals(DerbyDatabase.AUTO_INCREMENT_QUERY + idSource, db.getAutoIncrementValueSQL(idSource));
  }

  @Test
  public void getURL() {
    DerbyDatabase db = new DerbyDatabase("host", "1234", "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    assertEquals("jdbc:derby://host:1234/sid;user=scott", db.getURL(props));
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("jdbc:derby://host:1234/sid;user=scott;password=tiger", db.getURL(props));

    db = new DerbyDatabase("dbname");
    assertEquals("jdbc:derby:dbname;user=scott;password=tiger", db.getURL(props));
  }

  @Test(expected = RuntimeException.class)
  public void getURLMissingProperties() {
    final Database db = new DerbyDatabase(null, null, null);
    db.getURL(null);
  }
}
