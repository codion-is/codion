/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.Util;
import org.jminor.common.db.exception.AuthenticationException;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * A default abstract implementation of the Database interface.
 */
public abstract class AbstractDatabase implements Database {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractDatabase.class);

  private final Type databaseType;
  private final String driverClassName;
  private final String host;
  private final String sid;
  private final String port;
  private final boolean embedded;

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
    this(databaseType, driverClassName, System.getProperty(DATABASE_HOST));
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
    this(databaseType, driverClassName, host, System.getProperty(DATABASE_PORT));
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
  public AbstractDatabase(final Type databaseType, final String driverClassName, final String host, final String port) {
    this(databaseType, driverClassName, host, port, System.getProperty(DATABASE_SID));
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
  public AbstractDatabase(final Type databaseType, final String driverClassName, final String host, final String port,
                          final String sid) {
    this(databaseType, driverClassName, host, port, sid, System.getProperty(Database.DATABASE_EMBEDDED,
            Boolean.FALSE.toString()).equalsIgnoreCase(Boolean.TRUE.toString()));
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
  public AbstractDatabase(final Type databaseType, final String driverClassName, final String host, final String port,
                          final String sid, final boolean embedded) {
    Objects.requireNonNull(databaseType, "databaseType");
    Objects.requireNonNull(driverClassName, "driverClassName");
    loadDriver(driverClassName);
    this.databaseType = databaseType;
    this.driverClassName = driverClassName;
    this.host = host;
    this.port = port;
    this.sid = sid;
    this.embedded = embedded;
  }

  /** {@inheritDoc} */
  @Override
  public final Type getDatabaseType() {
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
  public final String getPort() {
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
  @Override
  public final Connection createConnection(final User user) throws DatabaseException {
    Objects.requireNonNull(user, "user");
    if (Util.nullOrEmpty(user.getUsername())) {
      throw new IllegalArgumentException("Username must be specified");
    }
    final Properties connectionProperties = new Properties();
    connectionProperties.put(USER_PROPERTY, user.getUsername());
    connectionProperties.put(PASSWORD_PROPERTY, user.getPassword());
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
  public String getCheckConnectionQuery() {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public void shutdownEmbedded(final Properties connectionProperties) {}

  /** {@inheritDoc} */
  @Override
  public String getSequenceSQL(final String sequenceName) {
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
      if (!Util.nullOrEmpty(username)) {
        authenticationInfo = USER_PROPERTY + "=" + username;
        if (!Util.nullOrEmpty(password)) {
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
    Objects.requireNonNull(driverClassName, "driverClassName");
    try {
      Class.forName(driverClassName);
    }
    catch (final ClassNotFoundException e) {
      LOG.warn(driverClassName + " not found on classpath", e);
    }
  }
}
