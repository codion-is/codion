/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.database;

import org.jminor.common.user.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * Responsible for providing JDBC {@link Connection} instances. Usually this means a new connection,
 * but in some cases, for example when wrapping existing connections, an existing connection may be returned.
 * Note that when used in conjunction with a {@link org.jminor.common.db.pool.ConnectionPool} a new
 * connection must be returned.
 */
public interface ConnectionProvider {

  /**
   * Returns a JDBC {@link Connection} instance based on the given database and user.
   * @param user the user
   * @param jdbcUrl the jdbc url
   * @return a JDBC {@link Connection} instance
   * @throws SQLException in case of an exception
   */
  default Connection getConnection(final User user, final String jdbcUrl) throws SQLException {
    if (nullOrEmpty(requireNonNull(user, "user").getUsername())) {
      throw new IllegalArgumentException("Username must be specified");
    }
    final Properties connectionProperties = new Properties();
    connectionProperties.put(Database.USER_PROPERTY, user.getUsername());
    connectionProperties.put(Database.PASSWORD_PROPERTY, String.valueOf(user.getPassword()));

    return DriverManager.getConnection(jdbcUrl, connectionProperties);
  }
}
