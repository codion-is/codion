/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;

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

  public final String getDatabaseType() {
    return databaseType;
  }

  public final String getHost() {
    return host;
  }

  public final String getPort() {
    return port;
  }

  public final String getSid() {
    return sid;
  }

  public final boolean isEmbedded() {
    return embedded;
  }

  public final Connection createConnection(final User user) throws ClassNotFoundException, SQLException {
    Util.rejectNullValue(user, "user");
    Util.rejectNullValue(user.getUsername(), "Username must be provided");
    Util.rejectNullValue(user.getPassword(), "Password must be provided");
    loadDriver();
    final Properties connectionProperties = new Properties();
    connectionProperties.put("user", user.getUsername());
    connectionProperties.put("password", user.getPassword());

    return DriverManager.getConnection(getURL(connectionProperties), addConnectionProperties(connectionProperties));
  }

  /**
   * Returns true if the dbms supports the select for update NOWAIT option
   * @return true if NOWAIT is supported for select for update
   */
  public boolean supportsNowait() {
    return true;
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

  /**
   * Validates that the given parameters suffice to connect to the given database type.
   * <pre>
   * <code>if (embedded) {
   *   require(DATABASE_HOST, host);
   * }
   * else {
   *   require(DATABASE_HOST, host);
   *   require(DATABASE_PORT, port);
   *   require(DATABASE_SID, sid);
   * }
   * </code>
   * </pre>
   * @param databaseType the database type
   * @param host the database host name
   * @param port the database port
   * @param sid the database sid
   * @param embedded true if the database being connected to is an embedded one
   * @see org.jminor.common.model.Util#require(String, String)
   */
  protected abstract void validate(final String databaseType, final String host, final String port, final String sid, final boolean embedded);
}
