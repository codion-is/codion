/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.hsqldb;

import org.jminor.common.db.database.Database;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class HSQLDatabaseTest {
  
  private static final String URL = "jdbc:hsqldb:hsql//host:1234/sid";

  @Test
  public void getSequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new HSQLDatabase(URL).getSequenceQuery(null));
  }

  @Test
  public void supportsIsValid() {
    final HSQLDatabase db = new HSQLDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final HSQLDatabase db = new HSQLDatabase(URL);
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
  }

  @Test
  public void getAutoIncrementQuery() {
    final HSQLDatabase db = new HSQLDatabase(URL);
    assertEquals(HSQLDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  public void getSequenceQuery() {
    final HSQLDatabase db = new HSQLDatabase(URL);
    final String idSource = "seq";
    assertEquals(HSQLDatabase.SEQUENCE_VALUE_QUERY + idSource, db.getSequenceQuery(idSource));
  }

  @Test
  public void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new HSQLDatabase(null));
  }
}