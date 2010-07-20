/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.LogEntry;
import org.jminor.common.model.MethodLogger;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A database connection class providing transaction control and functions for querying and manipulating data.
 */
public class DbConnectionImpl implements DbConnection {

  private static final Logger LOG = Util.getLogger(DbConnection.class);

  protected static final QueryCounter QUERY_COUNTER = new QueryCounter();

  private final User user;
  private final Database database;

  private Connection connection;
  private Statement checkConnectionStatement;
  private boolean transactionOpen = false;

  private long poolTime = -1;
  private int poolRetryCount = 0;

  /**
   * The object containing the method call log
   */
  private final MethodLogger methodLogger = new MethodLogger(100, true);

  /**
   * Constructs a new instance of the DbConnection class, initialized and ready for usage
   * @param database the database
   * @param user the user for the db-connection
   * @throws SQLException in case there is a problem connecting to the database
   * @throws ClassNotFoundException in case the database driver was not found
   */
  public DbConnectionImpl(final Database database, final User user) throws ClassNotFoundException, SQLException {
    this(database, user, database.createConnection(user));
  }

  public DbConnectionImpl(final Database database, final User user, final Connection connection) throws SQLException {
    Util.rejectNullValue(database, "database");
    Util.rejectNullValue(user, "user");
    this.database = database;
    this.user = user;
    setConnection(connection);
    if (!isConnectionValid()) {
      throw new IllegalArgumentException("Connection invalid during instantiation");
    }
  }

  /**
   * Sets the time this connection was checked into a connection pool
   * @param time the time this connection was pooled
   */
  public final void setPoolTime(final long time) {
    this.poolTime = time;
  }

  /**
   * @return the time at which this connection was pooled
   */
  public final long getPoolTime() {
    return poolTime;
  }

  public final void setPoolRetryCount(final int poolRetryCount) {
    this.poolRetryCount = poolRetryCount;
  }

  public int getPoolRetryCount() {
    return poolRetryCount;
  }

  @Override
  public String toString() {
    return "DbConnection: " + user.getUsername();
  }

  /**
   * @return the connection user
   */
  public final User getUser() {
    return user;
  }

  public final void setLoggingEnabled(final boolean enabled) {
    methodLogger.setEnabled(enabled);
  }

  /**
   * @return true if the connection is valid
   */
  public final boolean isConnectionValid() {
    try {
      return connection != null && database.supportsIsValid() ? connection.isValid(0) : checkConnection();
    }
    catch (SQLException e) {
      LOG.error(this, e);
      return false;
    }
  }

  /**
   * Disconnects this DbConnection
   */
  public final void disconnect() {
    if (!isConnected()) {
      return;
    }

    try {
      if (checkConnectionStatement != null) {
        checkConnectionStatement.close();
      }
    }
    catch (Exception e) {/**/}
    try {
      if (connection != null && !connection.isClosed()) {
        connection.rollback();
        connection.close();
      }
    }
    catch (SQLException ex) {
      LOG.error(this, ex);
    }
    connection = null;
    checkConnectionStatement = null;
  }

  /**
   * @return true if the connection is connected
   */
  public final boolean isConnected() {
    return connection != null;
  }

  /**
   * Begins a transaction on this connection
   * @throws IllegalStateException in case a transaction is already open
   */
  public final void beginTransaction() {
    if (transactionOpen) {
      throw new IllegalStateException("Transaction already open");
    }

    methodLogger.logAccess("beginTransaction", new Object[0]);
    transactionOpen = true;
    methodLogger.logExit("beginTransaction", null, null);
  }

