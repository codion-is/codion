/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.h2database;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class H2DatabaseTest {

  @Test
  void getDatabaseName() {
    H2Database database = new H2Database("jdbc:h2:file:C:/data/sample;option=true;option2=false");
    assertEquals("C:/data/sample", database.getName());
    database = new H2Database("jdbc:h2:C:/data/sample;option=true;option2=false");
    assertEquals("C:/data/sample", database.getName());
    database = new H2Database("jdbc:h2:mem:sampleDb;option=true;option2=false");
    assertEquals("sampleDb", database.getName());
    database = new H2Database("jdbc:h2:mem:");
    assertEquals("private", database.getName());
    database = new H2Database("jdbc:h2:tcp://sample.Db:1234");
    assertEquals("sample.Db:1234", database.getName());
    database = new H2Database("jdbc:h2:tcp://sample.Db:1234;option=true;option2=false");
    assertEquals("sample.Db:1234", database.getName());
    database = new H2Database("jdbc:h2:zip:db.zip!/h2db");
    assertEquals("db.zip!/h2db", database.getName());
    database = new H2Database("jdbc:h2:zip:db.zip!/h2db;option");
    assertEquals("db.zip!/h2db", database.getName());
  }

  @Test
  void getSequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new H2Database("url").getSequenceQuery(null));
  }

  @Test
  void supportsIsValid() {
    H2Database db = new H2Database("url");
    assertTrue(db.supportsIsValid());
  }

  @Test
  void getAutoIncrementQuery() {
    H2Database db = new H2Database("url");
    assertEquals(H2Database.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  void getSequenceQuery() {
    H2Database db = new H2Database("url");
    final String idSource = "seq";
    assertEquals(H2Database.SEQUENCE_VALUE_QUERY + idSource, db.getSequenceQuery(idSource));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new H2Database(null));
  }

  @Test
  void multipleDatabases() throws DatabaseException, SQLException, IOException {
    File file1 = File.createTempFile("h2db_test_1", ".sql");
    File file2 = File.createTempFile("h2db_test_2", ".sql");
    Files.write(file1.toPath(), singletonList("create schema scott; create table scott.test1 (id int);"));
    Files.write(file2.toPath(), singletonList("create schema scott; create table scott.test2 (id int);"));

    final String url1 = "jdbc:h2:mem:test1";
    final String url2 = "jdbc:h2:mem:test2";

    User user = User.user("sa");
    H2Database db1 = new H2Database(url1, singletonList(file1.getAbsolutePath()));
    H2Database db2 = new H2Database(url2, singletonList(file2.getAbsolutePath()));
    Connection connection1 = db1.createConnection(user);
    Connection connection2 = db2.createConnection(user);
    connection1.prepareCall("select id from scott.test1").executeQuery();
    connection2.prepareCall("select id from scott.test2").executeQuery();
    connection1.close();
    connection2.close();
    file1.delete();
    file2.delete();
  }

  @Test
  void fileDatabase() throws DatabaseException, SQLException {
    File tempDir = new File(System.getProperty("java.io.tmpdir"));
    String url = "jdbc:h2:file:" + tempDir.getAbsolutePath() + "/h2db/database";
    File dbFile = new File(tempDir.getAbsolutePath() + "/h2db/database.mv.db");
    dbFile.deleteOnExit();
    assertFalse(dbFile.exists());

    H2Database database = new H2Database(url, singletonList("src/test/resources/create_schema.sql"));
    assertTrue(dbFile.exists());

    User user = User.parse("scott:tiger");

    Connection connection = database.createConnection(user);
    connection.prepareStatement("select id from test.test_table").execute();
    connection.close();

    H2Database database2 = new H2Database(url, singletonList("src/test/resources/create_schema.sql"));
    connection = database2.createConnection(user);
    connection.prepareStatement("select id from test.test_table").execute();
    connection.close();

    //test old url type
    H2Database database3 = new H2Database("jdbc:h2:" + tempDir.getAbsolutePath() + "/h2db/database", singletonList("src/test/resources/create_schema.sql"));
    connection = database3.createConnection(user);
    connection.prepareStatement("select id from test.test_table").execute();
    connection.close();

    File parentDir = dbFile.getParentFile();
    dbFile.delete();
    parentDir.delete();
  }
}