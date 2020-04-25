/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.h2database;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class H2DatabaseTest {

  @Test
  public void getSequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> H2Database.serverDatabase("host", 1234, "sid").getSequenceQuery(null));
  }

  @Test
  public void supportsIsValid() {
    final H2Database db = H2Database.serverDatabase("host", 1234, "sid");
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final H2Database db = H2Database.serverDatabase("host", 1234, "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
  }

  @Test
  public void getAutoIncrementQuery() {
    final H2Database db = H2Database.serverDatabase("host", 1234, "sid");
    assertEquals(H2Database.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  public void getSequenceQuery() {
    final H2Database db = H2Database.serverDatabase("host", 1234, "sid");
    final String idSource = "seq";
    assertEquals(H2Database.SEQUENCE_VALUE_QUERY + idSource, db.getSequenceQuery(idSource));
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> H2Database.serverDatabase(null, null, null));
  }

  @Test
  public void multipleDatabases() throws DatabaseException, SQLException, IOException {
    final File file1 = File.createTempFile("h2db_test_1", ".sql");
    final File file2 = File.createTempFile("h2db_test_2", ".sql");
    Files.write(file1.toPath(), singletonList("create schema scott; create table scott.test1 (id int);"));
    Files.write(file2.toPath(), singletonList("create schema scott; create table scott.test2 (id int);"));
    final User user = Users.user("sa");
    final H2Database db1 = H2Database.memoryDatabase("test1", file1.getAbsolutePath());
    final H2Database db2 = H2Database.memoryDatabase("test2", file2.getAbsolutePath());
    final Connection connection1 = db1.createConnection(user);
    final Connection connection2 = db2.createConnection(user);
    connection1.prepareCall("select id from scott.test1").executeQuery();
    connection2.prepareCall("select id from scott.test2").executeQuery();
    connection1.close();
    connection2.close();
    file1.delete();
    file2.delete();
  }
}