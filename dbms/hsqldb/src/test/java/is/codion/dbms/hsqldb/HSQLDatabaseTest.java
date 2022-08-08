/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.hsqldb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HSQLDatabaseTest {
  
  private static final String URL = "jdbc:hsqldb:hsql//host:1234/sid";

  @Test
  void name() {
    HSQLDatabase database = new HSQLDatabase("jdbc:hsqldb:file:C:/data/sample;option=true;option2=false");
    assertEquals("C:/data/sample", database.name());
    database = new HSQLDatabase("jdbc:hsqldb:mem:sampleDb;option=true;option2=false");
    assertEquals("sampleDb", database.name());
    database = new HSQLDatabase("jdbc:hsqldb:mem:");
    assertEquals("private", database.name());
    database = new HSQLDatabase("jdbc:hsqldb:res:/dir/db");
    assertEquals("/dir/db", database.name());
  }

  @Test
  void sequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new HSQLDatabase(URL).sequenceQuery(null));
  }

  @Test
  void supportsIsValid() {
    HSQLDatabase db = new HSQLDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  void autoIncrementQuery() {
    HSQLDatabase db = new HSQLDatabase(URL);
    assertEquals(HSQLDatabase.AUTO_INCREMENT_QUERY, db.autoIncrementQuery(null));
  }

  @Test
  void sequenceQuery() {
    HSQLDatabase db = new HSQLDatabase(URL);
    final String idSource = "seq";
    assertEquals(HSQLDatabase.SEQUENCE_VALUE_QUERY + idSource, db.sequenceQuery(idSource));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new HSQLDatabase(null));
  }
}