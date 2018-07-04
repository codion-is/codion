/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLiteDatabaseTest {

  @Test
  public void getURL() {
    final SQLiteDatabase database = new SQLiteDatabase("/net/test");
    assertEquals("jdbc:sqlite:/net/test", database.getURL(null));
  }

  @Test
  public void getAutoIncrementQuery() {
    final SQLiteDatabase database = new SQLiteDatabase("test");
    assertEquals("select last_insert_rowid()", database.getAutoIncrementQuery(null));
  }
}
