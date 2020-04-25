/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.sqlite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLiteDatabaseTest {

  @Test
  public void getURL() {
    final SQLiteDatabase database = SQLiteDatabase.sqliteDatabase("/net/test");
    assertEquals("jdbc:sqlite:/net/test", database.getURL(null));
  }

  @Test
  public void getAutoIncrementQuery() {
    final SQLiteDatabase database = SQLiteDatabase.sqliteDatabase("test");
    assertEquals("select last_insert_rowid()", database.getAutoIncrementQuery(null));
  }
}
