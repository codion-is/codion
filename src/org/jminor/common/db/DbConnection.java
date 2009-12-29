/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.Util;

import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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

  public static final String OUT_PARAMETER_NAME = "procout";

  private final Properties connectionProperties = new Properties();
  private final Map<String, List> queryCache = new HashMap<String, List>();
  private final User user;
  private final Database database;

  private Connection connection;
  private Statement checkConnectionStatement;
  private boolean transactionOpen = false;
  private boolean connected = false;
  private boolean allowCaching = true;

  private int cacheQueriesRequests = 0;
  private boolean lastResultCached = false;

  private static long requestsPerSecondTime = System.currentTimeMillis();
  private static int queriesPerSecond = 0;
  private static int requestsPerSecondCounter = 0;
  private static int cachedQueriesPerSecond = 0;
  private static int cachedPerSecondCounter = 0;

  static {
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        updateRequestsPerSecond();
      }
    }, new Date(), 2000);
  }

  /**
   * Constructs a new instance of the DbConnection class, initialized and ready for usage
   * @param database the database
   * @param user the user for the db-connection
   * @throws SQLException in case there is a problem connecting to the database
   * @throws ClassNotFoundException in case the database driver was not found
   */
  public DbConnection(final Database database, final User user) throws ClassNotFoundException, SQLException {
    if (user == null)
      throw new IllegalArgumentException("DbConnection requires a non-null user instance");
    if (user.getUsername() == null)
      throw new IllegalArgumentException("Username must be provided");
    if (user.getPassword() == null)
      throw new IllegalArgumentException("Password must be provided");
    this.database = database;
    this.user = user;
    this.connectionProperties.put("user", user.getUsername());
    this.connectionProperties.put("password", user.getPassword());
    revalidate();
  }

  @Override
  public String toString() {
    return "DbConnection: " + getUser().getUsername();
  }

  /**
   * @return the connection user
   */
  public User getUser() {
    return user;
  }

  /**
   * @return true if the connection is valid
   */
  public boolean isConnectionValid() {
    try {
      return database.supportsIsValid() ? connection.isValid(0) : checkConnection();
    }
    catch (SQLException e) {
      log.error(this, e);
      return false;
    }
  }

  /**
   * Disconnects this DbConnection
   */
  public void disconnect() {
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
   * @return true if the connection is connected
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * Begins a transaction on this connection
   * @throws IllegalStateException in case a transaction is already open
   */
  public void beginTransaction() throws IllegalStateException {
    if (transactionOpen)
      throw new IllegalStateException("Transaction already open");

    transactionOpen = true;
  }

  /**
   * Performs a rollback and ends the current transaction
   * @throws SQLException in case anything goes wrong during the rollback action
   * @throws IllegalStateException in case transaction is not open
   */
  public void rollbackTransaction() throws SQLException, IllegalStateException {
    try {
      if (!transactionOpen)
        throw new IllegalStateException("Transaction is not open");

      rollback();
    }
    finally {
      transactionOpen = false;
    }
  }

  /**
   * Performs a commit and ends the current transaction
   * @throws SQLException in case anything goes wrong during the commit action
   * @throws IllegalStateException in case transaction is not open
   */
  public void commitTransaction() throws SQLException, IllegalStateException {
    try {
      if (!transactionOpen)
        throw new IllegalStateException("Transaction is not open");

      commit();
    }
    finally {
      transactionOpen = false;
    }
  }

  /**
   * @return true if a transaction is open
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
   * Turns the query cache mechanism on or off.
   * If the query cache is being turned off all query cache requests are removed and the cache is cleared.
   * @param value true if query caching should be allowed
   * @see #addCacheQueriesRequest()
   * @see #removeCacheQueriesRequest()
   */
  public void setAllowCaching(final boolean value) {
    allowCaching = value;
    if (!value) {
      queryCache.clear();
      cacheQueriesRequests = 0;
    }
  }

  /**
   * @return true if this connection allows query caching
   */
  public boolean getAllowCaching() {
    return allowCaching;
  }

  /**
   * Activates a simple query cache, based on the query string.
   * A call to this method adds a request for turning on the caching mechanism,
   * must be balanced with a call to <code>removeCacheQueriesRequest</code>
   * @see #removeCacheQueriesRequest()
   */
  public void addCacheQueriesRequest() {
    if (allowCaching)
      this.cacheQueriesRequests++;
  }

  /**
   * Removes a single query cache request, the cache is deactivated and cleared
   * when all query cache requests have been removed.
   * @see #addCacheQueriesRequest()
   */
  public void removeCacheQueriesRequest() {
    if (this.cacheQueriesRequests > 0)
      this.cacheQueriesRequests--;
    if (cacheQueriesRequests == 0)
      queryCache.clear();
  }

  /**
   * @return the number of cache query requests
   */
  public int getCacheQueriesRequests() {
    return cacheQueriesRequests;
  }

  /**
   * Performs the given sql query and returns the result in a List
   * @param sql the query
   * @param resultPacker a ResultPacker instance for creating the return List
   * @param fetchCount the number of records to retrieve, use -1 to retrieve all
   * @return the query result in a List
   * @throws SQLException thrown if anything goes wrong during the query execution
   */
  public final List query(final String sql, final ResultPacker resultPacker, final int fetchCount) throws SQLException {
    requestsPerSecondCounter++;
    if (cacheQueriesRequests > 0 && fetchCount < 0) {
      if (queryCache.containsKey(sql)) {
        log.debug(user.getUsername() + " (cached): " + sql.toUpperCase()+";");
        lastResultCached = true;
        cachedPerSecondCounter++;
        return queryCache.get(sql);
      }
    }
    final long time = System.currentTimeMillis();
    lastResultCached = false;
    Statement statement = null;
    try {
      statement = connection.createStatement();
      final List result = resultPacker.pack(statement.executeQuery(sql), fetchCount);
      if (cacheQueriesRequests > 0 && fetchCount < 0)
        queryCache.put(sql, result);

      log.debug(sql + " --(" + Long.toString(System.currentTimeMillis()-time) + "ms)");

      return result;
    }
    catch (SQLException e) {
      log.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): " + sql+";", e);
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
   * @param sql the query, it must select at least a single string column, any other
   * subsequent columns are disregarded
   * @return a List of Strings representing the query result
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  public final List<String> queryStrings(final String sql) throws SQLException {
    final List res = query(sql, DbUtil.STRING_PACKER, -1);
    final List<String> strings = new ArrayList<String>(res.size());
    for (final Object object : res)
      strings.add((String) object);

    return strings;
  }

  /**
   * Performs the given query and returns the result as an integer
   * @param sql the query must select at least a single number column, any other
   * subsequent columns are disregarded
   * @return the first record in the result as a integer
   * @throws SQLException thrown if anything goes wrong during the execution
   * @throws DbException thrown if no record is found
   */
  public final int queryInteger(final String sql) throws SQLException, DbException {
    final List<Integer> integers = queryIntegers(sql);
    if (integers.size() > 0)
      return integers.get(0);

    throw new DbException("No records returned when querying for an integer", sql);
  }

  /**
   * Performs the given query and returns the result as a List of Integers
   * @param sql the query, it must select at least a single number column, any other
   * subsequent columns are disregarded
   * @return a List of Integers representing the query result
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  @SuppressWarnings({"unchecked"})
  public final List<Integer> queryIntegers(final String sql) throws SQLException {
    return (List<Integer>) query(sql, DbUtil.INT_PACKER, -1);
  }

  /**
   * @param sql the query
   * @param fetchCount the maximum number of records to return, -1 for all
   * @return the result of this query, in a List of rows represented as Lists
   * @throws SQLException thrown if anything goes wrong during the query execution
   */
  @SuppressWarnings({"unchecked"})
  public final List<List> queryObjects(final String sql, final int fetchCount) throws SQLException {
    return (List<List>) query(sql, new MixedResultPacker(), fetchCount);
  }

  public final byte[] readBlobField(final String tableName, final String columnName, final String whereClause) throws SQLException {
    //http://www.idevelopment.info/data/Programming/java/jdbc/LOBS/BLOBFileExample.java
    final String sql = "select " + columnName + " from " + tableName + " " + whereClause;

    final List result = query(sql, new ResultPacker() {
      public List pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
        final List<Blob> blobs = new ArrayList<Blob>();
        if (resultSet.next())
          blobs.add(resultSet.getBlob(1));

        return blobs;
      }
    }, 1);

    final Blob blob = (Blob) result.get(0);

    return blob.getBytes(1, (int) blob.length());
  }

  public final void writeBlobField(final byte[] blobData, final String tableName, final String columnName,
                                   final String whereClause) throws SQLException {
    requestsPerSecondCounter++;
    final long time = System.currentTimeMillis();
    ByteArrayInputStream inputStream = null;
    PreparedStatement statement = null;
    final String sql = "update " + tableName + " set " + columnName + " = ? " + whereClause;
    try {
      statement = connection.prepareStatement(sql);
      inputStream = new ByteArrayInputStream(blobData);
      statement.setBinaryStream(1, inputStream, blobData.length);
      statement.execute();
    }
    catch (SQLException e) {
      log.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): " + sql+";", e);
      throw e;
    }
    finally {
      try {
        if (inputStream != null)
          inputStream.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      try {
        if (statement != null)
          statement.close();
      }
      catch (SQLException e) {/**/}
    }
  }

  /**
   * Performs a commit
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  public final void commit() throws SQLException {
    log.debug(user.getUsername() + ": " + "commit;");
    connection.commit();
  }

  /**
   * Performs a rollback
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  public final void rollback() throws SQLException {
    log.debug(user.getUsername() + ": " + "rollback;");
    connection.rollback();
  }

  public Object executeCallableStatement(final String sqlStatement, final int outParameterType) throws SQLException {
    requestsPerSecondCounter++;
    final long time = System.currentTimeMillis();
    log.debug(sqlStatement);
    CallableStatement statement = null;
    try {
      final boolean hasOutParameter = sqlStatement.indexOf(OUT_PARAMETER_NAME) > 0;
      statement = connection.prepareCall(sqlStatement);
      if (hasOutParameter)
        statement.registerOutParameter(1, outParameterType);

      statement.execute();

      log.debug(sqlStatement + " --(" + Long.toString(System.currentTimeMillis()-time) + "ms)");

      return hasOutParameter ? statement.getObject(1) : null;
    }
    catch (SQLException e) {
      log.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): " + sqlStatement+";", e);
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
   * Executes the given statement, which can be anything except a select query.
   * @param sql the statement to execute
   * @throws SQLException thrown if anything goes wrong during execution
   */
  public final void execute(final String sql) throws SQLException {
    requestsPerSecondCounter++;
    final long time = System.currentTimeMillis();
    log.debug(sql);
    Statement statement = null;
    try {
      (statement = connection.createStatement()).executeUpdate(sql);
      log.debug(sql + " --(" + Long.toString(System.currentTimeMillis()-time) + "ms)");
    }
    catch (SQLException e) {
      log.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): " + sql+";", e);
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
   * @return the underlying Connection object
   */
  public Connection getConnection() {
    return connection;
  }

  public Database getDatabase() {
    return database;
  }

  private void revalidate() throws ClassNotFoundException, SQLException {//todo rename
    try {
      if (connection != null) {
        log.info("Revalidating connection: " + user.getUsername());
        connection.rollback();
        connection.close();
      }
      if (checkConnectionStatement != null && !checkConnectionStatement.isClosed())
        checkConnectionStatement.close();
    }
    catch (SQLException e) {/**/}

    database.loadDriver();
    connection = DriverManager.getConnection(database.getURL(connectionProperties), connectionProperties);
    connection.setAutoCommit(false);
    checkConnectionStatement = connection.createStatement();
    connected = true;
  }

  private boolean checkConnection() throws SQLException {
    if (connection != null) {
      try {
        checkConnectionStatement.executeQuery("select 1 from dual");
        return true;
      }
      catch (SQLException exception) {
        exception.printStackTrace();
        throw exception;
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

  private static class MixedResultPacker implements ResultPacker<List> {
    public List<List> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<List> result = new ArrayList<List>();
      final int columnCount = resultSet.getMetaData().getColumnCount();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        final List<Object> row = new ArrayList<Object>(columnCount);
        for (int index = 1; index <= columnCount; index++)
          row.add(resultSet.getObject(index));
        result.add(row);
      }

      return result;
    }
  }
}