  /**
   * Performs a rollback and ends the current transaction
   * @throws SQLException in case anything goes wrong during the rollback action
   * @throws IllegalStateException in case transaction is not open
   */
  public final void rollbackTransaction() throws SQLException {
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug(user.getUsername() + ": rollback transaction;");
      methodLogger.logAccess("rollbackTransaction", new Object[0]);
      connection.rollback();
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      methodLogger.logExit("rollbackTransaction", exception, null);
    }
  }

  /**
   * Performs a commit and ends the current transaction
   * @throws SQLException in case anything goes wrong during the commit action
   * @throws IllegalStateException in case transaction is not open
   */
  public final void commitTransaction() throws SQLException {
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug(user.getUsername() + ": commit transaction;");
      methodLogger.logAccess("commitTransaction", new Object[0]);
      connection.commit();
    }
    finally {
      transactionOpen = false;
      methodLogger.logExit("commitTransaction", null, null);
    }
  }

  /**
   * @return true if a transaction is open
   */
  public final boolean isTransactionOpen() {
    return transactionOpen;
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
    QUERY_COUNTER.count(sql);
    methodLogger.logAccess("query", new Object[] {sql});
    final long time = System.currentTimeMillis();
    Statement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    try {
      statement = connection.createStatement();
      resultSet = statement.executeQuery(sql);
      final List result = resultPacker.pack(resultSet, fetchCount);

      LOG.debug(sql + " --(" + Long.toString(System.currentTimeMillis() - time) + "ms)");

      return result;
    }
    catch (SQLException e) {
      exception = e;
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis() - time) + "ms): " + sql + ";", e);
      throw e;
    }
    finally {
      try {
        if (statement != null) {
          statement.close();
        }
        if (resultSet != null) {
          resultSet.close();
        }
      }
      catch (SQLException e) {/**/}
      methodLogger.logExit("query", exception, null);
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
    final List res = query(sql, STRING_PACKER, -1);
    final List<String> strings = new ArrayList<String>(res.size());
    for (final Object object : res) {
      strings.add((String) object);
    }

    return strings;
  }

  /**
   * Performs the given query and returns the result as an integer
   * @param sql the query must select at least a single number column, any other
   * subsequent columns are disregarded
   * @return the first record in the result as a integer
   * @throws SQLException thrown if anything goes wrong during the execution
   * @throws org.jminor.common.db.exception.DbException thrown if no record is found
   */
  public final int queryInteger(final String sql) throws SQLException, DbException {
    final List<Integer> integers = queryIntegers(sql);
    if (!integers.isEmpty()) {
      return integers.get(0);
    }

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
    return (List<Integer>) query(sql, INT_PACKER, -1);
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
    final String sql = "select " + columnName + " from " + tableName + " where " + whereClause;

    final List result = query(sql, new ResultPacker() {
      public List pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
        final List<Blob> blobs = new ArrayList<Blob>();
        if (resultSet.next()) {
          blobs.add(resultSet.getBlob(1));
        }

        return blobs;
      }
    }, 1);

    final Blob blob = (Blob) result.get(0);

    return blob.getBytes(1, (int) blob.length());
  }

  public final void writeBlobField(final byte[] blobData, final String tableName, final String columnName,
                                   final String whereClause) throws SQLException {
    final long time = System.currentTimeMillis();
    final String sql = "update " + tableName + " set " + columnName + " = ? where " + whereClause;
    QUERY_COUNTER.count(sql);
    methodLogger.logAccess("writeBlobField", new Object[] {sql});
    SQLException exception = null;
    ByteArrayInputStream inputStream = null;
    PreparedStatement statement = null;
    try {
      statement = connection.prepareStatement(sql);
      inputStream = new ByteArrayInputStream(blobData);
      statement.setBinaryStream(1, inputStream, blobData.length);
      statement.execute();
    }
    catch (SQLException e) {
      exception = e;
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): " + sql+";", e);
      throw e;
    }
    finally {
      Util.closeSilently(inputStream);
      Util.closeSilently(statement);
      methodLogger.logExit("writeBlobField", exception, null);
    }
  }

  /**
   * Performs a commit
   * @throws SQLException thrown if anything goes wrong during the execution
   * @throws IllegalStateException in case a transaction is open
   */
  public final void commit() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a commit during an open transaction");
    }

    LOG.debug(user.getUsername() + ": " + "commit;");
    methodLogger.logAccess("commit", new Object[0]);
    SQLException exception = null;
    try {
      getConnection().commit();
    }
    catch (SQLException e) {
      LOG.error("Exception during commit: " + user.getUsername(), e);
      exception = e;
    }
    finally {
      methodLogger.logExit("commit", exception, null);
    }
  }

  /**
   * Performs a rollback
   * @throws SQLException thrown if anything goes wrong during the execution
   * @throws IllegalStateException in case a transaction is open
   */
  public final void rollback() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a rollback during an open transaction");
    }

    LOG.debug(user.getUsername() + ": " + "rollback;");
    methodLogger.logAccess("rollback", new Object[0]);
    SQLException exception = null;
    try {
      getConnection().rollback();
    }
    catch (SQLException e) {
      LOG.error("Exception during rollback: " + user.getUsername(), e);
      exception = e;
    }
    finally {
      methodLogger.logExit("rollback", exception, null);
    }
  }

  public final Object executeCallableStatement(final String sqlStatement, final int outParameterType) throws SQLException {
    QUERY_COUNTER.count(sqlStatement);
    methodLogger.logAccess("executeCallableStatement", new Object[] {sqlStatement, outParameterType});
    final long time = System.currentTimeMillis();
    LOG.debug(sqlStatement);
    CallableStatement statement = null;
    SQLException exception = null;
    try {
      final boolean hasOutParameter = outParameterType == Types.NULL;
      statement = connection.prepareCall(sqlStatement);
      if (hasOutParameter) {
        statement.registerOutParameter(1, outParameterType);
      }

      statement.execute();

      LOG.debug(sqlStatement + " --(" + Long.toString(System.currentTimeMillis()-time) + "ms)");

      return hasOutParameter ? statement.getObject(1) : null;
    }
    catch (SQLException e) {
      exception = e;
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): " + sqlStatement+";", e);
      throw e;
    }
    finally {
      try {
        if (statement != null) {
          statement.close();
        }
      }
      catch (SQLException e) {/**/}
      methodLogger.logExit("executeCallableStatement", exception, null);
    }
  }

  /**
   * Executes the given statement, which can be anything except a select query.
   * @param sql the statement to execute
   * @throws SQLException thrown if anything goes wrong during execution
   */
  public final void execute(final String sql) throws SQLException {
    QUERY_COUNTER.count(sql);
    methodLogger.logAccess("execute", new Object[] {sql});
    final long time = System.currentTimeMillis();
    LOG.debug(sql);
    Statement statement = null;
    SQLException exception = null;
    try {
      statement = connection.createStatement();
      statement.executeUpdate(sql);
      LOG.debug(sql + " --(" + Long.toString(System.currentTimeMillis()-time) + "ms)");
    }
    catch (SQLException e) {
      exception = e;
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): " + sql+";", e);
      throw e;
    }
    finally {
      try {
        if (statement != null) {
          statement.close();
        }
      }
      catch (SQLException e) {/**/}
      methodLogger.logExit("execute", exception, null);
    }
  }

  /**
   * Executes the given statements, in a batch if possible, which can be anything except a select query.
   * @param statements the statements to execute
   * @throws SQLException thrown if anything goes wrong during execution
   */
  public final void execute(final List<String> statements) throws SQLException {
    Util.rejectNullValue(statements, "statements");
    if (statements.size() == 1) {
      execute(statements.get(0));
      return;
    }

    methodLogger.logAccess("execute", statements.toArray());
    final long time = System.currentTimeMillis();
    Statement statement = null;
    SQLException exception = null;
    try {
      statement = connection.createStatement();
      for (final String sql : statements) {
        statement.addBatch(sql);
        QUERY_COUNTER.count(sql);
        LOG.debug(sql);
      }
      statement.executeBatch();
      LOG.debug("batch" + " --(" + Long.toString(System.currentTimeMillis()-time) + "ms)");
    }
    catch (SQLException e) {
      exception = e;
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + "ms): batch;", e);
      throw e;
    }
    finally {
      try {
        if (statement != null) {
          statement.close();
        }
      }
      catch (SQLException e) {/**/}
      methodLogger.logExit("execute", exception, null);
    }
  }

  /**
   * @return the underlying Connection object
   */
  public final Connection getConnection() {
    if (!isConnected()) {
      throw new RuntimeException("Not connected");
    }

    return connection;
  }

  public final Database getDatabase() {
    return database;
  }

  public final List<LogEntry> getLogEntries() {
    return methodLogger.getLogEntries();
  }

  protected final MethodLogger getMethodLogger() {
    return methodLogger;
  }

  public static DatabaseStatistics getDatabaseStatistics() {
    return new DbStatistics(QUERY_COUNTER.getQueriesPerSecond(),
            QUERY_COUNTER.getSelectsPerSecond(), QUERY_COUNTER.getInsertsPerSecond(),
            QUERY_COUNTER.getDeletesPerSecond(), QUERY_COUNTER.getUpdatesPerSecond());
  }

  private void setConnection(final Connection connection) throws SQLException {
    if (isConnected()) {
      throw new IllegalStateException("Already connected");
    }

    this.connection = connection;
    connection.setAutoCommit(false);
  }

  private boolean checkConnection() throws SQLException {
    if (connection != null) {
      ResultSet rs = null;
      try {
        if (checkConnectionStatement == null) {
          checkConnectionStatement = connection.createStatement();
        }
        rs = checkConnectionStatement.executeQuery(database.getCheckConnectionQuery());
        return true;
      }
      finally {
        try {
          if (rs != null) {
            rs.close();
          }
        }
        catch (Exception e) {/**/}
      }
    }

    return false;
  }

  private static class MixedResultPacker implements ResultPacker<List> {
    public List<List> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<List> result = new ArrayList<List>();
      final int columnCount = resultSet.getMetaData().getColumnCount();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        final List<Object> row = new ArrayList<Object>(columnCount);
        for (int index = 1; index <= columnCount; index++) {
          row.add(resultSet.getObject(index));
        }
        result.add(row);
      }

      return result;
    }
  }

  public static final ResultPacker<Integer> INT_PACKER = new ResultPacker<Integer>() {
    public List<Integer> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Integer> integers = new ArrayList<Integer>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        integers.add(resultSet.getInt(1));
      }

      return integers;
    }
  };

  public static final ResultPacker<String> STRING_PACKER = new ResultPacker<String>() {
    public List<String> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<String> strings = new ArrayList<String>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        strings.add(resultSet.getString(1));
      }

      return strings;
    }
  };

  public static class QueryCounter {
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

    public QueryCounter() {
      new Timer(true).schedule(new TimerTask() {
        @Override
        public void run() {
          updateQueriesPerSecond();
        }
      }, new Date(), 2000);
    }

    public void count(final String sql) {
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

    public int getQueriesPerSecond() {
      return queriesPerSecond;
    }

    public int getSelectsPerSecond() {
      return selectsPerSecond;
    }

    public int getDeletesPerSecond() {
      return deletesPerSecond;
    }

    public int getInsertsPerSecond() {
      return insertsPerSecond;
    }

    public int getUpdatesPerSecond() {
      return updatesPerSecond;
    }

    public int getUndefinedPerSecond() {
      return undefinedPerSecond;
    }

    private void updateQueriesPerSecond() {
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
}