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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
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
 * A default DbConnection implementation, which wraps a standard JDBC Connection object.
 */
public class DbConnectionImpl implements DbConnection {

  private static final Logger LOG = LoggerFactory.getLogger(DbConnection.class);

  private static final String EXECUTE = "execute";
  private static final String MS_LOG_PRESTFIX = "ms): ";
  private static final String MS_LOG_POSTFIX = "ms)";
  private static final String LOG_COMMENT_PREFIX = " --(";

  /**
   * A synchronized query counter
   */
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

  /**
   * Constructs a new instance of the DbConnection class, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param user the user for the db-connection
   * @param connection the Connection object to base this DbConnection on
   * @throws SQLException in case there is a problem connecting to the database
   */
  public DbConnectionImpl(final Database database, final User user, final Connection connection) throws SQLException {
    Util.rejectNullValue(database, "database");
    Util.rejectNullValue(user, "user");
    this.database = database;
    this.user = user;
    setConnection(connection);
    if (!isValid()) {
      throw new IllegalArgumentException("Connection invalid during instantiation");
    }
  }

  /** {@inheritDoc} */
  public final void setPoolTime(final long time) {
    this.poolTime = time;
  }

  /** {@inheritDoc} */
  public final long getPoolTime() {
    return poolTime;
  }

  /** {@inheritDoc} */
  public final void setRetryCount(final int retryCount) {
    this.poolRetryCount = retryCount;
  }

  /** {@inheritDoc} */
  public final int getRetryCount() {
    return poolRetryCount;
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + user.getUsername();
  }

  /** {@inheritDoc} */
  public final User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  public final void setLoggingEnabled(final boolean enabled) {
    methodLogger.setEnabled(enabled);
  }

  /** {@inheritDoc} */
  public final boolean isLoggingEnabled() {
    return methodLogger.isEnabled();
  }

