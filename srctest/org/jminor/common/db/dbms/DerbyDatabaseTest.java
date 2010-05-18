/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Properties;

public class DerbyDatabaseTest {

  @Test
  public void test() {
    DerbyDatabase db = new DerbyDatabase("host", "1234", "sid");
    final Properties props = new Properties();
    props.put("user", "scott");
    props.put("password", "tiger");
    assertTrue(db.supportsIsValid());
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
    assertEquals("select IDENTITY_VAL_LOCAL() from id_source", db.getAutoIncrementValueSQL("id_source"));
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
