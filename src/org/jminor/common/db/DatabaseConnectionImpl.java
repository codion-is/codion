/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.LogEntry;
import org.jminor.common.model.MethodLogger;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.List;

/**
 * A default DatabaseConnection implementation, which wraps a standard JDBC Connection object.
 */
public class DatabaseConnectionImpl implements DatabaseConnection {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseConnection.class);

  private static final String EXECUTE = "execute";
  private static final String MS_LOG_PREFIX = "ms): ";
  private static final String MS_LOG_POSTFIX = "ms)";
  private static final String LOG_COMMENT_PREFIX = " --(";

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
   * Constructs a new DatabaseConnectionImpl instance, initialized and ready for usage
   * @param database the database
   * @param user the user to base this database connection on
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws ClassNotFoundException in case the database driver was not found
   */
  public DatabaseConnectionImpl(final Database database, final User user) throws ClassNotFoundException, DatabaseException {
    Util.rejectNullValue(database, "database");
    Util.rejectNullValue(user, "user");
    this.database = database;
    this.user = user;
    setConnection(database.createConnection(user));
  }

  /**
   * Constructs a new DatabaseConnectionImpl instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param connection the Connection object to base this DatabaseConnectionImpl on
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case of an exception while retrieving the username from the connection
   * meta data or if a validation statement is required and creating it fails
   */
  public DatabaseConnectionImpl(final Database database, final Connection connection) throws DatabaseException {
    Util.rejectNullValue(database, "database");
    this.database = database;
    if (!isValid(database, connection, null)) {
      throw new IllegalArgumentException("Connection invalid during instantiation");
    }
    this.user = getUser(connection);
    setConnection(connection);
  }

  /** {@inheritDoc} */
  @Override
  public final void setPoolTime(final long time) {
    this.poolTime = time;
  }

  /** {@inheritDoc} */
  @Override
  public final long getPoolTime() {
    return poolTime;
  }

  /** {@inheritDoc} */
  @Override
  public final void setRetryCount(final int retryCount) {
    this.poolRetryCount = retryCount;
  }

  /** {@inheritDoc} */
  @Override
  public final int getRetryCount() {
    return poolRetryCount;
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + user.getUsername();
  }

  /** {@inheritDoc} */
  @Override
  public final User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  @Override
  public final void setLoggingEnabled(final boolean enabled) {
    methodLogger.setEnabled(enabled);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isLoggingEnabled() {
    return methodLogger.isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValid() {
    return isValid(database, connection, checkConnectionStatement);
  }

  /** {@inheritDoc} */
  @Override
  public final void disconnect() {
    if (!isConnected()) {
      return;
    }

    try {
      if (checkConnectionStatement != null) {
        checkConnectionStatement.close();
      }
    }
    catch (Exception ignored) {}
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
  @Override
  public final boolean isConnected() {
    return connection != null;
  }

  /**
   * @return the underlying Connection object
   */
  @Override
  public final Connection getConnection() {
    if (!isConnected()) {
      throw new IllegalStateException("Not connected");
    }

    return connection;
  }

  /** {@inheritDoc} */
  @Override
  public final Database getDatabase() {
    return database;
  }

  /** {@inheritDoc} */
  @Override
  public final void beginTransaction() {
    if (transactionOpen) {
      throw new IllegalStateException("Transaction already open");
    }

    methodLogger.logAccess("beginTransaction", new Object[0]);
    transactionOpen = true;
    methodLogger.logExit("beginTransaction", null, null);
  }

  /** {@inheritDoc} */
  @Override
  public final void rollbackTransaction(){
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug("{}: rollback transaction;", user.getUsername());
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
  @Override
  public final void commitTransaction(){
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug("{}: commit transaction;", user.getUsername());
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
  @Override
  public final boolean isTransactionOpen() {
    return transactionOpen;
  }

  /** {@inheritDoc} */
  @Override
  public final List query(final String sql, final ResultPacker resultPacker, final int fetchCount) throws SQLException {
    Databases.QUERY_COUNTER.count(sql);
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
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis() - time) + MS_LOG_PREFIX + sql + ";", e);
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
      catch (SQLException ignored) {}
      methodLogger.logExit("query", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final List<String> queryStrings(final String sql) throws SQLException {
    final List res = query(sql, Databases.STRING_PACKER, -1);
    final List<String> strings = new ArrayList<String>(res.size());
    for (final Object object : res) {
      strings.add((String) object);
    }

    return strings;
  }

  /** {@inheritDoc} */
  @Override
  public final int queryInteger(final String sql) throws SQLException {
    final List<Integer> integers = queryIntegers(sql);
    if (!integers.isEmpty()) {
      return integers.get(0);
    }

    throw new SQLException("No records returned when querying for an integer", sql);
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings({"unchecked"})
  public final List<Integer> queryIntegers(final String sql) throws SQLException {
    return (List<Integer>) query(sql, Databases.INT_PACKER, -1);
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings({"unchecked"})
  public final List<List> queryObjects(final String sql, final int fetchCount) throws SQLException {
    return (List<List>) query(sql, new MixedResultPacker(), fetchCount);
  }

  /** {@inheritDoc} */
  @Override
  public final byte[] readBlobField(final String tableName, final String columnName, final String whereClause) throws SQLException {
    //http://www.idevelopment.info/data/Programming/java/jdbc/LOBS/BLOBFileExample.java
    final String sql = "select " + columnName + " from " + tableName + " where " + whereClause;

    final List result = query(sql, new BlobResultPacker(), 1);

    final Blob blob = (Blob) result.get(0);

    return blob.getBytes(1, (int) blob.length());
  }

  /** {@inheritDoc} */
  @Override
  public final void writeBlobField(final byte[] blobData, final String tableName, final String columnName,
                                   final String whereClause) throws SQLException {
    final long time = System.currentTimeMillis();
    final String sql = "update " + tableName + " set " + columnName + " = ? where " + whereClause;
    Databases.QUERY_COUNTER.count(sql);
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
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + MS_LOG_PREFIX + sql+";", e);
      throw e;
    }
    finally {
      Util.closeSilently(inputStream);
      Util.closeSilently(statement);
      methodLogger.logExit("writeBlobField", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void commit() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a commit during an open transaction");
    }

    LOG.debug("{}: commit;", user.getUsername());
    methodLogger.logAccess("commit", new Object[0]);
    SQLException exception = null;
    try {
      getConnection().commit();
    }
    catch (SQLException e) {
      LOG.error("Exception during commit: " + user.getUsername(), e);
      exception = e;
      throw e;
    }
    finally {
      methodLogger.logExit("commit", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void rollback() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a rollback during an open transaction");
    }

    LOG.debug("{}: rollback;", user.getUsername());
    methodLogger.logAccess("rollback", new Object[0]);
    SQLException exception = null;
    try {
      getConnection().rollback();
    }
    catch (SQLException e) {
      LOG.error("Exception during rollback: " + user.getUsername(), e);
      exception = e;
      throw e;
    }
    finally {
      methodLogger.logExit("rollback", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Object executeCallableStatement(final String sqlStatement, final int outParameterType) throws SQLException {
    Databases.QUERY_COUNTER.count(sqlStatement);
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
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + MS_LOG_PREFIX + sqlStatement+";", e);
      throw e;
    }
    finally {
      try {
        if (statement != null) {
          statement.close();
        }
      }
      catch (SQLException ignored) {}
      methodLogger.logExit("executeCallableStatement", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void execute(final String sql) throws SQLException {
    Databases.QUERY_COUNTER.count(sql);
    methodLogger.logAccess(EXECUTE, new Object[] {sql});
    final long time = System.currentTimeMillis();
    LOG.debug(sql);
    Statement statement = null;
    SQLException exception = null;
    try {
      statement = connection.createStatement();
      statement.executeUpdate(sql);
      LOG.debug(sql + LOG_COMMENT_PREFIX + Long.toString(System.currentTimeMillis() - time) + MS_LOG_POSTFIX);
    }
    catch (SQLException e) {
      exception = e;
      LOG.error(user.getUsername() + " (" + Long.toString(System.currentTimeMillis()-time) + MS_LOG_PREFIX + sql+";", e);
      throw e;
    }
    finally {
      try {
        if (statement != null) {
          statement.close();
        }
      }
      catch (SQLException ignored) {}
      methodLogger.logExit(EXECUTE, exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final List<?> executeFunction(final String functionID, final Object... arguments) throws DatabaseException {
    if (transactionOpen) {
      throw new DatabaseException("Can not execute a function within an open transaction");
    }
    final List<Object> returnArguments = Databases.getFunction(functionID).execute(this, arguments);
    if (transactionOpen) {
      rollbackTransaction();
      throw new DatabaseException("Function with ID: " + functionID + " did not end the transaction");
    }

    return returnArguments;
  }

  /** {@inheritDoc} */
  @Override
  public final void executeProcedure(final String procedureID, final Object... arguments) throws DatabaseException {
    if (transactionOpen) {
      throw new DatabaseException("Can not execute a procedure within an open transaction");
    }
    Databases.getProcedure(procedureID).execute(this, arguments);
    if (transactionOpen) {
      rollbackTransaction();
      throw new DatabaseException("Procedure with ID: " + procedureID + " did not end the transaction");
    }
  }

  /** {@inheritDoc} */
  @Override
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
        Databases.QUERY_COUNTER.count(sql);
        LOG.debug(sql);
      }
      statement.executeBatch();
      LOG.debug("batch" + LOG_COMMENT_PREFIX + Long.toString(System.currentTimeMillis() - time) + MS_LOG_POSTFIX);
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
      catch (SQLException ignored) {}
      methodLogger.logExit(EXECUTE, exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final List<LogEntry> getLogEntries() {
    return methodLogger.getLogEntries();
  }

  /** {@inheritDoc} */
  @Override
  public final MethodLogger getMethodLogger() {
    return methodLogger;
  }

  private void setConnection(final Connection connection) throws DatabaseException {
    if (isConnected()) {
      throw new IllegalStateException("Already connected");
    }

    this.connection = connection;
    try {
      connection.setAutoCommit(false);
    }
    catch (SQLException e) {
      throw new DatabaseException(e, "", "Unable to disable auto commit on the given connection");
    }
    if (!database.supportsIsValid()) {
      try {
        this.checkConnectionStatement = connection.createStatement();
      }
      catch (SQLException e) {
        throw new DatabaseException(e, "", "Unable to create a statement for validity checking");
      }
    }
  }

  private static boolean isValid(final Database database, final Connection connection, final Statement checkConnectionStatement) {
    if (connection == null) {
      return false;
    }
    Statement temporaryStatement = null;
    try {
      if (database.supportsIsValid()) {
        return connection.isValid(0);
      }

      if (checkConnectionStatement == null) {
        temporaryStatement = connection.createStatement();
      }

      return checkConnection(database, checkConnectionStatement == null ? temporaryStatement : checkConnectionStatement);
    }
    catch (SQLException e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
    finally {
      if (temporaryStatement != null) {
        Util.closeSilently(temporaryStatement);
      }
    }
  }

  private static boolean checkConnection(final Database database, final Statement checkConnectionStatement) {
    ResultSet rs = null;
    try {
      rs = checkConnectionStatement.executeQuery(database.getCheckConnectionQuery());
      return true;
    }
    catch (SQLException e) {
      return false;
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
      }
      catch (Exception ignored) {}
    }
  }

  /**
   * Returns a User with the username from the meta data retrieved from the given connection
   * @param connection the connection
   * @return a user based on the information gleamed from the given connection
   * @throws DatabaseException in case of an exception while retrieving
   * the username from the connection meta data
   * @see java.sql.DatabaseMetaData#getUserName()
   */
  private static User getUser(final Connection connection) throws DatabaseException {
    try {
      return new User(connection.getMetaData().getUserName(), null);
    }
    catch (SQLException e) {
      throw new DatabaseException(e, "", "Exception while trying to retrieve username from meta data");
    }
  }

  private static final class MixedResultPacker implements ResultPacker<List> {
    /** {@inheritDoc} */
    @Override
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
    @Override
    public List pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Blob> blobs = new ArrayList<Blob>();
      if (resultSet.next()) {
        blobs.add(resultSet.getBlob(1));
      }

      return blobs;
    }
  }
}