  /** {@inheritDoc} */
  public final boolean isValid() {
    try {
      return connection != null && database.supportsIsValid() ? connection.isValid(0) : checkConnection();
    }
    catch (SQLException e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }

  /** {@inheritDoc} */
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
      LOG.error(ex.getMessage(), ex);
    }
    connection = null;
    checkConnectionStatement = null;
  }

  /** {@inheritDoc} */
  public final boolean isConnected() {
    return connection != null;
  }

  /**
   * @return the underlying Connection object
   */
  public final Connection getConnection() {
    if (!isConnected()) {
      throw new IllegalStateException("Not connected");
    }

    return connection;
  }

  /** {@inheritDoc} */
  public final Database getDatabase() {
    return database;
  }

  /** {@inheritDoc} */
  public final void beginTransaction() {
    if (transactionOpen) {
      throw new IllegalStateException("Transaction already open");
    }

    methodLogger.logAccess("beginTransaction", new Object[0]);
    transactionOpen = true;
    methodLogger.logExit("beginTransaction", null, null);
  }

  /** {@inheritDoc} */
  public final void rollbackTransaction(){
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

  /** {@inheritDoc} */
  public final void commitTransaction(){
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug(user.getUsername() + ": commit transaction;");
      methodLogger.logAccess("commitTransaction", new Object[0]);
      connection.commit();
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      methodLogger.logExit("commitTransaction", exception, null);
    }
  }

  /** {@inheritDoc} */
  public final boolean isTransactionOpen() {
    return transactionOpen;
  }

  /** {@inheritDoc} */
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

      LOG.debug(sql + LOG_COMMENT_PREFIX + Long.toString(System.currentTimeMillis() - time) + MS_LOG_POSTFIX);

      return result;
    }
    catch (SQLException e) {
      exception = e;
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis() - time) + MS_LOG_PRESTFIX + sql + ";", e);
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

  /** {@inheritDoc} */
  public final List<String> queryStrings(final String sql) throws SQLException {
    final List res = query(sql, STRING_PACKER, -1);
    final List<String> strings = new ArrayList<String>(res.size());
    for (final Object object : res) {
      strings.add((String) object);
    }

    return strings;
  }

  /** {@inheritDoc} */
  public final int queryInteger(final String sql) throws SQLException, DbException {
    final List<Integer> integers = queryIntegers(sql);
    if (!integers.isEmpty()) {
      return integers.get(0);
    }

    throw new DbException("No records returned when querying for an integer", sql);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public final List<Integer> queryIntegers(final String sql) throws SQLException {
    return (List<Integer>) query(sql, INT_PACKER, -1);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public final List<List> queryObjects(final String sql, final int fetchCount) throws SQLException {
    return (List<List>) query(sql, new MixedResultPacker(), fetchCount);
  }

  /** {@inheritDoc} */
  public final byte[] readBlobField(final String tableName, final String columnName, final String whereClause) throws SQLException {
    //http://www.idevelopment.info/data/Programming/java/jdbc/LOBS/BLOBFileExample.java
    final String sql = "select " + columnName + " from " + tableName + " where " + whereClause;

    final List result = query(sql, new BlobResultPacker(), 1);

    final Blob blob = (Blob) result.get(0);

    return blob.getBytes(1, (int) blob.length());
  }

  /** {@inheritDoc} */
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
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + MS_LOG_PRESTFIX + sql+";", e);
      throw e;
    }
    finally {
      Util.closeSilently(inputStream);
      Util.closeSilently(statement);
      methodLogger.logExit("writeBlobField", exception, null);
    }
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

      LOG.debug(sqlStatement + LOG_COMMENT_PREFIX + Long.toString(System.currentTimeMillis()-time) + MS_LOG_POSTFIX);

      return hasOutParameter ? statement.getObject(1) : null;
    }
    catch (SQLException e) {
      exception = e;
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + MS_LOG_PRESTFIX + sqlStatement+";", e);
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

  /** {@inheritDoc} */
  public final void execute(final String sql) throws SQLException {
    QUERY_COUNTER.count(sql);
    methodLogger.logAccess(EXECUTE, new Object[] {sql});
    final long time = System.currentTimeMillis();
    LOG.debug(sql);
    Statement statement = null;
    SQLException exception = null;
    try {
      statement = connection.createStatement();
      statement.executeUpdate(sql);
      LOG.debug(sql + LOG_COMMENT_PREFIX + Long.toString(System.currentTimeMillis()-time) + MS_LOG_POSTFIX);
    }
    catch (SQLException e) {
      exception = e;
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + MS_LOG_PRESTFIX + sql+";", e);
      throw e;
    }
    finally {
      try {
        if (statement != null) {
          statement.close();
        }
      }
      catch (SQLException e) {/**/}
      methodLogger.logExit(EXECUTE, exception, null);
    }
  }

  /** {@inheritDoc} */
  public final void execute(final List<String> statements) throws SQLException {
    Util.rejectNullValue(statements, "statements");
    if (statements.size() == 1) {
      execute(statements.get(0));
      return;
    }

    methodLogger.logAccess(EXECUTE, statements.toArray());
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
      LOG.debug("batch" + LOG_COMMENT_PREFIX + Long.toString(System.currentTimeMillis()-time) + MS_LOG_POSTFIX);
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
      methodLogger.logExit(EXECUTE, exception, null);
    }
  }

  /** {@inheritDoc} */
  public final List<LogEntry> getLogEntries() {
    return methodLogger.getLogEntries();
  }

  /**
   * @return the MethodLogger being used by this db connection
   */
  protected final MethodLogger getMethodLogger() {
    return methodLogger;
  }

  /**
   * @return a DatabaseStatistics object containing the most recent statistics from the underlying database
   */
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
     * Counts the given query, base on it's first character
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

  private static final class MixedResultPacker implements ResultPacker<List> {
    /** {@inheritDoc} */
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

  private static final class BlobResultPacker implements ResultPacker {
    /** {@inheritDoc} */
    public List pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Blob> blobs = new ArrayList<Blob>();
      if (resultSet.next()) {
        blobs.add(resultSet.getBlob(1));
      }

      return blobs;
    }
  }

  /**
 * A default DatabaseStatistics implementation.
   */
  private static final class DbStatistics implements DatabaseStatistics, Serializable {

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
    private DbStatistics(final int queriesPerSecond, final int selectsPerSecond, final int insertsPerSecond,
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