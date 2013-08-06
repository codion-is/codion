/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides Database implementations based on system settings.
 * @see Database#DATABASE_IMPLEMENTATION_CLASS
 * @see Database#DATABASE_TYPE
 */
public final class Databases {

  private static final Map<String, DatabaseConnection.Operation> OPERATIONS = Collections.synchronizedMap(new HashMap<String, DatabaseConnection.Operation>());

  private Databases() {}

  /**
   * @return a new Database instance based on runtime properties
   * @see Database#DATABASE_TYPE
   * @see Database#DATABASE_IMPLEMENTATION_CLASS
   * @throws IllegalArgumentException in case an unsupported database type is specified
   * @throws RuntimeException in case of an exception occurring while instantiating the database implementation instance
   */
  public static Database createInstance() {
    try {
      final String databaseClassName = System.getProperty(Database.DATABASE_IMPLEMENTATION_CLASS, getDatabaseClassName());
      return (Database) Class.forName(databaseClassName).newInstance();
    }
    catch (IllegalArgumentException e) {
      throw e;
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
      throw new IllegalArgumentException("Required system property missing: " + Database.DATABASE_TYPE);
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
      throw new IllegalArgumentException("Unknown database type: " + dbType);
    }
  }

  /**
   * Adds the given Operation to this repository
   * @param operation the operation to add
   * @throws RuntimeException in case an operation with the same ID has already been added
   */
  public static void addOperation(final DatabaseConnection.Operation operation) {
    if (OPERATIONS.containsKey(operation.getID())) {
      throw new RuntimeException("Operation already defined: " + OPERATIONS.get(operation.getID()).getName());
    }

    OPERATIONS.put(operation.getID(), operation);
  }

  /**
   * @param procedureID the procedure ID
   * @return the procedure
   * @throws RuntimeException in case the procedure is not found
   */
  public static DatabaseConnection.Procedure getProcedure(final String procedureID) {
    final DatabaseConnection.Operation operation = OPERATIONS.get(procedureID);
    if (operation == null) {
      throw new RuntimeException("Procedure not found: " + procedureID);
    }

    return (DatabaseConnection.Procedure) operation;
  }

  /**
   * @param functionID the function ID
   * @return the function
   * @throws RuntimeException in case the function is not found
   */
  public static DatabaseConnection.Function getFunction(final String functionID) {
    final DatabaseConnection.Operation operation = OPERATIONS.get(functionID);
    if (operation == null) {
      throw new RuntimeException("Function not found: " + functionID);
    }

    return (DatabaseConnection.Function) operation;
  }

  /**
   * A default DatabaseStatistics implementation.
   */
  static final class DatabaseStatistics implements Database.Statistics, Serializable {

    private static final long serialVersionUID = 1;

    private final long timestamp = System.currentTimeMillis();
    private final int queriesPerSecond;
    private final int selectsPerSecond;
    private final int insertsPerSecond;
    private final int deletesPerSecond;
    private final int updatesPerSecond;

    /**
     * Instantiates a new DatabaseStatistics object
     * @param queriesPerSecond the number of queries being run per second
     * @param selectsPerSecond the number of select queries being run per second
     * @param insertsPerSecond the number of insert queries being run per second
     * @param deletesPerSecond the number of delete queries being run per second
     * @param updatesPerSecond the number of update queries being run per second
     */
    DatabaseStatistics(final int queriesPerSecond, final int selectsPerSecond, final int insertsPerSecond,
                       final int deletesPerSecond, final int updatesPerSecond) {
      this.queriesPerSecond = queriesPerSecond;
      this.selectsPerSecond = selectsPerSecond;
      this.insertsPerSecond = insertsPerSecond;
      this.deletesPerSecond = deletesPerSecond;
      this.updatesPerSecond = updatesPerSecond;
    }

    /** {@inheritDoc} */
    @Override
    public int getQueriesPerSecond() {
      return queriesPerSecond;
    }

    /** {@inheritDoc} */
    @Override
    public int getDeletesPerSecond() {
      return deletesPerSecond;
    }

    /** {@inheritDoc} */
    @Override
    public int getInsertsPerSecond() {
      return insertsPerSecond;
    }

    /** {@inheritDoc} */
    @Override
    public int getSelectsPerSecond() {
      return selectsPerSecond;
    }

    /** {@inheritDoc} */
    @Override
    public int getUpdatesPerSecond() {
      return updatesPerSecond;
    }

    /** {@inheritDoc} */
    @Override
    public long getTimestamp() {
      return timestamp;
    }
  }
}