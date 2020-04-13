/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.AuthenticationException;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG = LoggerFactory.getLogger(AbstractDatabase.class);

  private final QueryCounter queryCounter = new QueryCounter();
  private final Type databaseType;
  private final String driverClassName;
  private final String host;
  private final String sid;
  private final Integer port;
  private final boolean embedded;

  private String urlAppend = "";

  /**
   * Instantiates a new AbstractDatabase using host/port/sid/embedded settings specified by system properties
   * @param databaseType the database type
   * @param driverClassName the database driver class name
   * @see #DATABASE_HOST
   * @see #DATABASE_PORT
   * @see #DATABASE_SID
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final Type databaseType, final String driverClassName) {
    this(databaseType, driverClassName, Database.DATABASE_HOST.get());
  }

  /**
   * Instantiates a new AbstractDatabase using port/sid/embedded settings specified by system properties
   * @param databaseType the database type
   * @param driverClassName the database driver class name
   * @param host the database host name or path to the database files in case of an embedded database
   * @see #DATABASE_PORT
   * @see #DATABASE_SID
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final Type databaseType, final String driverClassName, final String host) {
    this(databaseType, driverClassName, host, Database.DATABASE_PORT.get());
  }

  /**
   * Instantiates a new AbstractDatabase using sid/embedded settings specified by system properties
   * @param databaseType the database type
   * @param driverClassName the database driver class name
   * @param host the database host name or path to the database files in case of an embedded database
   * @param port the database server port
   * @see #DATABASE_SID
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final Type databaseType, final String driverClassName, final String host, final Integer port) {
    this(databaseType, driverClassName, host, port, Database.DATABASE_SID.get());
  }

  /**
   * Instantiates a new AbstractDatabase using the embedded settings specified by the system property
   * @param databaseType the database type
   * @param driverClassName the database driver class name
   * @param host the database host name or path to the database files in case of an embedded database
   * @param port the database server port
   * @param sid the service identifier
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final Type databaseType, final String driverClassName, final String host, final Integer port,
                          final String sid) {
    this(databaseType, driverClassName, host, port, sid, Database.DATABASE_EMBEDDED.get());
  }

  /**
   * Instantiates a new AbstractDatabase
   * @param databaseType a the database type
   * @param driverClassName the database driver class name
   * @param host the database host name or path to the database files in case of an embedded database
   * @param port the database server port
   * @param sid the service identifier
   * @param embedded true if the database is embedded
   */
  public AbstractDatabase(final Type databaseType, final String driverClassName, final String host, final Integer port,
                          final String sid, final boolean embedded) {
    loadDriver(driverClassName);
    this.databaseType = requireNonNull(databaseType, "databaseType");
    this.driverClassName = driverClassName;
    this.host = host;
    this.port = port;
    this.sid = sid;
    this.embedded = embedded;
  }

  /** {@inheritDoc} */
  @Override
  public final Type getType() {
    return databaseType;
  }

  /** {@inheritDoc} */
  @Override
  public final String getDriverClassName() {
    return driverClassName;
  }

  /** {@inheritDoc} */
  @Override
  public final String getHost() {
    return host;
  }

  /** {@inheritDoc} */
  @Override
  public final Integer getPort() {
    return port;
  }

  /** {@inheritDoc} */
  @Override
  public final String getSid() {
    return sid;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isEmbedded() {
    return embedded;
  }

  /** {@inheritDoc} */
  public final void setUrlAppend(final String urlAppend) {
    this.urlAppend = urlAppend;
  }

  /** {@inheritDoc} */
  public final String getUrlAppend() {
    return urlAppend;
  }

  /** {@inheritDoc} */
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
      return DriverManager.getConnection(getURL(connectionProperties), addConnectionProperties(connectionProperties));
    }
    catch (final SQLException e) {
      if (isAuthenticationException(e)) {
        throw new AuthenticationException(e.getMessage());
      }
      throw new DatabaseException(e, getErrorMessage(e));
    }
  }

  /** {@inheritDoc} */
  @Override
  public void countQuery(final String query) {
    queryCounter.count(query);
  }

  /** {@inheritDoc} */
  @Override
  public Statistics getStatistics() {
    return queryCounter.getStatisticsAndResetCounter();
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsSelectForUpdate() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsNowait() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsIsValid() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean subqueryRequiresAlias() {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public String getCheckConnectionQuery() {
    throw new IllegalStateException("No check connection query specified");
  }

  /** {@inheritDoc} */
  @Override
  public void shutdownEmbedded(final Properties connectionProperties) {}

  /** {@inheritDoc} */
  @Override
  public String getSequenceQuery(final String sequenceName) {
    throw new UnsupportedOperationException("Sequence support is not implemented for database type: " + databaseType);
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public boolean isUniqueConstraintException(final SQLException exception) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public Properties addConnectionProperties(final Properties properties) {
    return properties;
  }

  /**
   * @return the connection timeout in seconds
   * @see Database#DEFAULT_LOGIN_TIMEOUT
   */
  protected int getLoginTimeout() {
    return Database.DEFAULT_LOGIN_TIMEOUT;
  }

  /**
   * Loads the given class by name
   * @param driverClassName the class name
   */
  private static void loadDriver(final String driverClassName) {
    try {
      Class.forName(requireNonNull(driverClassName, "driverClassName"));
    }
    catch (final ClassNotFoundException e) {
      LOG.warn(driverClassName + " not found on classpath", e);
    }
  }

  private static final class QueryCounter {

    private static final double THOUSAND = 1000d;

    private final AtomicLong queriesPerSecondTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger queriesPerSecondCounter = new AtomicInteger(0);
    private final AtomicInteger selectsPerSecondCounter = new AtomicInteger(0);
    private final AtomicInteger insertsPerSecondCounter = new AtomicInteger(0);
    private final AtomicInteger updatesPerSecondCounter = new AtomicInteger(0);
    private final AtomicInteger deletesPerSecondCounter = new AtomicInteger(0);
    private final AtomicInteger undefinedPerSecondCounter = new AtomicInteger(0);

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
