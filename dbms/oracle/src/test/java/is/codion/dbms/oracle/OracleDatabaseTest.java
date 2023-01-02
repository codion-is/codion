/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.oracle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OracleDatabaseTest {

  public static final String URL = "jdbc:oracle:thin:@host:1234:sid";

  @Test
  void name() {
    OracleDatabase database = new OracleDatabase("jdbc:oracle:thin:@host.com:1234:sid");
    assertEquals("sid", database.name());
    database = new OracleDatabase("jdbc:oracle:thin:@host.com:1234:sid;option=true;option2=false");
    assertEquals("sid", database.name());
    database = new OracleDatabase("jdbc:oracle:thin:@host.com:1234/sid;option=true;option2=false");
    assertEquals("sid", database.name());
    database = new OracleDatabase("jdbc:oracle:thin:/@sid");
    assertEquals("sid", database.name());
  }

  @Test
  void sequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new OracleDatabase(URL).sequenceQuery(null));
  }

  @Test
  void supportsIsValid() {
    OracleDatabase db = new OracleDatabase(URL);
    assertFalse(db.supportsIsValid());
  }

  @Test
  void autoIncrementQuery() {
    OracleDatabase db = new OracleDatabase(URL);
    assertEquals("select seq.currval from dual", db.autoIncrementQuery("seq"));
  }

  @Test
  void sequenceQuery() {
    OracleDatabase db = new OracleDatabase(URL);
    assertEquals("select seq.nextval from dual", db.sequenceQuery("seq"));
  }

  @Test
  void url() {
    OracleDatabase db = new OracleDatabase(URL);
    assertEquals("jdbc:oracle:thin:@host:1234:sid", db.url());
  }

  @Test
  void checkConnectionQuery() {
    OracleDatabase db = new OracleDatabase(URL);
    assertEquals(OracleDatabase.CHECK_QUERY, db.checkConnectionQuery());
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new OracleDatabase(null));
  }
}