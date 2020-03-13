/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.mariadb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MariaDbDatabaseTest {

  @Test
  public void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new MariaDbDatabase("host", 1234, "sid").getSequenceQuery("seq"));
  }

  @Test
  public void supportsIsValid() {
    final MariaDbDatabase db = new MariaDbDatabase("host", 1234, "sid");
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final MariaDbDatabase db = new MariaDbDatabase("host", 1234, "sid");
    assertNull(db.getAuthenticationInfo(null));
  }

  @Test
  public void getAutoIncrementQuery() {
    final MariaDbDatabase db = new MariaDbDatabase("host", 1234, "sid");
    assertEquals(MariaDbDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  public void getURL() {
    final MariaDbDatabase db = new MariaDbDatabase("host", 1234, "sid");
    assertEquals("jdbc:mariadb://host:1234/sid", db.getURL(null));
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new MariaDbDatabase(null, null, null));
  }
}