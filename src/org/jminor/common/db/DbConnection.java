/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.common.db;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;

import org.apache.log4j.Logger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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
public abstract class DbConnection {

  private static final Logger log = Util.getLogger(DbConnection.class);

  public static final String OUT_PARAM_NAME = "procout";

  public final Event evtConnected = new Event("DbConnection.evtConnected");

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
   * @throws UserAccessException in case the user does not have access to the database
   * @throws ClassNotFoundException in case the database driver was not found
   */
  public DbConnection(final User user) throws ClassNotFoundException, UserAccessException {
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
      return DbUtil.isMySQL() ? connection.isValid(0) : checkConnection();
    }
    catch (Exception e) {
      log.error(this, e);
      return false;
    }
  }

  public void connect() throws ClassNotFoundException, UserAccessException {
    if (isConnected())
      return;

    revalidate();
    evtConnected.fire();
  }

  public void revalidate() throws UserAccessException, ClassNotFoundException {
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

    Class.forName(DbUtil.getDatabaseDriver());
    try {
      connection = DriverManager.getConnection(getDatabaseURL(), connectionInfo);
      connection.setAutoCommit(false);
      checkConnectionStatement = connection.createStatement();
      connected = true;
    }
    catch (SQLException e) {
      throw new UserAccessException(connectionInfo.getProperty("user"), e);
    }
    catch (Exception e) {
      throw new UserAccessException(connectionInfo.getProperty("user"));
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

    setTransactionOpen(true);
  }

  public void endTransaction(final boolean rollback) throws SQLException {
    if (!isTransactionOpen())
      throw new IllegalStateException("Transaction already closed");

    try {
      if (rollback)
        connection.rollback();
      else
        connection.commit();
    }
    finally {
      setTransactionOpen(false);
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
   * Performs the given sql query and returns the result as an Object array
   * @param sql the query
   * @param resultPacker a IResultPacker instance for creating the return object
   * @return the query result in an Object array
   * @throws SQLException thrown if anything goes wrong during the query execution
   */
  public final synchronized List query(final String sql, final IResultPacker resultPacker) throws SQLException {
    requestsPerSecondCounter++;
    if (cacheQueriesRequests > 0) {
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
      final List ret = resultPacker.pack(statement.executeQuery(sql));
      if (cacheQueriesRequests > 0)
        queryCache.put(sql, ret);

      return ret;
    }
    catch (SQLException e) {
      System.out.println(sql);
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
   * Performes the given query and returns the result as a String array
   * @param sql the query, it must select a single string column
   * @return a String array representing the result of the query
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  public final List<String> queryStrings(final String sql) throws SQLException {
    final List res = query(sql, DbUtil.STRING_PACKER);
    final List<String> ret = new ArrayList<String>(res.size());
    for (final Object object : res)
      ret.add((String) object);

    return ret;
  }

  /**
   * Performes the given query and returns the result as an integer
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
   * Performes the given query and returns the result as an integer array
   * @param sql the query, it must select a single number column
   * @return an integer array representing the result of the query
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  @SuppressWarnings({"unchecked"})
  public final List<Integer> queryIntegers(final String sql) throws SQLException {
    return (List<Integer>) query(sql, DbUtil.INT_PACKER);
  }

  /**
   * Performs a commit on the connection
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  public final void commit() throws SQLException {
    log.debug(connectionUser.getUsername() + ": " + "commit;");
    connection.commit();
  }

  /**
   * Performs a rollback on the connection
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  public final void rollback() throws SQLException {
    log.debug(connectionUser.getUsername() + ": " + "rollback;");
    connection.rollback();
  }

  /**
   * @param transactionOpen Value to set for property 'transactionOpen'.
   */
  public final void setTransactionOpen(final boolean transactionOpen) {
    this.transactionOpen = transactionOpen;
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

  public int getAutoIncrementValue(final String sql) throws DbException {
    try {
      return queryInteger(sql);
    }
    catch (SQLException e) {
      throw new DbException(e, sql);
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

  protected abstract String getDatabaseURL();

//  /**
//   * @param schema the schema of the table for which to retrieve dependencies
//   * @param table the table for which to retrieve dependencies
//   * @return a TableDependencies object containing information about
//   * the tables that have foreign keys to <code>schema</code>.<code>table</code>
//   * @throws SQLException in case of a database error
//   */
  //todo oracle specific
//  private TableDependencies getDependencies(final String schema, final String table) throws SQLException {
//    final String schemaUpper = schema.toUpperCase();
//    final String tableUpper = table.toUpperCase();
//    final DbUtil.PrimaryKey primaryKey = getPrimaryKey(schemaUpper, tableUpper);
//    final TableDependencies ret =
//            new TableDependencies(schemaUpper+"."+tableUpper, primaryKey.getColumns());
//
//    final List<String> foreignKeyNames = queryStrings(
//            "select constraint_name from all_constraints " +
//                    "where r_constraint_name = '" + primaryKey.getName() + "' " +
//                    "and constraint_type = 'R'");
//
//    if (foreignKeyNames.size() == 0)
//      return ret;
//
//    final String sql = "select ac.owner || '.' || ac.table_name, acc.column_name "
//            + "from all_constraints ac, all_cons_columns acc where "
//            + DbUtil.generateInList("ac.constraint_name", foreignKeyNames, Type.STRING)
//            + " and ac.constraint_name = acc.constraint_name";
//
//    return packDependencies(sql, ret);
//  }

//  private TableDependencies packDependencies(final String sql, final TableDependencies ret) throws SQLException {
//    query(sql, new DbUtil.TableDependenciesPacker(ret));
//
//    return ret;
//  }

  //todo oracle specific
//  private DbUtil.PrimaryKey getPrimaryKey(final String schema, final String table) throws SQLException {
//    final String sql = "select ac.constraint_name, acc.column_name " +
//            "from all_constraints ac, all_cons_columns acc " +
//            "where ac.owner = '" + schema + "' " +
//            "and ac.table_name = '" + table + "' " +
//            "and ac.constraint_type = 'P' " +
//            "and ac.constraint_name = acc.constraint_name " +
//            "order by position";
//
//    final List qRet = query(sql, DbUtil.PRIMARY_KEY_PACKER);
//
//    if (qRet.size() > 0)
//      return (DbUtil.PrimaryKey) qRet.get(0);
//
//    return null;
//  }

  private boolean checkConnection() throws Exception {
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
}