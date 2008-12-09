/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.Util;

import org.apache.log4j.Logger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A JDBC layer database connection class
 */
public class DbConnection {

  private static final Logger log = Util.getLogger(DbConnection.class);

  public static final String OUT_PARAM_NAME = "procout";

  private final Properties connectionInfo = new Properties();
  private final Map<String, List> queryCache = Collections.synchronizedMap(new HashMap<String, List>());

  private Connection connection;
  private Statement checkConnectionStatement;
  private boolean transactionOpen = false;
  private boolean connected = false;
  private boolean allowCaching = true;

  private int cacheQueriesRequests = 0;
  private boolean lastResultCached = false;

  //for logging purposes
  private final User connectionUser;

  private static long requestsPerSecondTime = System.currentTimeMillis();
  private static int queriesPerSecond = 0;
  private static int requestsPerSecondCounter = 0;
  private static int cachedQueriesPerSecond = 0;
  private static int cachedPerSecondCounter = 0;

  static {
    new Timer(true).schedule(new TimerTask() {
      public void run() {
        updateRequestsPerSecond();
      }
    }, new Date(), 2000);
  }

  /**
   * Constructs a new instance of the DbConnection class, initialized and ready for usage
   * @param user the user for the db-connection
   * @throws AuthenticationException in case the user does not have access to the database
   * @throws ClassNotFoundException in case the database driver was not found
   */
  public DbConnection(final User user) throws ClassNotFoundException, AuthenticationException {
    this.connectionUser = user;
    if (user.getUsername() == null)
      throw new IllegalArgumentException("Username must be provided");
    if (user.getPassword() == null)
      throw new IllegalArgumentException("Password must be provided");
    this.connectionInfo.put("user", user.getUsername());
    this.connectionInfo.put("password", user.getPassword());
    revalidate();
  }

  public User getConnectionUser() {
    return connectionUser;
  }

  /**
   * @return Value for property 'connectionValid'.
   */
  public boolean isConnectionValid() {
    try {
      return Database.isOracle() || Database.isPostgreSQL() ? checkConnection() : connection.isValid(0);
    }
    catch (SQLException e) {
      log.error(this, e);
      return false;
    }
  }

  public synchronized void disconnect() {
    if (!isConnected())
      return;

    connected = false;
    try {
      if (checkConnectionStatement != null && !checkConnectionStatement.isClosed())
        checkConnectionStatement.close();
    }
    catch (Throwable e) {/**/}
    try {
      if (connection != null && !connection.isClosed()) {
        connection.rollback();
        connection.close();
      }
    }
    catch (SQLException ex) {
      log.error(this, ex);
    }
    Database.onDisconnect();
    connection = null;
    checkConnectionStatement = null;
  }

  /**
   * @return Value for property 'connected'.
   */
  public boolean isConnected() {
    return connected;
  }

  public void startTransaction() throws IllegalStateException {
    if (isTransactionOpen())
      throw new IllegalStateException("Transaction already open");

    transactionOpen = true;
  }

  public void endTransaction(final boolean rollback) throws SQLException {
    try {
      if (rollback)
        rollback();
      else
        commit();
    }
    finally {
      if (transactionOpen)
        transactionOpen = false;
    }
  }

  /**
   * @return Value for property 'transactionOpen'.
   */
  public final boolean isTransactionOpen() {
    return transactionOpen;
  }

  /**
   * @return true if the last query result was retrieved from the cache
   */
  public boolean lastQueryResultCached() {
    return lastResultCached;
  }

  /**
   * @param val Value to set for property 'allowCaching'.
   */
  public void setAllowCaching(final boolean val) {
    allowCaching = val;
    if (!val) {
      queryCache.clear();
      cacheQueriesRequests = 0;
    }
  }

  /**
   * @return Value for property 'allowCaching'.
   */
  public boolean getAllowCaching() {
    return allowCaching;
  }

  public void addCacheQueriesRequest() {
    if (allowCaching)
      this.cacheQueriesRequests++;
  }

