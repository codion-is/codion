/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

/**
 * Provides Database implementations based on system settings.
 * @see Database#DATABASE_IMPLEMENTATION_CLASS
 * @see Database#DATABASE_TYPE
 */
public final class DatabaseProvider {

  private DatabaseProvider() {}

  public static Database createInstance() {
    try {
      final String databaseClassName = System.getProperty(Database.DATABASE_IMPLEMENTATION_CLASS, getDatabaseClassName());
      return (Database) Class.forName(databaseClassName).newInstance();
    }
    catch (RuntimeException iae) {
      throw iae;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the database type string as specified by the DATABASE_TYPE system property
   * @see Database#DATABASE_TYPE
   */
  public static String getDatabaseType() {
    return System.getProperty(Database.DATABASE_TYPE);
  }

  private static String getDatabaseClassName() {
    final String dbType = getDatabaseType();
    if (dbType == null) {
      throw new RuntimeException("Required system property missing: " + Database.DATABASE_TYPE);
    }

    if (dbType.equals(Database.POSTGRESQL)) {
      return "org.jminor.common.db.dbms.PostgreSQLDatabase";
    }
    else if (dbType.equals(Database.MYSQL)) {
      return "org.jminor.common.db.dbms.MySQLDatabase";
    }
    else if (dbType.equals(Database.ORACLE)) {
      return "org.jminor.common.db.dbms.OracleDatabase";
    }
    else if (dbType.equals(Database.SQLSERVER)) {
      return "org.jminor.common.db.dbms.SQLServerDatabase";
    }
    else if (dbType.equals(Database.DERBY)) {
      return "org.jminor.common.db.dbms.DerbyDatabase";
    }
    else if (dbType.equals(Database.H2)) {
      return "org.jminor.common.db.dbms.H2Database";
    }
    else if (dbType.equals(Database.HSQL)) {
      return "org.jminor.common.db.dbms.HSQLDatabase";
    }
    else {
      throw new RuntimeException("Unknown database type: " + dbType);
    }
  }
}