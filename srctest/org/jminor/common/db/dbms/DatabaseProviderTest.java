package org.jminor.common.db.dbms;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class DatabaseProviderTest {

  @Test
  public void test() {
    final String type = System.getProperty(Database.DATABASE_TYPE);
    final String host = System.getProperty(Database.DATABASE_HOST);
    final String embedded = System.getProperty(Database.DATABASE_EMBEDDED);

    try {
      System.setProperty(Database.DATABASE_TYPE, "derby");
      System.setProperty(Database.DATABASE_HOST, "host");
      System.setProperty(Database.DATABASE_EMBEDDED, "true");
      Database database = DatabaseProvider.createInstance();
      assertTrue(database instanceof DerbyDatabase);

      System.setProperty(Database.DATABASE_TYPE, "h2");
      System.setProperty(Database.DATABASE_HOST, "host");
      System.setProperty(Database.DATABASE_EMBEDDED, "true");
      database = DatabaseProvider.createInstance();
      assertTrue(database instanceof H2Database);

      System.setProperty(Database.DATABASE_TYPE, "hsql");
      System.setProperty(Database.DATABASE_HOST, "host");
      System.setProperty(Database.DATABASE_EMBEDDED, "true");
      database = DatabaseProvider.createInstance();
      assertTrue(database instanceof HSQLDatabase);

      System.setProperty(Database.DATABASE_TYPE, "mysql");
      System.setProperty(Database.DATABASE_PORT, "3306");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = DatabaseProvider.createInstance();
      assertTrue(database instanceof MySQLDatabase);

      System.setProperty(Database.DATABASE_TYPE, "oracle");
      System.setProperty(Database.DATABASE_PORT, "1521");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = DatabaseProvider.createInstance();
      assertTrue(database instanceof OracleDatabase);

      System.setProperty(Database.DATABASE_TYPE, "postgresql");
      System.setProperty(Database.DATABASE_PORT, "5435");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = DatabaseProvider.createInstance();
      assertTrue(database instanceof PostgreSQLDatabase);

      System.setProperty(Database.DATABASE_TYPE, "sqlserver");
      System.setProperty(Database.DATABASE_PORT, "7414");
      System.setProperty(Database.DATABASE_SID, "sid");
      database = DatabaseProvider.createInstance();
      assertTrue(database instanceof SQLServerDatabase);

      try {
        System.setProperty(Database.DATABASE_TYPE, "what");
        DatabaseProvider.createInstance();
        fail();
      }
      catch (Exception e) {}
      try {
        System.setProperty(Database.DATABASE_TYPE, null);
        DatabaseProvider.createInstance();
        fail();
      }
      catch (Exception e) {}
    }
    finally {
      System.setProperty(Database.DATABASE_TYPE, type);
      System.setProperty(Database.DATABASE_HOST, host);
      System.setProperty(Database.DATABASE_EMBEDDED, embedded);
    }
  }
}
