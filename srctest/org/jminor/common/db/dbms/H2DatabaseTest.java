/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Properties;

public class H2DatabaseTest {

  @Test
  public void test() {
    H2Database db = new H2Database("host", "1234", "sid");
    final Properties props = new Properties();
    props.put("user", "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertTrue(db.supportsIsValid());
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
    assertEquals(H2Database.AUTO_INCREMENT_QUERY, db.getAutoIncrementValueSQL(null));
    final String idSource = "seq";
    assertEquals(H2Database.SEQUENCE_VALUE_QUERY + idSource, db.getSequenceSQL(idSource));
    assertEquals("jdbc:h2://host:1234/sid;user=scott;password=tiger", db.getURL(props));

    db = new H2Database("dbname");
    assertEquals("jdbc:h2:dbname;user=scott;password=tiger", db.getURL(props));
  }
}