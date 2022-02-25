/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static is.codion.common.db.database.Database.closeSilently;
import static java.util.Objects.requireNonNull;

/**
 * A default abstract implementation of the Database interface.
 */
public abstract class AbstractDatabase implements Database {

  protected static final String FOR_UPDATE = "for update";
  protected static final String FOR_UPDATE_NOWAIT = "for update nowait";

  static Database instance;

  private final Map<String, ConnectionPoolWrapper> connectionPools = new HashMap<>();
  private final int validityCheckTimeout = CONNECTION_VALIDITY_CHECK_TIMEOUT.get();
  private final QueryCounter queryCounter = new QueryCounter();
  private final boolean queryCounterEnabled = QUERY_COUNTER_ENABLED.get();
  private final String jdbcUrl;

  private ConnectionProvider connectionProvider = new ConnectionProvider() {};

  /**
   * Instantiates a new AbstractDatabase.
   * @param jdbcUrl the jdbc url
   */
  public AbstractDatabase(final String jdbcUrl) {
    this.jdbcUrl = requireNonNull(jdbcUrl, "jdbcUrl");
  }

  @Override
  public final String getUrl() {
    return jdbcUrl;
  }

  @Override
  public final Connection createConnection(final User user) throws DatabaseException {
    DriverManager.setLoginTimeout(getLoginTimeout());
    try {
      return connectionProvider.getConnection(user, jdbcUrl);
    }
    catch (SQLException e) {
      if (isAuthenticationException(e)) {
        throw new AuthenticationException(e.getMessage());
      }
      throw new DatabaseException(e, getErrorMessage(e));
    }
  }

  @Override
  public final boolean isConnectionValid(final Connection connection) {
    requireNonNull(connection, "connection");
    try {
      if (supportsIsValid()) {
        return connection.isValid(validityCheckTimeout);
      }

      return validateWithQuery(connection);
    }
    catch (SQLException e) {
      return false;
    }
  }

  @Override
  public final void countQuery(final String query) {
    if (queryCounterEnabled) {
      queryCounter.count(query);
    }
  }

  @Override
  public final Statistics getStatistics() {
    return queryCounter.getStatisticsAndResetCounter();
  }

  @Override
  public final void initializeConnectionPool(final ConnectionPoolFactory connectionPoolFactory,
                                             final User poolUser) throws DatabaseException {
    requireNonNull(connectionPoolFactory, "connectionPoolFactory");
    requireNonNull(poolUser, "poolUser");
    if (connectionPools.containsKey(poolUser.getUsername())) {
      throw new IllegalStateException("Connection pool for user " + poolUser.getUsername() + " has already been initialized");
    }
    connectionPools.put(poolUser.getUsername().toLowerCase(), connectionPoolFactory.createConnectionPoolWrapper(this, poolUser));
  }

  @Override
  public final ConnectionPoolWrapper getConnectionPool(final String username) {
    return connectionPools.get(requireNonNull(username, "username").toLowerCase());
  }

  @Override
  public final void closeConnectionPool(final String username) {
    ConnectionPoolWrapper connectionPoolWrapper = connectionPools.remove(requireNonNull(username, "username").toLowerCase());
    if (connectionPoolWrapper != null) {
      connectionPoolWrapper.close();
    }
  }

  @Override
  public final void closeConnectionPools() {
    for (final ConnectionPoolWrapper pool : connectionPools.values()) {
      closeConnectionPool(pool.getUser().getUsername());
    }
  }

  @Override
  public final Collection<String> getConnectionPoolUsernames() {
    return new ArrayList<>(connectionPools.keySet());
  }

  @Override
  public final void setConnectionProvider(final ConnectionProvider connectionProvider) {
    this.connectionProvider = connectionProvider == null ? new ConnectionProvider() {} : connectionProvider;
  }

  @Override
  public boolean supportsIsValid() {
    return true;
  }

  @Override
  public boolean subqueryRequiresAlias() {
    return false;
  }

  @Override
  public String getCheckConnectionQuery() {
    throw new IllegalStateException("No check connection query specified");
  }

  @Override
  public int getValidityCheckTimeout() {
    return validityCheckTimeout;
  }

  @Override
  public void shutdownEmbedded() {}

  @Override
  public String getSequenceQuery(final String sequenceName) {
    throw new UnsupportedOperationException("Sequence support is not implemented for database: " + getClass().getSimpleName());
  }

  @Override
  public String getErrorMessage(final SQLException exception) {
    return exception.getMessage();
  }

  /**
   * This default implementation returns false
   * @param exception the exception
   * @return false
   */
  @Override
  public boolean isAuthenticationException(final SQLException exception) {
    return false;
  }

