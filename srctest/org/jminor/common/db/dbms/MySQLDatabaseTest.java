/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.Test;

import static org.junit.Assert.*;

public class MySQLDatabaseTest {

  @Test(expected = UnsupportedOperationException.class)
  public void getSequenceSQL() {
    new MySQLDatabase("host", "1234", "sid").getSequenceSQL("seq");
  }
  
  @Test
  public void supportsIsValid() {
    final MySQLDatabase db = new MySQLDatabase("host", "1234", "sid");
    assertTrue(db.supportsIsValid());
  }
  
  @Test
  public void getAuthenticationInfo() {
    final MySQLDatabase db = new MySQLDatabase("host", "1234", "sid");
    assertNull(db.getAuthenticationInfo(null));
  }
  
  @Test
  public void getAutoIncrementValueSQL() {
    final MySQLDatabase db = new MySQLDatabase("host", "1234", "sid");
    assertEquals(MySQLDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementValueSQL(null));
  }
  
  @Test
  public void getURL() {
    final MySQLDatabase db = new MySQLDatabase("host", "1234", "sid");
    assertEquals("jdbc:mysql://host:1234/sid", db.getURL(null));
  }
  
  @Test(expected = RuntimeException.class)
  public void getURLMissingProperties() {
    final MySQLDatabase db = new MySQLDatabase(null, null, null);
    db.getURL(null);
  }
}