/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A default abstract implementation of the Database interface.
 */
public abstract class AbstractDatabase implements Database {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractDatabase.class);

  private final String databaseType;
  private final String host;
  private final String sid;
  private final String port;
  private final boolean embedded;

  /**
   * Instantiates a new AbstractDatabase using host/port/sid/embedded settings specified
   * by system properties
   * @param databaseType a string identifying the database type
   * @see #DATABASE_HOST
   * @see #DATABASE_PORT
   * @see #DATABASE_SID
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final String databaseType) {
    this(databaseType, System.getProperty(DATABASE_HOST));
  }

  /**
   * Instantiates a new AbstractDatabase using port/sid/embedded settings specified
   * by system properties
   * @param databaseType a string identifying the database type
   * @param host the database host name or path to the database files in case of an embedded database
   * @see #DATABASE_PORT
   * @see #DATABASE_SID
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final String databaseType, final String host) {
    this(databaseType, host, System.getProperty(DATABASE_PORT));
  }

  /**
   * Instantiates a new AbstractDatabase using sid/embedded settings specified
   * by system properties
   * @param databaseType a string identifying the database type
   * @param host the database host name or path to the database files in case of an embedded database
   * @param port the database server port
   * @see #DATABASE_SID
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final String databaseType, final String host, final String port) {
    this(databaseType, host, port, System.getProperty(DATABASE_SID));
  }

  /**
   * Instantiates a new AbstractDatabase using the embedded settings specified
   * by the system property
   * @param databaseType a string identifying the database type
   * @param host the database host name or path to the database files in case of an embedded database
   * @param port the database server port
   * @param sid the service identifier
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final String databaseType, final String host, final String port, final String sid) {
    this(databaseType, host, port, sid, System.getProperty(Database.DATABASE_EMBEDDED, "false").equalsIgnoreCase("true"));
  }

  /**
   * Instantiates a new AbstractDatabase
   * @param databaseType a string identifying the database type
   * @param host the database host name or path to the database files in case of an embedded database
   * @param port the database server port
   * @param sid the service identifier
   * @param embedded true if the database is embedded
   */
  public AbstractDatabase(final String databaseType, final String host, final String port, final String sid,
                          final boolean embedded) {
    Util.rejectNullValue(databaseType, "databaseType");
    this.databaseType = databaseType;
    this.host = host;
    this.port = port;
    this.sid = sid;
    this.embedded = embedded;
  }

  /** {@inheritDoc} */
  @Override
  public final String getDatabaseType() {
    return databaseType;
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
    Util.rejectNullValue(user, "user");
    Util.rejectNullValue(user.getUsername(), "Username must be provided");
    Util.rejectNullValue(user.getPassword(), "Password must be provided");
    final Properties connectionProperties = new Properties();
    connectionProperties.put(USER_PROPERTY, user.getUsername());
    connectionProperties.put(PASSWORD_PROPERTY, user.getPassword());
    DriverManager.setLoginTimeout(getLoginTimeout());
    try {
      return DriverManager.getConnection(getURL(connectionProperties), addConnectionProperties(connectionProperties));
    }
    catch (SQLException e) {
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
   * based on the values found in <code>connectionProperties</code>.
   * This default implementation returns the following assuming that <code>connectionProperties</code>
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

  protected static void loadDriver(final String driverClassName) {
    Util.rejectNullValue(driverClassName, "driverClassName");
    try {
      Class.forName(driverClassName);
    }
    catch (ClassNotFoundException e) {
      LOG.warn(driverClassName + " not found on classpath", e);
    }
  }
}
