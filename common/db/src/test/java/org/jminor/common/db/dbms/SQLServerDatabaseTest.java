/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SQLServerDatabaseTest {

  @Test
  public void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new SQLServerDatabase("host", 1234, "sid").getSequenceQuery("seq"));
  }
  
  @Test
  public void supportsIsValid() {
    final SQLServerDatabase db = new SQLServerDatabase("host", 1234, "sid");
    assertTrue(db.supportsIsValid());
  }
  
  @Test
  public void getAuthenticationInfo() {
    final SQLServerDatabase db = new SQLServerDatabase("host", 1234, "sid");
    assertNull(db.getAuthenticationInfo(null));
  }
  
  @Test
  public void getAutoIncrementQuery() {
    final SQLServerDatabase db = new SQLServerDatabase("host", 1234, "sid");
    assertEquals(SQLServerDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }
  
  @Test
  public void getURL() {
    final SQLServerDatabase db = new SQLServerDatabase("host", 1234, "sid");
    assertEquals("jdbc:sqlserver://host:1234;databaseName=sid", db.getURL(null));
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new SQLServerDatabase(null, null, null));
  }
}