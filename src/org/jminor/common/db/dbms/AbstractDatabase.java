/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A default abstract implementation of the Database interface.
 */
public abstract class AbstractDatabase implements Database {

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
   * @param host the database host name
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
   * @param host the database host name
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
   * @param host the database host name
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
   * @param host the database host name
   * @param port the database server port
   * @param sid the service identifier
   * @param embedded true if the database is embedded
   */
  public AbstractDatabase(final String databaseType, final String host, final String port, final String sid,
                          final boolean embedded) {
    validate(databaseType, host, port, sid, embedded);
    this.databaseType = databaseType;
    this.host = host;
    this.port = port;
    this.sid = sid;
    this.embedded = embedded;
  }

  /** {@inheritDoc} */
  public String getDatabaseType() {
    return databaseType;
  }

  /** {@inheritDoc} */
  public String getHost() {
    return host;
  }

  /** {@inheritDoc} */
  public String getPort() {
    return port;
  }

  /** {@inheritDoc} */
  public String getSid() {
    return sid;
  }

  /** {@inheritDoc} */
  public boolean isEmbedded() {
    return embedded;
  }

  /** {@inheritDoc} */
  public Connection createConnection(final User user) throws ClassNotFoundException, SQLException {
    if (user == null)
      throw new IllegalArgumentException("Connection requires a non-null user instance");
    if (user.getUsername() == null)
      throw new IllegalArgumentException("Username must be provided");
    if (user.getPassword() == null)
      throw new IllegalArgumentException("Password must be provided");

    loadDriver();
    final Properties connectionProperties = new Properties();
    connectionProperties.put("user", user.getUsername());
    connectionProperties.put("password", user.getPassword());
    return DriverManager.getConnection(getURL(connectionProperties),
            addConnectionProperties(connectionProperties));
  }

  /**
   * This default implementation returns true
   * @return true if the dbms supports the Java 6 jdbc call Connection.isValid()
   */
  public boolean supportsIsValid() {
    return true;
  }

  /**
   * Returns a query to use when checking if the connection is valid,
   * this is used in cases where the dbms does not support the isValid() call.
   * Returning null is safe if isValid() is supported.
   * This default implementation returns null.
   * @return a check connection query
   * @see #supportsIsValid()
   */
  public String getCheckConnectionQuery() {
    return null;
  }

  /**
   * This should shutdown the database in case it is an embedded one
   * and if that is applicable, such as for Derby.
   * This default implementation does nothing.
   * @param connectionProperties the connection properties
   */
  public void shutdownEmbedded(final Properties connectionProperties) {}

  /**
   * This default implementation returns null
   * @param connectionProperties the connection properties
   * @return null
   */
  public String getAuthenticationInfo(final Properties connectionProperties) {
    return null;
  }

  /**
   * This default implementation simply returns the error message from the exception
   * @param exception the underlying SQLException
   * @return the exception message
   */
  public String getErrorMessage(final SQLException exception) {
    return exception.getMessage();
  }

  /**
   * This default implementation simply returns the properties map.
   * @param properties the properties map to add to
   * @return the given properties map
   */
  public Properties addConnectionProperties(final Properties properties) {
    return properties;
  }

  protected abstract void validate(final String databaseType, final String host, final String port, final String sid, final boolean embedded);

  protected void require(final String property, final String value) {
    if (value == null || value.length() == 0)
      throw new RuntimeException(property + " is required for database type " + databaseType);
  }
}
