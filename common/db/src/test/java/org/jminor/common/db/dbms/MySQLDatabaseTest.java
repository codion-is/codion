/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLDatabaseTest {

  @Test
  public void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new MySQLDatabase("host", 1234, "sid").getSequenceQuery("seq"));
  }

  @Test
  public void supportsIsValid() {
    final MySQLDatabase db = new MySQLDatabase("host", 1234, "sid");
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final MySQLDatabase db = new MySQLDatabase("host", 1234, "sid");
    assertNull(db.getAuthenticationInfo(null));
  }

  @Test
  public void getAutoIncrementQuery() {
    final MySQLDatabase db = new MySQLDatabase("host", 1234, "sid");
    assertEquals(MySQLDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  public void getURL() {
    final MySQLDatabase db = new MySQLDatabase("host", 1234, "sid");
    assertEquals("jdbc:mysql://host:1234/sid", db.getURL(null));
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new MySQLDatabase(null, null, null));
  }
}