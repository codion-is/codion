/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.dbms.Dbms;

public class Database {

  private static Dbms instance;

  public static Dbms get() {
    if (instance == null) {
      try {
        String dbImplementationClass = System.getProperty(Dbms.DATABASE_IMPLEMENTATION_CLASS);
        if (dbImplementationClass == null) {
          final String dbType = System.getProperty(Dbms.DATABASE_TYPE);
          if (dbType == null)
            throw new IllegalArgumentException("Required system property missing: " + Dbms.DATABASE_TYPE);

          if (dbType.equals(Dbms.POSTGRESQL))
            dbImplementationClass = "org.jminor.common.db.dbms.PostgreSQLDatabase";
          else if (dbType.equals(Dbms.MYSQL))
            dbImplementationClass = "org.jminor.common.db.dbms.MySQLDatabase";
          else if (dbType.equals(Dbms.ORACLE))
            dbImplementationClass = "org.jminor.common.db.dbms.OracleDatabase";
          else if (dbType.equals(Dbms.SQLSERVER))
            dbImplementationClass = "org.jminor.common.db.dbms.SQLServerDatabase";
          else if (dbType.equals(Dbms.DERBY))
            dbImplementationClass = "org.jminor.common.db.dbms.DerbyDatabase";
          else if (dbType.equals(Dbms.H2))
            dbImplementationClass = "org.jminor.common.db.dbms.H2Database";
          else if (dbType.equals(Dbms.HSQL))
            dbImplementationClass = "org.jminor.common.db.dbms.HSQLDatabase";
          else
            throw new IllegalArgumentException("Unknown database type: " + dbType);
        }
        instance = (Dbms) Class.forName(dbImplementationClass).newInstance();
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