  public void removeCacheQueriesRequest() {
    if (this.cacheQueriesRequests > 0)
      this.cacheQueriesRequests--;
    if (cacheQueriesRequests == 0)
      queryCache.clear();
  }

  /**
   * @return Value for property 'cacheQueriesRequests'.
   */
  public int getCacheQueriesRequests() {
    return cacheQueriesRequests;
  }

  /**
   * Performs the given sql query and returns the result in a List
   * @param sql the query
   * @param resultPacker a IResultPacker instance for creating the return List
   * @param recordCount the number of records to retrieve, use -1 to retrieve all
   * @return the query result in a List
   * @throws SQLException thrown if anything goes wrong during the query execution
   */
  public final synchronized List query(final String sql, final IResultPacker resultPacker, final int recordCount) throws SQLException {
    requestsPerSecondCounter++;
    if (cacheQueriesRequests > 0 && recordCount < 0) {
      if (queryCache.containsKey(sql)) {
        log.debug(connectionUser.getUsername() + " (cached): " + sql.toUpperCase()+";");
        lastResultCached = true;
        cachedPerSecondCounter++;
        return queryCache.get(sql);
      }
    }
    final long time = System.currentTimeMillis();
    log.debug(sql);
    lastResultCached = false;
    Statement statement = null;
    try {
      statement = connection.createStatement();
      final List ret = resultPacker.pack(statement.executeQuery(sql), recordCount);
      if (cacheQueriesRequests > 0 && recordCount < 0)
        queryCache.put(sql, ret);

      return ret;
    }
    catch (SQLException e) {
      log.debug(connectionUser.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): " + sql+";", e);
      throw e;
    }
    finally {
      try {
        if (statement != null)
          statement.close();//also closes the result set
      }
      catch (SQLException e) {/**/}
    }
  }

  /**
   * Performs the given query and returns the result as a List of Strings
   * @param sql the query, it must select a single string column
   * @return a List of Strings representing the query result
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  public final List<String> queryStrings(final String sql) throws SQLException {
    final List res = query(sql, DbUtil.STRING_PACKER, -1);
    final List<String> ret = new ArrayList<String>(res.size());
    for (final Object object : res)
      ret.add((String) object);

    return ret;
  }

  /**
   * Performs the given query and returns the result as an integer
   * @param sql the query must select a single number column
   * @return the first record in the result as a integer
   * @throws SQLException thrown if anything goes wrong during the execution
   * @throws DbException thrown if no record is found
   */
  public final int queryInteger(final String sql) throws SQLException, DbException {
    final List<Integer> ret = queryIntegers(sql);
    if (ret.size() > 0)
      return ret.get(0);

    throw new DbException("No records returned when querying for an integer", sql);
  }

  /**
   * Performs the given query and returns the result as a List of Integers
   * @param sql the query, it must select a single number column
   * @return a List of Integers representing the query result
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  @SuppressWarnings({"unchecked"})
  public final List<Integer> queryIntegers(final String sql) throws SQLException {
    return (List<Integer>) query(sql, DbUtil.INT_PACKER, -1);
  }

  /**
   *
   * @param sql the query
   * @param recordCount the maximum number of records to return, -1 for all
   * @return the result of this query, in a List of rows represented as Lists
   * @throws SQLException thrown if anything goes wrong during the query execution
   */
  @SuppressWarnings({"unchecked"})
  public final List<List> queryObjects(final String sql, final int recordCount) throws SQLException {
    return (List<List>) query(sql, new MixedResultPacker(), recordCount);
  }

