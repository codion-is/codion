/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

  private static final String FETCH_NEXT = "fetch next ";
  private static final String ROWS = " rows";
  private static final String ONLY = " only";
  private static final String OFFSET = "offset ";
  private static final String LIMIT = "limit ";

  private static Database instance;

  private final Map<String, ConnectionPoolWrapper> connectionPools = new HashMap<>();
  private final int validityCheckTimeout = CONNECTION_VALIDITY_CHECK_TIMEOUT.get();
  private final QueryCounter queryCounter = new QueryCounter();
  private final boolean queryCounterEnabled = QUERY_COUNTER_ENABLED.get();
  private final String url;

  private ConnectionProvider connectionProvider = new ConnectionProvider() {};

  /**
   * Instantiates a new AbstractDatabase.
   * @param url the jdbc url
   */
  public AbstractDatabase(String url) {
    this.url = requireNonNull(url, "url");
  }

  @Override
  public final String url() {
    return url;
  }

  @Override
  public final Connection createConnection(User user) throws DatabaseException {
    DriverManager.setLoginTimeout(loginTimeout());
    try {
      Connection connection = connectionProvider.connection(user, url);
      if (Database.TRANSACTION_ISOLATION.isNotNull()) {
        connection.setTransactionIsolation(Database.TRANSACTION_ISOLATION.get());
      }

      return connection;
    }
    catch (SQLException e) {
      if (isAuthenticationException(e)) {
        throw new AuthenticationException(e.getMessage());
      }
      throw new DatabaseException(e, errorMessage(e));
    }
  }

  @Override
  public final boolean isConnectionValid(Connection connection) {
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
  public final void countQuery(String query) {
    if (queryCounterEnabled) {
      queryCounter.count(query);
    }
  }

  @Override
  public final Statistics statistics() {
    return queryCounter.collectStatisticsAndResetCounter();
  }

  @Override
  public final void createConnectionPool(ConnectionPoolFactory connectionPoolFactory,
                                         User poolUser) throws DatabaseException {
    requireNonNull(connectionPoolFactory, "connectionPoolFactory");
    requireNonNull(poolUser, "poolUser");
    if (connectionPools.containsKey(poolUser.username())) {
      throw new IllegalStateException("Connection pool for user " + poolUser.username() + " has already been created");
    }
    connectionPools.put(poolUser.username().toLowerCase(), connectionPoolFactory.createConnectionPoolWrapper(this, poolUser));
  }

  @Override
  public final ConnectionPoolWrapper connectionPool(String username) {
    return connectionPools.get(requireNonNull(username, "username").toLowerCase());
  }

  @Override
  public final void closeConnectionPool(String username) {
    ConnectionPoolWrapper connectionPoolWrapper = connectionPools.remove(requireNonNull(username, "username").toLowerCase());
    if (connectionPoolWrapper != null) {
      connectionPoolWrapper.close();
    }
  }

  @Override
  public final void closeConnectionPools() {
    for (ConnectionPoolWrapper pool : connectionPools.values()) {
      closeConnectionPool(pool.user().username());
    }
  }

  @Override
  public final Collection<String> connectionPoolUsernames() {
    return new ArrayList<>(connectionPools.keySet());
  }

  @Override
  public final void setConnectionProvider(ConnectionProvider connectionProvider) {
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
  public String checkConnectionQuery() {
    throw new IllegalStateException("No check connection query specified");
  }

  @Override
  public int validityCheckTimeout() {
    return validityCheckTimeout;
  }

  @Override
  public void shutdownEmbedded() {}

  @Override
  public String sequenceQuery(String sequenceName) {
    throw new UnsupportedOperationException("Sequence support is not implemented for database: " + getClass().getSimpleName());
  }

  @Override
  public String errorMessage(SQLException exception) {
    return exception.getMessage();
  }

  /**
   * This default implementation returns false
   * @param exception the exception
   * @return false
   */
  @Override
  public boolean isAuthenticationException(SQLException exception) {
    return false;
  }

  /**
   * This default implementation returns false
   * @param exception the exception
   * @return false
   */
  @Override
  public boolean isReferentialIntegrityException(SQLException exception) {
    return false;
  }

  @Override
  public boolean isUniqueConstraintException(SQLException exception) {
    return false;
  }

  @Override
  public boolean isTimeoutException(SQLException exception) {
    return false;
  }

  static Database instance() {
    try {
      synchronized (AbstractDatabase.class) {
        String databaseUrl = DATABASE_URL.get();
        if (AbstractDatabase.instance == null || !AbstractDatabase.instance.url().equals(databaseUrl)) {
          Database previousInstance = AbstractDatabase.instance;
          //replace the instance
          AbstractDatabase.instance = DatabaseFactory.instance().createDatabase(databaseUrl);
          if (previousInstance != null) {
            //cleanup
            previousInstance.closeConnectionPools();
          }
        }

        return AbstractDatabase.instance;
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the connection timeout in seconds
   * @see Database#LOGIN_TIMEOUT
   */
  protected int loginTimeout() {
    return Database.LOGIN_TIMEOUT.getOrThrow();
  }

  protected static final String createLimitOffsetClause(Integer limit, Integer offset) {
    /* LIMIT {limit} OFFSET {offset} */
    StringBuilder builder = new StringBuilder();
    if (limit != null) {
      builder.append(LIMIT).append(limit);
    }
    if (offset != null) {
      builder.append(builder.length() == 0 ? "" : " ").append(OFFSET).append(offset);
    }

    return builder.toString();
  }

  protected static final String createOffsetFetchNextClause(Integer limit, Integer offset) {
    /* OFFSET {offset} ROWS FETCH NEXT {limit} ROWS ONLY */
    StringBuilder builder = new StringBuilder();
    if (offset != null) {
      builder.append(OFFSET).append(offset).append(ROWS);
    }
    if (limit != null) {
      builder.append(builder.length() == 0 ? "" : " ").append(FETCH_NEXT).append(limit).append(ROWS).append(ONLY);
    }

    return builder.toString();
  }

  protected static String removeUrlPrefixOptionsAndParameters(String url, String... prefixes) {
    String result = url;
    for (String prefix : prefixes) {
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

  private boolean validateWithQuery(Connection connection) throws SQLException {
    ResultSet rs = null;
    try (Statement statement = connection.createStatement()) {
      if (validityCheckTimeout > 0) {
        try {
          statement.setQueryTimeout(validityCheckTimeout);
        }
        catch (SQLException ignored) {/*Not all databases have implemented this feature*/}
      }
      rs = statement.executeQuery(checkConnectionQuery());

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
    private void count(String query) {
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

    private Database.Statistics collectStatisticsAndResetCounter() {
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
    private DefaultDatabaseStatistics(long timestamp, int queriesPerSecond, int selectsPerSecond,
                                      int insertsPerSecond, int deletesPerSecond, int updatesPerSecond) {
      this.timestamp = timestamp;
      this.queriesPerSecond = queriesPerSecond;
      this.selectsPerSecond = selectsPerSecond;
      this.insertsPerSecond = insertsPerSecond;
      this.deletesPerSecond = deletesPerSecond;
      this.updatesPerSecond = updatesPerSecond;
    }

    @Override
    public int queriesPerSecond() {
      return queriesPerSecond;
    }

    @Override
    public int deletesPerSecond() {
      return deletesPerSecond;
    }

    @Override
    public int insertsPerSecond() {
      return insertsPerSecond;
    }

    @Override
    public int selectsPerSecond() {
      return selectsPerSecond;
    }

    @Override
    public int updatesPerSecond() {
      return updatesPerSecond;
    }

    @Override
    public long timestamp() {
      return timestamp;
    }
  }
}
