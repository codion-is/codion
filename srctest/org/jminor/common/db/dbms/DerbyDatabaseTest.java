/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.Database;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Properties;

public class DerbyDatabaseTest {

  @Test
  public void test() {
    DerbyDatabase db = new DerbyDatabase("host", "1234", "sid");
    final Properties props = new Properties();
    props.put("user", "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertTrue(db.supportsIsValid());
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
    final String idSource = "id_source";
    assertEquals(DerbyDatabase.AUTO_INCREMENT_QUERY + idSource, db.getAutoIncrementValueSQL(idSource));
    try {
      db.getSequenceSQL("seq");
      fail();
    }
    catch (RuntimeException e) {}
    assertEquals("jdbc:derby://host:1234/sid;user=scott;password=tiger", db.getURL(props));

    db = new DerbyDatabase("dbname");
    assertEquals("jdbc:derby:dbname;user=scott;password=tiger", db.getURL(props));
  }
}
