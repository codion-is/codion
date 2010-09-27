/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Provides Database implementations based on system settings.
 * @see Database#DATABASE_IMPLEMENTATION_CLASS
 * @see Database#DATABASE_TYPE
 */
public final class Databases {

  private static final Map<String, DatabaseConnection.Operation> OPERATIONS = Collections.synchronizedMap(new HashMap<String, DatabaseConnection.Operation>());

  /**
   * A synchronized query counter
   */
  public static final QueryCounter QUERY_COUNTER = new QueryCounter();

  /**
   * A result packer for fetching integers from an result set containing a single integer column
   */
  public static final ResultPacker<Integer> INT_PACKER = new ResultPacker<Integer>() {
    /** {@inheritDoc} */
    public List<Integer> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Integer> integers = new ArrayList<Integer>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        integers.add(resultSet.getInt(1));
      }

      return integers;
    }
  };
  /**
   * A result packer for fetching strings from an result set containing a single string column
   */
  public static final ResultPacker<String> STRING_PACKER = new ResultPacker<String>() {
    /** {@inheritDoc} */
    public List<String> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<String> strings = new ArrayList<String>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        strings.add(resultSet.getString(1));
      }

      return strings;
    }
  };

  private Databases() {}

  /**
   * @return a new Database instance based on runtime properties
   * @see Database#DATABASE_TYPE
   * @see Database#DATABASE_IMPLEMENTATION_CLASS
   * @throws RuntimeException if an unrecognized database type is specified
   */
  public static Database createInstance() {
    try {
      final String databaseClassName = System.getProperty(Database.DATABASE_IMPLEMENTATION_CLASS, getDatabaseClassName());
      return (Database) Class.forName(databaseClassName).newInstance();
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
   * @return a DatabaseStatistics object containing the most recent statistics from the underlying database
   */
  public static Database.Statistics getDatabaseStatistics() {
    return new DatabaseStatistics(QUERY_COUNTER.getQueriesPerSecond(),
            QUERY_COUNTER.getSelectsPerSecond(), QUERY_COUNTER.getInsertsPerSecond(),
            QUERY_COUNTER.getDeletesPerSecond(), QUERY_COUNTER.getUpdatesPerSecond());
  }

  /**
   * A class for counting query types, providing avarages over time
   */
  public static final class QueryCounter {

    private static final int DEFAULT_UPDATE_INTERVAL_MS = 2000;

    private long queriesPerSecondTime = System.currentTimeMillis();
    private int queriesPerSecond = 0;
    private int queriesPerSecondCounter = 0;
    private int selectsPerSecond = 0;
    private int selectsPerSecondCounter = 0;
    private int insertsPerSecond = 0;
    private int insertsPerSecondCounter = 0;
    private int updatesPerSecond = 0;
    private int updatesPerSecondCounter = 0;
    private int deletesPerSecond = 0;
    private int deletesPerSecondCounter = 0;
    private int undefinedPerSecond = 0;
    private int undefinedPerSecondCounter = 0;

    private QueryCounter() {
      new Timer(true).schedule(new TimerTask() {
        @Override
        public void run() {
          updateQueriesPerSecond();
        }
      }, new Date(), DEFAULT_UPDATE_INTERVAL_MS);
    }

    /**
     * Counts the given query, based on it's first character
     * @param sql the sql query
     */
    public synchronized void count(final String sql) {
      queriesPerSecondCounter++;
      switch (Character.toLowerCase(sql.charAt(0))) {
        case 's':
          selectsPerSecondCounter++;
          break;
        case 'i':
          insertsPerSecondCounter++;
          break;
        case 'u':
          updatesPerSecondCounter++;
          break;
        case 'd':
          deletesPerSecondCounter++;
          break;
        default:
          undefinedPerSecondCounter++;
      }
    }

    /**
     * @return the number of queries being run per second
     */
    public synchronized int getQueriesPerSecond() {
      return queriesPerSecond;
    }

    /**
     * @return the number of select queries being run per second
     */
    public synchronized int getSelectsPerSecond() {
      return selectsPerSecond;
    }

    /**
     * @return the number of delete queries being run per second
     */
    public synchronized int getDeletesPerSecond() {
      return deletesPerSecond;
    }

    /**
     * @return the number of insert queries being run per second
     */
    public synchronized int getInsertsPerSecond() {
      return insertsPerSecond;
    }

    /**
     * @return the number of update queries being run per second
     */
    public synchronized int getUpdatesPerSecond() {
      return updatesPerSecond;
    }

    /**
     * @return the number of undefined queries being run per second
     */
    public synchronized int getUndefinedPerSecond() {
      return undefinedPerSecond;
    }

    private synchronized void updateQueriesPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - queriesPerSecondTime) / 1000d;
      if (seconds > 5) {
        queriesPerSecond = (int) (queriesPerSecondCounter / (double) seconds);
        selectsPerSecond = (int) (selectsPerSecondCounter / (double) seconds);
        insertsPerSecond = (int) (insertsPerSecondCounter / (double) seconds);
        deletesPerSecond = (int) (deletesPerSecondCounter / (double) seconds);
        updatesPerSecond = (int) (updatesPerSecondCounter / (double) seconds);
        undefinedPerSecond = (int) (undefinedPerSecondCounter / (double) seconds);
        queriesPerSecondCounter = 0;
        selectsPerSecondCounter = 0;
        insertsPerSecondCounter = 0;
        deletesPerSecondCounter = 0;
        updatesPerSecondCounter = 0;
        undefinedPerSecondCounter = 0;
        queriesPerSecondTime = current;
      }
    }
  }

  /**
   * A default DatabaseStatistics implementation.
   */
  public static final class DatabaseStatistics implements Database.Statistics, Serializable {

    private static final long serialVersionUID = 1;

    private final long timestamp = System.currentTimeMillis();
    private final int queriesPerSecond;
    private final int selectsPerSecond;
    private final int insertsPerSecond;
    private final int deletesPerSecond;
    private final int updatesPerSecond;

    /**
     * Instantiates a new DbStatistics object
     * @param queriesPerSecond the number of queries being run per second
     * @param selectsPerSecond the number of select queries being run per second
     * @param insertsPerSecond the number of insert queries being run per second
     * @param deletesPerSecond the number of delete queries being run per second
     * @param updatesPerSecond the number of update queries being run per second
     */
    private DatabaseStatistics(final int queriesPerSecond, final int selectsPerSecond, final int insertsPerSecond,
                               final int deletesPerSecond, final int updatesPerSecond) {
      this.queriesPerSecond = queriesPerSecond;
      this.selectsPerSecond = selectsPerSecond;
      this.insertsPerSecond = insertsPerSecond;
      this.deletesPerSecond = deletesPerSecond;
      this.updatesPerSecond = updatesPerSecond;
    }

    /** {@inheritDoc} */
    public int getQueriesPerSecond() {
      return queriesPerSecond;
    }

    /** {@inheritDoc} */
    public int getDeletesPerSecond() {
      return deletesPerSecond;
    }

    /** {@inheritDoc} */
    public int getInsertsPerSecond() {
      return insertsPerSecond;
    }

    /** {@inheritDoc} */
    public int getSelectsPerSecond() {
      return selectsPerSecond;
    }

    /** {@inheritDoc} */
    public int getUpdatesPerSecond() {
      return updatesPerSecond;
    }

    /** {@inheritDoc} */
    public long getTimestamp() {
      return timestamp;
    }
  }
}