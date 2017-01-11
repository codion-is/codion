/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.Test;

import static org.junit.Assert.*;

public class SQLServerDatabaseTest {

  @Test(expected = UnsupportedOperationException.class)
  public void getSequenceSQL() {
    new SQLServerDatabase("host", "1234", "sid").getSequenceSQL("seq");
  }
  
  @Test
  public void supportsIsValid() {
    final SQLServerDatabase db = new SQLServerDatabase("host", "1234", "sid");
    assertTrue(db.supportsIsValid());
  }
  
  @Test
  public void getAuthenticationInfo() {
    final SQLServerDatabase db = new SQLServerDatabase("host", "1234", "sid");
    assertNull(db.getAuthenticationInfo(null));
  }
  
  @Test
  public void getAutoIncrementValueSQL() {
    final SQLServerDatabase db = new SQLServerDatabase("host", "1234", "sid");
    assertEquals(SQLServerDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementValueSQL(null));
  }
  
  @Test
  public void getURL() {
    final SQLServerDatabase db = new SQLServerDatabase("host", "1234", "sid");
    assertEquals("jdbc:sqlserver://host:1234;databaseName=sid", db.getURL(null));
  }

  @Test(expected = RuntimeException.class)
  public void getURLMissingProperties() {
    final SQLServerDatabase db = new SQLServerDatabase(null, null, null);
    db.getURL(null);
  }
}