/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.sqlserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SQLServerDatabaseTest {

  private static final String URL = "jdbc:sqlserver://host:1234;databaseName=sid";

  @Test
  public void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new SQLServerDatabase(URL).getSequenceQuery("seq"));
  }

  @Test
  public void supportsIsValid() {
    final SQLServerDatabase db = new SQLServerDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final SQLServerDatabase db = new SQLServerDatabase(URL);
    assertNull(db.getAuthenticationInfo(null));
  }

  @Test
  public void getAutoIncrementQuery() {
    final SQLServerDatabase db = new SQLServerDatabase(URL);
    assertEquals(SQLServerDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new SQLServerDatabase(null));
  }
}