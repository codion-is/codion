/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides Database implementations based on system settings.
 * @see Database#DATABASE_IMPLEMENTATION_CLASS
 * @see Database#DATABASE_TYPE
 */
public final class Databases {

  private static final Map<String, DatabaseConnection.Operation> OPERATIONS = Collections.synchronizedMap(new HashMap<>());

  private static Database INSTANCE;

  private Databases() {}

  /**
   * @deprecated use {@link #getInstance()}
   */
  public static synchronized Database createInstance() {
    return getInstance();
  }

  /**
   * @return a Database instance based on the current runtime database type property
   * @see Database#DATABASE_TYPE
   * @see Database#DATABASE_IMPLEMENTATION_CLASS
   * @see #getDatabaseType()
   * @throws IllegalArgumentException in case an unsupported database type is specified
   * @throws RuntimeException in case of an exception occurring while instantiating the database implementation instance
   */
  public static synchronized Database getInstance() {
    try {
      final Database.Type currentType = getDatabaseType();
      if (INSTANCE == null || !INSTANCE.getDatabaseType().equals(currentType)) {
        //refresh the instance
        final String databaseClassName = System.getProperty(Database.DATABASE_IMPLEMENTATION_CLASS, getDatabaseClassName());
        INSTANCE = (Database) Class.forName(databaseClassName).newInstance();
      }

      return INSTANCE;
    }
    catch (final IllegalArgumentException e) {
      throw e;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the database type string as specified by the DATABASE_TYPE system property
   * @see Database#DATABASE_TYPE
   */
  public static Database.Type getDatabaseType() {
    final String dbType = System.getProperty(Database.DATABASE_TYPE);
    if (dbType == null) {
      throw new IllegalArgumentException("Required system property missing: " + Database.DATABASE_TYPE);
    }

    return Database.Type.valueOf(dbType.trim().toUpperCase());
  }

  private static String getDatabaseClassName() {
    final Database.Type dbType = getDatabaseType();
    switch (dbType) {
      case POSTGRESQL:
        return "org.jminor.common.db.dbms.PostgreSQLDatabase";
      case MYSQL:
        return "org.jminor.common.db.dbms.MySQLDatabase";
      case ORACLE:
        return "org.jminor.common.db.dbms.OracleDatabase";
      case SQLSERVER:
        return "org.jminor.common.db.dbms.SQLServerDatabase";
      case DERBY:
        return "org.jminor.common.db.dbms.DerbyDatabase";
      case H2:
        return "org.jminor.common.db.dbms.H2Database";
      case HSQL:
        return "org.jminor.common.db.dbms.HSQLDatabase";
      case OTHER:
        throw new IllegalArgumentException("Database type OTHER does not have an implementation");
      default:
        throw new IllegalArgumentException("Unknown database type: " + dbType);
    }
  }

  /**
   * Adds the given Operation to this repository
   * @param operation the operation to add
   * @throws IllegalArgumentException in case an operation with the same ID has already been added
   */
  public static void addOperation(final DatabaseConnection.Operation operation) {
    if (OPERATIONS.containsKey(operation.getID())) {
      throw new IllegalArgumentException("Operation already defined: " + OPERATIONS.get(operation.getID()).getName());
    }

    OPERATIONS.put(operation.getID(), operation);
  }

  /**
   * @param <C> the type of the database connection this procedure requires
   * @param procedureID the procedure ID
   * @return the procedure
   * @throws IllegalArgumentException in case the procedure is not found
   */
  public static <C> DatabaseConnection.Procedure<C> getProcedure(final String procedureID) {
    final DatabaseConnection.Operation operation = OPERATIONS.get(procedureID);
    if (operation == null) {
      throw new IllegalArgumentException("Procedure not found: " + procedureID);
    }

    return (DatabaseConnection.Procedure) operation;
  }

  /**
   * @param <C> the type of the database connection this function requires
   * @param functionID the function ID
   * @return the function
   * @throws IllegalArgumentException in case the function is not found
   */
  public static <C> DatabaseConnection.Function<C> getFunction(final String functionID) {
    final DatabaseConnection.Operation operation = OPERATIONS.get(functionID);
    if (operation == null) {
      throw new IllegalArgumentException("Function not found: " + functionID);
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

    @Override
    public int getQueriesPerSecond() {
      return queriesPerSecond;
    }

    @Override
    public int getDeletesPerSecond() {
      return deletesPerSecond;
    }

    @Override
    public int getInsertsPerSecond() {
      return insertsPerSecond;
    }

    @Override
    public int getSelectsPerSecond() {
      return selectsPerSecond;
    }

    @Override
    public int getUpdatesPerSecond() {
      return updatesPerSecond;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }
  }

  /**
   * Annotation for a database operation id, specifying the operation class name
   */
  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Operation {

    /**
     * @return the operation class name
     */
    String className();
  }
}