/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Responsible for providing JDBC {@link Connection} instances. Usually this means a new connection,
 * but in some cases, for example when wrapping existing connections, an existing connection may be returned.
 * Note that when used in conjunction with a {@link ConnectionPoolWrapper} a new
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
  default Connection getConnection(User user, String jdbcUrl) throws SQLException {
    if (nullOrEmpty(requireNonNull(user, "user").getUsername())) {
      throw new IllegalArgumentException("Username must be specified");
    }
    Properties connectionProperties = new Properties();
    connectionProperties.put(Database.USER_PROPERTY, user.getUsername());
    connectionProperties.put(Database.PASSWORD_PROPERTY, String.valueOf(user.getPassword()));

    return DriverManager.getConnection(jdbcUrl, connectionProperties);
  }
}
