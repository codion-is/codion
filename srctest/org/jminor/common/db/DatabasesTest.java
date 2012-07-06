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
import static org.junit.Assert.fail;

public class DatabasesTest {

  @Test
  public void getDatabaseStatistics() {
    Databases.getDatabaseStatistics();
  }

  @Test
  public void test() {
    final String type = System.getProperty(Database.DATABASE_TYPE);
    final String host = System.getProperty(Database.DATABASE_HOST);
    final String embedded = System.getProperty(Database.DATABASE_EMBEDDED);

    try {
      System.setProperty(Database.DATABASE_TYPE, Database.DERBY);
      System.setProperty(Database.DATABASE_HOST, "host");
      System.setProperty(Database.DATABASE_EMBEDDED, "true");
      Database database = Databases.createInstance();
      assertTrue(database instanceof DerbyDatabase);

      System.setProperty(Database.DATABASE_TYPE, Database.H2);
      System.setProperty(Database.DATABASE_HOST, "host");
      System.setProperty(Database.DATABASE_EMBEDDED, "true");
      database = Databases.createInstance();
      assertTrue(database instanceof H2Database);

      System.setProperty(Database.DATABASE_TYPE, Database.HSQL);
      System.setProperty(Database.DATABASE_HOST, "host");
      System.setProperty(Database.DATABASE_EMBEDDED, "true");
      database = Databases.createInstance();
      assertTrue(database instanceof HSQLDatabase);

      System.setProperty(Database.DATABASE_TYPE, Database.MYSQL);
      System.setProperty(Database.DATABASE_PORT, "3306");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = Databases.createInstance();
      assertTrue(database instanceof MySQLDatabase);

      System.setProperty(Database.DATABASE_TYPE, Database.ORACLE);
      System.setProperty(Database.DATABASE_PORT, "1521");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = Databases.createInstance();
      assertTrue(database instanceof OracleDatabase);

      System.setProperty(Database.DATABASE_TYPE, Database.POSTGRESQL);
      System.setProperty(Database.DATABASE_PORT, "5435");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = Databases.createInstance();
      assertTrue(database instanceof PostgreSQLDatabase);

      System.setProperty(Database.DATABASE_TYPE, Database.SQLSERVER);
      System.setProperty(Database.DATABASE_PORT, "7414");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = Databases.createInstance();
      assertTrue(database instanceof SQLServerDatabase);

      try {
        System.setProperty(Database.DATABASE_TYPE, "what");
        Databases.createInstance();
        fail();
      }
      catch (IllegalArgumentException e) {}
      try {
        System.clearProperty(Database.DATABASE_TYPE);
        Databases.createInstance();
        fail();
      }
      catch (IllegalArgumentException e) {}
    }
    finally {
      System.setProperty(Database.DATABASE_TYPE, type);
      System.setProperty(Database.DATABASE_HOST, host);
      System.setProperty(Database.DATABASE_EMBEDDED, embedded);
    }
  }
}
