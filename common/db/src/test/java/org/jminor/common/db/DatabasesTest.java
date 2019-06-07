/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.dbms.derby.DerbyDatabase;
import org.jminor.dbms.h2database.H2Database;
import org.jminor.dbms.hsqldb.HSQLDatabase;
import org.jminor.dbms.mysql.MySQLDatabase;
import org.jminor.dbms.oracle.OracleDatabase;
import org.jminor.dbms.postgresql.PostgreSQLDatabase;
import org.jminor.dbms.sqlserver.SQLServerDatabase;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabasesTest {

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
    }
    finally {
      Database.DATABASE_TYPE.set(type);
      Database.DATABASE_HOST.set(host);
      Database.DATABASE_PORT.set(port);
      Database.DATABASE_SID.set(sid);
      Database.DATABASE_EMBEDDED.set(embedded);
      Database.DATABASE_EMBEDDED_IN_MEMORY.set(embeddedInMemory);
      Database.DATABASE_INIT_SCRIPT.set(initScript);
    }
  }

  @Test
  public void closeSilently() {
    Databases.closeSilently((Statement) null);
    Databases.closeSilently((ResultSet) null);
    Databases.closeSilently((Connection) null);
  }

  @Test
  public void getDatabaseStatistics() {
    Databases.getDatabaseStatistics();
  }

  @Test
  public void validateWithQuery() throws DatabaseException, SQLException {
    final Database testDatabase = new TestDatabase();
    final Connection connection = testDatabase.createConnection(new User("scott", "tiger".toCharArray()));
    assertTrue(Databases.isValid(connection, testDatabase, 2));
    connection.close();
    assertFalse(Databases.isValid(connection, testDatabase, 2));
  }

  private static final class TestDatabase extends AbstractDatabase {

    private final Database database;

    public TestDatabase() {
      super(Type.H2, "org.h2.Driver");
      this.database = Databases.getInstance();
    }

    @Override
    public String getCheckConnectionQuery() {
      return "select 1 from dual";
    }

    @Override
    public boolean supportsIsValid() {
      return false;
    }

    @Override
    public String getAutoIncrementQuery(final String idSource) {
      return database.getAutoIncrementQuery(idSource);
    }

    @Override
    public String getURL(final Properties connectionProperties) {
      return database.getURL(connectionProperties);
    }
  }
}
