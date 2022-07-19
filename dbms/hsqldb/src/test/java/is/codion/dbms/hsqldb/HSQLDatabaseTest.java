/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.hsqldb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HSQLDatabaseTest {
  
  private static final String URL = "jdbc:hsqldb:hsql//host:1234/sid";

  @Test
  void getDatabaseName() {
    HSQLDatabase database = new HSQLDatabase("jdbc:hsqldb:file:C:/data/sample;option=true;option2=false");
    assertEquals("C:/data/sample", database.getName());
    database = new HSQLDatabase("jdbc:hsqldb:mem:sampleDb;option=true;option2=false");
    assertEquals("sampleDb", database.getName());
    database = new HSQLDatabase("jdbc:hsqldb:mem:");
    assertEquals("private", database.getName());
    database = new HSQLDatabase("jdbc:hsqldb:res:/dir/db");
    assertEquals("/dir/db", database.getName());
  }

  @Test
  void getSequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new HSQLDatabase(URL).getSequenceQuery(null));
  }

  @Test
  void supportsIsValid() {
    HSQLDatabase db = new HSQLDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  void getAutoIncrementQuery() {
    HSQLDatabase db = new HSQLDatabase(URL);
    assertEquals(HSQLDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  void getSequenceQuery() {
    HSQLDatabase db = new HSQLDatabase(URL);
    final String idSource = "seq";
    assertEquals(HSQLDatabase.SEQUENCE_VALUE_QUERY + idSource, db.getSequenceQuery(idSource));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new HSQLDatabase(null));
  }
}