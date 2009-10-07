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
        final String dbmsClassName = System.getProperty(Dbms.DATABASE_IMPLEMENTATION_CLASS, getDbmsClassName());
        instance = (Dbms) Class.forName(dbmsClassName).newInstance();
      }
      catch (RuntimeException iae) {
        throw iae;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return instance;
  }

  private static String getDbmsClassName() {
    final String dbType = System.getProperty(Dbms.DATABASE_TYPE);
    if (dbType == null)
      throw new RuntimeException("Required system property missing: " + Dbms.DATABASE_TYPE);

    if (dbType.equals(Dbms.POSTGRESQL))
      return "org.jminor.common.db.dbms.PostgreSQLDatabase";
    else if (dbType.equals(Dbms.MYSQL))
      return "org.jminor.common.db.dbms.MySQLDatabase";
    else if (dbType.equals(Dbms.ORACLE))
      return "org.jminor.common.db.dbms.OracleDatabase";
    else if (dbType.equals(Dbms.SQLSERVER))
      return "org.jminor.common.db.dbms.SQLServerDatabase";
    else if (dbType.equals(Dbms.DERBY))
      return "org.jminor.common.db.dbms.DerbyDatabase";
    else if (dbType.equals(Dbms.H2))
      return "org.jminor.common.db.dbms.H2Database";
    else if (dbType.equals(Dbms.HSQL))
      return "org.jminor.common.db.dbms.HSQLDatabase";
    else
      throw new RuntimeException("Unknown database type: " + dbType);
  }
}