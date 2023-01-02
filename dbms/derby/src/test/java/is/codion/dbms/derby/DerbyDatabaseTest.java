/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.derby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DerbyDatabaseTest {

  private static final String URL = "jdbc:derby://host:1234/sid";

  @Test
  void name() {
    DerbyDatabase database = new DerbyDatabase("jdbc:derby:C:/data/sample;option=true;option2=false");
    assertEquals("C:/data/sample", database.name());
    database = new DerbyDatabase("jdbc:derby://sample.db:1234;option=true;option2=false");
    assertEquals("sample.db:1234", database.name());
    database = new DerbyDatabase("jdbc:derby://sample.db:1234");
    assertEquals("sample.db:1234", database.name());
    database = new DerbyDatabase("jdbc:derby://sample.db:1234/dbname");
    assertEquals("dbname", database.name());
  }

  @Test
  void sequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new DerbyDatabase(URL).sequenceQuery("seq"));
  }

  @Test
  void supportsIsValid() {
    DerbyDatabase db = new DerbyDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  void supportsNoWait() {
    DerbyDatabase db = new DerbyDatabase(URL);
    assertEquals("for update", db.selectForUpdateClause());
  }

  @Test
  void autoIncrementQuery() {
    DerbyDatabase db = new DerbyDatabase(URL);
    final String idSource = "id_source";
    assertEquals(DerbyDatabase.AUTO_INCREMENT_QUERY + idSource, db.autoIncrementQuery(idSource));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new DerbyDatabase(null));
  }
}
