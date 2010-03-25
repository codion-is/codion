package org.jminor.common.db.dbms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Properties;

public class HSQLDatabaseTest {

  @Test
  public void test() {
    HSQLDatabase db = new HSQLDatabase("host", "1234", "sid");
    final Properties props = new Properties();
    props.put("user", "scott");
    props.put("password", "tiger");
    assertTrue(db.supportsIsValid());
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
    assertEquals("IDENTITY()", db.getAutoIncrementValueSQL(null));
    assertEquals("select next value for seq", db.getSequenceSQL("seq"));
    assertEquals("jdbc:hsqldb:hsql//host:1234/sid;user=scott;password=tiger", db.getURL(props));
    final Timestamp date = new Timestamp(System.currentTimeMillis());
    assertEquals("'" + new SimpleDateFormat("yyyy-MM-dd").format(date) + "'", db.getSQLDateString(date, false));
    assertEquals("'" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date)  + "'", db.getSQLDateString(date, true));

    db = new HSQLDatabase("dbname");
    assertEquals("jdbc:hsqldb:file:dbname;user=scott;password=tiger", db.getURL(props));
  }
}