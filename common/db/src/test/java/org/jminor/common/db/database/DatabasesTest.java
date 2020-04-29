/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.database;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.Users;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DatabasesTest {

  @Test
  public void test() {
    final String type = Database.DATABASE_TYPE.get();
    final String url = Database.DATABASE_URL.get();
    final String initScript = Database.DATABASE_INIT_SCRIPT.get();
    Database.DATABASE_URL.set("dummy/url");
    Database.DATABASE_INIT_SCRIPT.set(null);
    try {
      Database.DATABASE_TYPE.set(null);
      assertThrows(IllegalArgumentException.class, Databases::getInstance);

      Database.DATABASE_TYPE.set(Database.Type.DERBY.toString());
      Database database = Databases.getInstance();
      assertSame(database.getType(), Database.Type.DERBY);

      Database.DATABASE_TYPE.set(Database.Type.H2.toString());
      database = Databases.getInstance();
      assertSame(database.getType(), Database.Type.H2);

      Database.DATABASE_TYPE.set(Database.Type.HSQL.toString());
      database = Databases.getInstance();
      assertSame(database.getType(), Database.Type.HSQL);

      Database.DATABASE_TYPE.set(Database.Type.MARIADB.toString());
      database = Databases.getInstance();
      assertSame(database.getType(), Database.Type.MARIADB);

      Database.DATABASE_TYPE.set(Database.Type.MYSQL.toString());
      database = Databases.getInstance();
      assertSame(database.getType(), Database.Type.MYSQL);

      Database.DATABASE_TYPE.set(Database.Type.ORACLE.toString());
      database = Databases.getInstance();
      assertSame(database.getType(), Database.Type.ORACLE);

      Database.DATABASE_TYPE.set(Database.Type.POSTGRESQL.toString());
      database = Databases.getInstance();
      assertSame(database.getType(), Database.Type.POSTGRESQL);

      Database.DATABASE_TYPE.set(Database.Type.SQLSERVER.toString());
      database = Databases.getInstance();
      assertSame(database.getType(), Database.Type.SQLSERVER);

      Database.DATABASE_TYPE.set(Database.Type.SQLITE.toString());
      database = Databases.getInstance();
      assertSame(database.getType(), Database.Type.SQLITE);

      Database.DATABASE_TYPE.set(Database.Type.OTHER.toString());
      assertThrows(IllegalArgumentException.class, Databases::getInstance);

      Database.DATABASE_TYPE.set("Unknown");
      assertThrows(IllegalArgumentException.class, Databases::getInstance);
    }
    finally {
      Database.DATABASE_URL.set(url);
      Database.DATABASE_TYPE.set(type);
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
  public void validateWithQuery() throws DatabaseException, SQLException {
    final Database testDatabase = new TestDatabase();
    final Connection connection = testDatabase.createConnection(Users.parseUser("scott:tiger"));
    assertTrue(Databases.isValid(connection, testDatabase, 2));
    connection.close();
    assertFalse(Databases.isValid(connection, testDatabase, 2));
  }

  private static final class TestDatabase extends AbstractDatabase {

    private final Database database;

    public TestDatabase() {
      super(Type.H2, "jdbc:h2:mem:h2db");
      this.database = Databases.getInstance();
    }

    @Override
    public String getName() {
      return database.getName();
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
  }
}
