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
    final String type = System.getProperty(Database.DATABASE_TYPE);
    final String host = System.getProperty(Database.DATABASE_HOST);
    final String port = System.getProperty(Database.DATABASE_PORT, "1234");
    final String sid = System.getProperty(Database.DATABASE_SID, "sid");
    final String embedded = System.getProperty(Database.DATABASE_EMBEDDED, "false");
    final String embeddedInMemory = System.getProperty(Database.DATABASE_EMBEDDED_IN_MEMORY, "false");
    final String initScript = System.getProperty(H2Database.DATABASE_INIT_SCRIPT);
    try {
      System.setProperty(Database.DATABASE_TYPE, type == null ? Database.Type.H2.toString() : type);
      System.setProperty(Database.DATABASE_HOST, host == null ? "h2db/h2" : host);
      System.setProperty(Database.DATABASE_PORT, port);
      System.setProperty(Database.DATABASE_SID, sid);
      System.setProperty(Database.DATABASE_EMBEDDED, embedded == null ? "true" : embedded);
      System.setProperty(Database.DATABASE_EMBEDDED_IN_MEMORY, embeddedInMemory == null ? "true" : embeddedInMemory);
      System.setProperty(H2Database.DATABASE_INIT_SCRIPT, initScript == null ? "demos/src/main/sql/create_h2_db.sql" : initScript);

      return Databases.getInstance();
    }
    finally {
      setSystemProperties(type, host, port, sid, embedded, embeddedInMemory, initScript);
    }
  }

  @Test
  public void test() {
    final String type = System.getProperty(Database.DATABASE_TYPE);
    final String host = System.getProperty(Database.DATABASE_HOST);
    final String port = System.getProperty(Database.DATABASE_PORT, "1234");
    final String sid = System.getProperty(Database.DATABASE_SID, "sid");
    final String embedded = System.getProperty(Database.DATABASE_EMBEDDED, "false");
    final String embeddedInMemory = System.getProperty(Database.DATABASE_EMBEDDED_IN_MEMORY, "false");
    final String initScript = System.getProperty(H2Database.DATABASE_INIT_SCRIPT);
    try {
      System.setProperty(Database.DATABASE_TYPE, Database.Type.DERBY.toString());
      System.setProperty(Database.DATABASE_HOST, "host");
      System.setProperty(Database.DATABASE_EMBEDDED, "true");
      Database database = Databases.getInstance();
      assertTrue(database instanceof DerbyDatabase);

      System.setProperty(Database.DATABASE_TYPE, Database.Type.H2.toString());
      System.setProperty(Database.DATABASE_HOST, "host");
      System.setProperty(Database.DATABASE_EMBEDDED, "true");
      database = Databases.getInstance();
      assertTrue(database instanceof H2Database);

      System.setProperty(Database.DATABASE_TYPE, Database.Type.HSQL.toString());
      System.setProperty(Database.DATABASE_HOST, "host");
      System.setProperty(Database.DATABASE_EMBEDDED, "true");
      database = Databases.getInstance();
      assertTrue(database instanceof HSQLDatabase);

      System.setProperty(Database.DATABASE_TYPE, Database.Type.MYSQL.toString());
      System.setProperty(Database.DATABASE_PORT, "3306");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = Databases.getInstance();
      assertTrue(database instanceof MySQLDatabase);

      System.setProperty(Database.DATABASE_TYPE, Database.Type.ORACLE.toString());
      System.setProperty(Database.DATABASE_PORT, "1521");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = Databases.getInstance();
      assertTrue(database instanceof OracleDatabase);

      System.setProperty(Database.DATABASE_TYPE, Database.Type.POSTGRESQL.toString());
      System.setProperty(Database.DATABASE_PORT, "5435");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = Databases.getInstance();
      assertTrue(database instanceof PostgreSQLDatabase);

      System.setProperty(Database.DATABASE_TYPE, Database.Type.SQLSERVER.toString());
      System.setProperty(Database.DATABASE_PORT, "7414");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = Databases.getInstance();
      assertTrue(database instanceof SQLServerDatabase);
    }
    finally {
      setSystemProperties(type, host, port, sid, embedded, embeddedInMemory, initScript);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void addOperationExisting() {
    final DatabaseConnection.Operation operation = new AbstractProcedure<DatabaseConnection>("operationId", "test") {
      @Override
      public void execute(final DatabaseConnection databaseConnection, final Object... arguments) {}
    };
    Databases.addOperation(operation);
    Databases.addOperation(operation);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getFunctionNonExisting() {
    Databases.getFunction("nonexistingfunctionid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void getProcedureNonExisting() {
    Databases.getProcedure("nonexistingprocedureid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void createInstanceUnknownDatabaseType() {
    final String type = System.getProperty(Database.DATABASE_TYPE);
    try {
      System.setProperty(Database.DATABASE_TYPE, "what");
      Databases.getInstance();
    }
    finally {
      if (type != null) {
        System.setProperty(Database.DATABASE_TYPE, type);
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void createInstanceNoDatabaseType() {
    final String type = System.getProperty(Database.DATABASE_TYPE);
    try {
      System.clearProperty(Database.DATABASE_TYPE);
      Databases.getInstance();
    }
    finally {
      if (type != null) {
        System.setProperty(Database.DATABASE_TYPE, type);
      }
    }
  }

  private static void setSystemProperties(final String type, final String host, final String port, final String sid,
                                          final String embedded, final String embeddedInMemory, final String initScript) {
    if (type != null) {
      System.setProperty(Database.DATABASE_TYPE, type);
    }
    if (host != null) {
      System.setProperty(Database.DATABASE_HOST, host);
    }
    if (port != null) {
      System.setProperty(Database.DATABASE_PORT, port);
    }
    if (sid != null) {
      System.setProperty(Database.DATABASE_SID, sid);
    }
    if (embedded != null) {
      System.setProperty(Database.DATABASE_EMBEDDED, embedded);
    }
    if (embeddedInMemory != null) {
      System.setProperty(Database.DATABASE_EMBEDDED_IN_MEMORY, embeddedInMemory);
    }
    if (initScript != null) {
      System.setProperty(H2Database.DATABASE_INIT_SCRIPT, initScript);
    }
  }
}