  /**
   * Performs a commit
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  public final void commit() throws SQLException {
    log.debug(connectionUser.getUsername() + ": " + "commit;");
    connection.commit();
  }

  /**
   * Performs a rollback
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  public final void rollback() throws SQLException {
    log.debug(connectionUser.getUsername() + ": " + "rollback;");
    connection.rollback();
  }

  public synchronized Object executeCallableStatement(final String sqlStatement,
                                                      final int outParamType) throws SQLException {
    requestsPerSecondCounter++;
    final long time = System.currentTimeMillis();
    log.debug(sqlStatement);
    CallableStatement statement = null;
    try {
      final boolean hasOutParameter = sqlStatement.indexOf(OUT_PARAM_NAME) > 0;
      statement = connection.prepareCall(sqlStatement);
      if (hasOutParameter)
        statement.registerOutParameter(1, outParamType);

      statement.execute();

      return hasOutParameter ? statement.getObject(1) : null;
    }
    catch (SQLException e) {
      System.out.println(sqlStatement);
      log.debug(connectionUser.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): " + sqlStatement+";", e);
      throw e;
    }
    finally {
      try {
        if (statement != null)
          statement.close();
      }
      catch (SQLException e) {/**/}
    }
  }

  /**
   * Executes the given DML-query
   * @param sql the query to execute
   * @throws SQLException thrown if anything goes wrong during execution
   */
  public synchronized final void execute(final String sql) throws SQLException {
    requestsPerSecondCounter++;
    final long time = System.currentTimeMillis();
    log.debug(sql);
    Statement statement = null;
    try {
      (statement = connection.createStatement()).execute(sql);
    }
    catch (SQLException e) {
      System.out.println(sql);
      log.error(connectionUser.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): " + sql+";", e);
      throw e;
    }
    finally {
      try {
        if (statement != null)
          statement.close();
      }
      catch (SQLException e) {/**/}
    }
  }

  public static int getQueriesPerSecond() {
    return queriesPerSecond;
  }

  public static int getCachedQueriesPerSecond() {
    return cachedQueriesPerSecond;
  }

  /**
   * @return Value for property 'connection'.
   */
  protected Connection getConnection() {
    return connection;
  }

  private void revalidate() throws AuthenticationException, ClassNotFoundException {
    try {
      if (connection != null) {
        log.info("Revalidating connection: " + connectionUser.getUsername());
        connection.rollback();
        connection.close();
      }
      if (checkConnectionStatement != null && !checkConnectionStatement.isClosed())
        checkConnectionStatement.close();
    }
    catch (SQLException e) {/**/}

    Database.loadDriver();
    try {
      connection = DriverManager.getConnection(Database.getURL(), connectionInfo);
      connection.setAutoCommit(false);
      checkConnectionStatement = connection.createStatement();
      connected = true;
    }
    catch (SQLException e) {
      throw new AuthenticationException(connectionInfo.getProperty("user"), e);
    }
  }

  private boolean checkConnection() throws SQLException {
    if (connection != null) {
      try {
        checkConnectionStatement.executeQuery("select 1 from dual");
        return true;
      }
      catch (SQLException sqle) {
        sqle.printStackTrace();
        throw sqle;
      }
    }

    return false;
  }

  private static void updateRequestsPerSecond() {
    final long current = System.currentTimeMillis();
    final double seconds = (current - requestsPerSecondTime)/1000;
    if (seconds > 5) {
      queriesPerSecond = (int) ((double) requestsPerSecondCounter/seconds);
      cachedQueriesPerSecond = (int) ((double) cachedPerSecondCounter/seconds);
      cachedPerSecondCounter = 0;
      requestsPerSecondCounter = 0;
      requestsPerSecondTime = current;
    }
  }

  private static class MixedResultPacker implements IResultPacker<List> {
    public List<List> pack(final ResultSet resultSet, final int recordCount) throws SQLException {
      final List<List> ret = new ArrayList<List>();
      final int columnCount = resultSet.getMetaData().getColumnCount();
      int counter = 0;
      while (resultSet.next() && (recordCount < 0 || counter++ < recordCount)) {
        final List<Object> row = new ArrayList<Object>(columnCount);
        for (int index = 1; index <= columnCount; index++)
          row.add(resultSet.getObject(index));
        ret.add(row);
      }

      return ret;
    }
  }
}