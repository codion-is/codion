/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.dbms.DerbyDatabase;
import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.db.dbms.HSQLDatabase;
import org.jminor.common.db.dbms.MySQLDatabase;
import org.jminor.common.db.dbms.OracleDatabase;
import org.jminor.common.db.dbms.PostgreSQLDatabase;
import org.jminor.common.db.dbms.SQLServerDatabase;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DatabasesTest {

  public static Database createTestDatabaseInstance() {
    final String type = Database.DATABASE_TYPE.get();
    final String host = Database.DATABASE_HOST.get();
    final Integer port = Database.DATABASE_PORT.get();
    final String sid = Database.DATABASE_SID.get();
    final Boolean embedded = Database.DATABASE_EMBEDDED.get();
    final Boolean embeddedInMemory = Database.DATABASE_EMBEDDED_IN_MEMORY.get();
    final String initScript = Database.DATABASE_INIT_SCRIPT.get();
    try {
      Database.DATABASE_TYPE.set(type == null ? Database.Type.H2.toString() : type);
      Database.DATABASE_HOST.set(host == null ? "h2db/h2" : host);
      Database.DATABASE_PORT.set(port);
      Database.DATABASE_SID.set(sid);
      Database.DATABASE_EMBEDDED.set(embedded == null ? true : embedded);
      Database.DATABASE_EMBEDDED_IN_MEMORY.set(embeddedInMemory == null ? true : embeddedInMemory);
      Database.DATABASE_INIT_SCRIPT.set(initScript == null ? "demos/src/main/sql/create_h2_db.sql" : initScript);

      return Databases.getInstance();
    }
    finally {
      setSystemProperties(type, host, port, sid, embedded, embeddedInMemory, initScript);
    }
  }

  @Test
  public void test() {
    final String type = Database.DATABASE_TYPE.get();
    final String host = Database.DATABASE_HOST.get();
    final Integer port = Database.DATABASE_PORT.get();
    final String sid = Database.DATABASE_SID.get();
    final Boolean embedded = Database.DATABASE_EMBEDDED.get();
    final Boolean embeddedInMemory = Database.DATABASE_EMBEDDED_IN_MEMORY.get();
    final String initScript = Database.DATABASE_INIT_SCRIPT.get();
    try {
      Database.DATABASE_TYPE.set(Database.Type.DERBY.toString());
      Database.DATABASE_HOST.set("host");
      Database.DATABASE_EMBEDDED.set(true);
      Database database = Databases.getInstance();
      assertTrue(database instanceof DerbyDatabase);

      Database.DATABASE_TYPE.set(Database.Type.H2.toString());
      Database.DATABASE_HOST.set("host");
      Database.DATABASE_EMBEDDED.set(true);
      database = Databases.getInstance();
      assertTrue(database instanceof H2Database);

      Database.DATABASE_TYPE.set(Database.Type.HSQL.toString());
      Database.DATABASE_HOST.set("host");
      Database.DATABASE_EMBEDDED.set(true);
      database = Databases.getInstance();
      assertTrue(database instanceof HSQLDatabase);

      Database.DATABASE_TYPE.set(Database.Type.MYSQL.toString());
      Database.DATABASE_PORT.set(3306);
      Database.DATABASE_SID.set("sid");
      database = Databases.getInstance();
      assertTrue(database instanceof MySQLDatabase);

      Database.DATABASE_TYPE.set(Database.Type.ORACLE.toString());
      Database.DATABASE_PORT.set(1521);
      Database.DATABASE_SID.set("sid");
      database = Databases.getInstance();
      assertTrue(database instanceof OracleDatabase);

      Database.DATABASE_TYPE.set(Database.Type.POSTGRESQL.toString());
      Database.DATABASE_PORT.set(5435);
      Database.DATABASE_SID.set("sid");
      database = Databases.getInstance();
      assertTrue(database instanceof PostgreSQLDatabase);

      Database.DATABASE_TYPE.set(Database.Type.SQLSERVER.toString());
      Database.DATABASE_PORT.set(7414);
      Database.DATABASE_SID.set("sid");
      database = Databases.getInstance();
      assertTrue(database instanceof SQLServerDatabase);
      assertTrue(database == Databases.createInstance());
    }
    finally {
      setSystemProperties(type, host, port, sid, embedded, embeddedInMemory, initScript);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void createInstanceUnknownDatabaseType() {
    final String type = Database.DATABASE_TYPE.get();
    try {
      Database.DATABASE_TYPE.set("what");
      Databases.getInstance();
    }
    finally {
      if (type != null) {
        Database.DATABASE_TYPE.set(type);
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void createInstanceNoDatabaseType() {
    final String type = Database.DATABASE_TYPE.get();
    try {
      Database.DATABASE_TYPE.set(null);
      Databases.getInstance();
    }
    finally {
      if (type != null) {
        Database.DATABASE_TYPE.set(type);
      }
    }
  }

  private static void setSystemProperties(final String type, final String host, final Integer port, final String sid,
                                          final Boolean embedded, final Boolean embeddedInMemory, final String initScript) {
    if (type != null) {
      Database.DATABASE_TYPE.set(type);
    }
    if (host != null) {
      Database.DATABASE_HOST.set(host);
    }
    if (port != null) {
      Database.DATABASE_PORT.set(port);
    }
    if (sid != null) {
      Database.DATABASE_SID.set(sid);
    }
    if (embedded != null) {
      Database.DATABASE_EMBEDDED.set(embedded);
    }
    if (embeddedInMemory != null) {
      Database.DATABASE_EMBEDDED_IN_MEMORY.set(embeddedInMemory);
    }
    if (initScript != null) {
      Database.DATABASE_INIT_SCRIPT.set(initScript);
    }
  }
}
