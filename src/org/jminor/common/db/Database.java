/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.dbms.IDatabase;

public class Database {

  private static IDatabase instance;

  public static IDatabase get() {
    if (instance == null) {
      try {
        String dbImplementationClass = System.getProperty(IDatabase.DATABASE_IMPLEMENTATION_CLASS);
        if (dbImplementationClass == null) {
          final String dbType = System.getProperty(IDatabase.DATABASE_TYPE);
          if (dbType == null)
            throw new IllegalArgumentException("Required system property missing: " + IDatabase.DATABASE_TYPE);

          if (dbType.equals(IDatabase.POSTGRESQL))
            dbImplementationClass = "org.jminor.common.db.dbms.PostgreSQLDatabase";
          else if (dbType.equals(IDatabase.MYSQL))
            dbImplementationClass = "org.jminor.common.db.dbms.MySQLDatabase";
          else if (dbType.equals(IDatabase.ORACLE))
            dbImplementationClass = "org.jminor.common.db.dbms.OracleDatabase";
          else if (dbType.equals(IDatabase.SQLSERVER))
            dbImplementationClass = "org.jminor.common.db.dbms.SQLServerDatabase";
          else if (dbType.equals(IDatabase.DERBY))
            dbImplementationClass = "org.jminor.common.db.dbms.DerbyDatabase";
          else if (dbType.equals(IDatabase.H2))
            dbImplementationClass = "org.jminor.common.db.dbms.H2Database";
          else
            throw new IllegalArgumentException("Unknown database type: " + dbType);
        }
        instance = (IDatabase) Class.forName(dbImplementationClass).newInstance();
      }
      catch (Exception e) {
        if (e instanceof IllegalArgumentException)
          throw (IllegalArgumentException) e;

        throw new RuntimeException(e);
      }
    }

    return instance;
  }
}