/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class H2DatabaseTest {

  @Test
  public void getSequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new H2Database("host", 1234, "sid").getSequenceQuery(null));
  }

  @Test
  public void supportsIsValid() {
    final H2Database db = new H2Database("host", 1234, "sid");
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final H2Database db = new H2Database("host", 1234, "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
  }

  @Test
  public void getAutoIncrementQuery() {
    final H2Database db = new H2Database("host", 1234, "sid");
    assertEquals(H2Database.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  public void getSequenceQuery() {
    final H2Database db = new H2Database("host", 1234, "sid");
    final String idSource = "seq";
    assertEquals(H2Database.SEQUENCE_VALUE_QUERY + idSource, db.getSequenceQuery(idSource));
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new H2Database(null, null, null));
  }

  @Test
  public void multipleDatabases() throws DatabaseException, SQLException {
    final User user = new User("sa", null);
    final File file1 = new File(H2Database.class.getClassLoader().getResource("org/jminor/common/db/dbms/h2db_test_1.sql").getFile());
    final File file2 = new File(H2Database.class.getClassLoader().getResource("org/jminor/common/db/dbms/h2db_test_2.sql").getFile());
    final H2Database db1 = new H2Database("test1", file1.getAbsolutePath(), true);
    final H2Database db2 = new H2Database("test2", file2.getAbsolutePath(), true);
    final Connection connection1 = db1.createConnection(user);
    final Connection connection2 = db2.createConnection(user);
    connection1.prepareCall("select id from scott.test1").executeQuery();
    connection2.prepareCall("select id from scott.test2").executeQuery();
    connection1.close();
    connection2.close();
  }
}