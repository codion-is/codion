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
          final String dbType = System.getProperty(IDatabase.DATABASE_TYPE_PROPERTY);
          if (dbType == null)
            throw new IllegalArgumentException("Required system property missing: " + IDatabase.DATABASE_TYPE_PROPERTY);

          if (dbType.equals(IDatabase.DATABASE_TYPE_POSTGRESQL))
            dbImplementationClass = "org.jminor.common.db.dbms.PostgreSQLDatabase";
          else if (dbType.equals(IDatabase.DATABASE_TYPE_MYSQL))
            dbImplementationClass = "org.jminor.common.db.dbms.MySQLDatabase";
          else if (dbType.equals(IDatabase.DATABASE_TYPE_ORACLE))
            dbImplementationClass = "org.jminor.common.db.dbms.OracleDatabase";
          else if (dbType.equals(IDatabase.DATABASE_TYPE_SQLSERVER))
            dbImplementationClass = "org.jminor.common.db.dbms.SQLServerDatabase";
          else if (dbType.equals(IDatabase.DATABASE_TYPE_DERBY))
            dbImplementationClass = "org.jminor.common.db.dbms.DerbyDatabase";
          else if (dbType.equals(IDatabase.DATABASE_TYPE_EMBEDDED_DERBY))
            dbImplementationClass = "org.jminor.common.db.dbms.DerbyEmbeddedDatabase";
          else if (dbType.equals(IDatabase.DATABASE_TYPE_H2))
            dbImplementationClass = "org.jminor.common.db.dbms.H2Database";
          else if (dbType.equals(IDatabase.DATABASE_TYPE_EMBEDDED_H2))
            dbImplementationClass = "org.jminor.common.db.dbms.H2EmbeddedDatabase";
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