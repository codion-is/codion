/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.mariadb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MariaDBDatabaseTest {

  private static final String URL = "jdbc:mariadb://host:1234/sid";

  @Test
  void getName() {
    MariaDBDatabase database = new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname");
    assertEquals("dbname", database.getName());
    database = new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname;option=true;option2=false");
    assertEquals("dbname", database.getName());
  }

  @Test
  void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new MariaDBDatabase(URL).getSequenceQuery("seq"));
  }

  @Test
  void supportsIsValid() {
    final MariaDBDatabase db = new MariaDBDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  void getAutoIncrementQuery() {
    final MariaDBDatabase db = new MariaDBDatabase(URL);
    assertEquals(MariaDBDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new MariaDBDatabase(null));
  }
}