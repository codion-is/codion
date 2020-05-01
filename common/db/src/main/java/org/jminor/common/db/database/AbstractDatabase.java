/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.database;

import org.jminor.common.db.exception.AuthenticationException;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * A default abstract implementation of the Database interface.
 */
public abstract class AbstractDatabase implements Database {

  private final QueryCounter queryCounter = new QueryCounter();
  private final String jdbcUrl;

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
    if (nullOrEmpty(requireNonNull(user, "user").getUsername())) {
      throw new IllegalArgumentException("Username must be specified");
    }
    final Properties connectionProperties = new Properties();
    connectionProperties.put(USER_PROPERTY, user.getUsername());
    connectionProperties.put(PASSWORD_PROPERTY, String.valueOf(user.getPassword()));
    DriverManager.setLoginTimeout(getLoginTimeout());
    try {
      return DriverManager.getConnection(getUrl(), connectionProperties);
    }
    catch (final SQLException e) {
      if (isAuthenticationException(e)) {
        throw new AuthenticationException(e.getMessage());
      }
      throw new DatabaseException(e, getErrorMessage(e));
    }
  }

  @Override
  public void countQuery(final String query) {
    queryCounter.count(query);
  }

  @Override
  public Statistics getStatistics() {
    return queryCounter.getStatisticsAndResetCounter();
  }

  @Override
  public SelectForUpdateSupport getSelectForUpdateSupport() {
    return SelectForUpdateSupport.FOR_UPDATE_NOWAIT;
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
  public void shutdownEmbedded() {}

  @Override
  public String getSequenceQuery(final String sequenceName) {
    throw new UnsupportedOperationException("Sequence support is not implemented for database: " + getClass().getSimpleName());
  }

  /**
   * Returns a string containing authentication info to append to the connection URL,
   * based on the values found in {@code connectionProperties}.
   * This default implementation returns the following assuming that {@code connectionProperties}
   * contains values for both "user" and "password" keys:
   * user=scott;password=tiger
   * The password clause is not included if no password is provided
   * @param connectionProperties the connection properties
   * @return a string containing authentication info to append to the connection URL
   */
  @Override
  public String getAuthenticationInfo(final Properties connectionProperties) {
    String authenticationInfo = null;
    if (connectionProperties != null) {
      final String username = (String) connectionProperties.get(USER_PROPERTY);
      final String password = (String) connectionProperties.get(PASSWORD_PROPERTY);
      if (!nullOrEmpty(username)) {
        authenticationInfo = USER_PROPERTY + "=" + username;
        if (!nullOrEmpty(password)) {
          authenticationInfo += ";" + PASSWORD_PROPERTY + "=" + password;
        }
      }
    }

    return authenticationInfo;
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

  /**
   * @return the connection timeout in seconds
   * @see Database#DEFAULT_LOGIN_TIMEOUT
   */
  protected int getLoginTimeout() {
    return Database.DEFAULT_LOGIN_TIMEOUT;
  }

  protected static String removeUrlPrefixAndOptions(final String url, final String... prefixes) {
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

    return result;
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
      final long current = System.currentTimeMillis();
      final double seconds = (current - queriesPerSecondTime.get()) / THOUSAND;
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