  /**
   * This default implementation returns false
   * @param exception the exception
   * @return false
   */
  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return false;
  }

  @Override
  public boolean isUniqueConstraintException(final SQLException exception) {
    return false;
  }

  @Override
  public boolean isTimeoutException(final SQLException exception) {
    return false;
  }

  /**
   * @return the connection timeout in seconds
   * @see Database#LOGIN_TIMEOUT
   */
  protected int getLoginTimeout() {
    return Database.LOGIN_TIMEOUT.getOrThrow();
  }

  protected static String removeUrlPrefixOptionsAndParameters(final String url, final String... prefixes) {
    String result = url;
    for (final String prefix : prefixes) {
      if (url.toLowerCase().startsWith(prefix.toLowerCase())) {
        result = url.substring(prefix.length());
        break;
      }
    }
    if (result.contains(";")) {
      result = result.substring(0, result.indexOf(';'));
    }
    if (result.contains("?")) {
      result = result.substring(0, result.indexOf('?'));
    }

    return result;
  }

  private boolean validateWithQuery(final Connection connection) throws SQLException {
    ResultSet rs = null;
    try (final Statement statement = connection.createStatement()) {
      if (validityCheckTimeout > 0) {
        try {
          statement.setQueryTimeout(validityCheckTimeout);
        }
        catch (SQLException ignored) {/*Not all databases have implemented this feature*/}
      }
      rs = statement.executeQuery(getCheckConnectionQuery());

      return true;
    }
    finally {
      closeSilently(rs);
    }
  }

  private static final class QueryCounter {

    private static final double THOUSAND = 1000d;

    private final AtomicLong queriesPerSecondTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger queriesPerSecondCounter = new AtomicInteger();
    private final AtomicInteger selectsPerSecondCounter = new AtomicInteger();
    private final AtomicInteger insertsPerSecondCounter = new AtomicInteger();
    private final AtomicInteger updatesPerSecondCounter = new AtomicInteger();
    private final AtomicInteger deletesPerSecondCounter = new AtomicInteger();
    private final AtomicInteger undefinedPerSecondCounter = new AtomicInteger();

    /**
     * Counts the given query, based on its first character
     * @param query the sql query
     */
    private void count(final String query) {
      requireNonNull(query);
      queriesPerSecondCounter.incrementAndGet();
      switch (Character.toLowerCase(query.charAt(0))) {
        case 's':
          selectsPerSecondCounter.incrementAndGet();
          break;
        case 'i':
          insertsPerSecondCounter.incrementAndGet();
          break;
        case 'u':
          updatesPerSecondCounter.incrementAndGet();
          break;
        case 'd':
          deletesPerSecondCounter.incrementAndGet();
          break;
        default:
          undefinedPerSecondCounter.incrementAndGet();
      }
    }

    private Database.Statistics getStatisticsAndResetCounter() {
      long current = System.currentTimeMillis();
      double seconds = (current - queriesPerSecondTime.get()) / THOUSAND;
      int queriesPerSecond = 0;
      int selectsPerSecond = 0;
      int insertsPerSecond = 0;
      int updatesPerSecond = 0;
      int deletesPerSecond = 0;
      if (seconds > 0) {
        queriesPerSecond = (int) (queriesPerSecondCounter.get() / seconds);
        selectsPerSecond = (int) (selectsPerSecondCounter.get() / seconds);
        insertsPerSecond = (int) (insertsPerSecondCounter.get() / seconds);
        deletesPerSecond = (int) (deletesPerSecondCounter.get() / seconds);
        updatesPerSecond = (int) (updatesPerSecondCounter.get() / seconds);
        queriesPerSecondCounter.set(0);
        selectsPerSecondCounter.set(0);
        insertsPerSecondCounter.set(0);
        deletesPerSecondCounter.set(0);
        updatesPerSecondCounter.set(0);
        undefinedPerSecondCounter.set(0);
        queriesPerSecondTime.set(current);
      }

      return new DefaultDatabaseStatistics(current, queriesPerSecond, selectsPerSecond, insertsPerSecond, deletesPerSecond, updatesPerSecond);
    }
  }

  /**
   * A default Database.Statistics implementation.
   */
  private static final class DefaultDatabaseStatistics implements Database.Statistics, Serializable {

    private static final long serialVersionUID = 1;

    private final long timestamp;
    private final int queriesPerSecond;
    private final int selectsPerSecond;
    private final int insertsPerSecond;
    private final int deletesPerSecond;
    private final int updatesPerSecond;

    /**
     * Instantiates a new DatabaseStatistics object
     * @param timestamp the timestamp
     * @param queriesPerSecond the number of queries being run per second
     * @param selectsPerSecond the number of select queries being run per second
     * @param insertsPerSecond the number of insert queries being run per second
     * @param deletesPerSecond the number of delete queries being run per second
     * @param updatesPerSecond the number of update queries being run per second
     */
    private DefaultDatabaseStatistics(final long timestamp, final int queriesPerSecond, final int selectsPerSecond,
                                      final int insertsPerSecond, final int deletesPerSecond, final int updatesPerSecond) {
      this.timestamp = timestamp;
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
}
