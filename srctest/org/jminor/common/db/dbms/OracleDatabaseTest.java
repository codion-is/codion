package org.jminor.common.db.dbms;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class OracleDatabaseTest {

  @Test
  public void test() {
    OracleDatabase db = new OracleDatabase("host", "1234", "sid");
    assertFalse(db.supportsIsValid());
    assertNull(db.getAuthenticationInfo(null));
    assertEquals("select seq.currval from dual", db.getAutoIncrementValueSQL("seq"));
    assertEquals("select seq.nextval from dual", db.getSequenceSQL("seq"));
    assertEquals("jdbc:oracle:thin:@host:1234:sid", db.getURL(null));
    final Timestamp date = new Timestamp(System.currentTimeMillis());
    assertEquals("to_date('" + new SimpleDateFormat("dd-MM-yyyy").format(date) + "', 'DD-MM-YYYY')", db.getSQLDateString(date, false));
    assertEquals("to_date('" + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(date)  + "', 'DD-MM-YYYY HH24:MI')", db.getSQLDateString(date, true));
  }
}