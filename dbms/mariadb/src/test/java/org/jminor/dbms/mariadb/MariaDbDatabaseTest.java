/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.mariadb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MariaDbDatabaseTest {

  private static final String URL = "jdbc:mariadb://host:1234/sid";

  @Test
  public void getName() {
    MariaDbDatabase database = new MariaDbDatabase("jdbc:mariadb://host.com:1234/dbname");
    assertEquals("dbname", database.getName());
    database = new MariaDbDatabase("jdbc:mariadb://host.com:1234/dbname;option=true;option2=false");
    assertEquals("dbname", database.getName());
  }

  @Test
  public void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new MariaDbDatabase(URL).getSequenceQuery("seq"));
  }

  @Test
  public void supportsIsValid() {
    final MariaDbDatabase db = new MariaDbDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void getAutoIncrementQuery() {
    final MariaDbDatabase db = new MariaDbDatabase(URL);
    assertEquals(MariaDbDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  public void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new MariaDbDatabase(null));
  }
}