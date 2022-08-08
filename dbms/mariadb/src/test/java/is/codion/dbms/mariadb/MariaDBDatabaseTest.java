/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.mariadb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MariaDBDatabaseTest {

  private static final String URL = "jdbc:mariadb://host:1234/sid";

  @Test
  void name() {
    MariaDBDatabase database = new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname");
    assertEquals("dbname", database.name());
    database = new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname;option=true;option2=false");
    assertEquals("dbname", database.name());
  }

  @Test
  void sequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new MariaDBDatabase(URL).sequenceQuery("seq"));
  }

  @Test
  void supportsIsValid() {
    MariaDBDatabase db = new MariaDBDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  void autoIncrementQuery() {
    MariaDBDatabase db = new MariaDBDatabase(URL);
    assertEquals(MariaDBDatabase.AUTO_INCREMENT_QUERY, db.autoIncrementQuery(null));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new MariaDBDatabase(null));
  }
}