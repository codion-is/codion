/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Responsible for providing JDBC {@link Connection} instances. Usually this means a new connection,
 * but in some cases, for example when wrapping existing connections, an existing connection may be returned.
 * Note that when used in conjunction with a {@link ConnectionPoolWrapper} a new connection must be returned.
 */
public interface ConnectionProvider {

  /**
   * Returns a JDBC {@link Connection} instance based on the given database and user.
   * @param user the user
   * @param url the jdbc url
   * @return a JDBC {@link Connection} instance
   * @throws SQLException in case of an exception
   * @throws NullPointerException in case user or url is null
   */
  default Connection connection(User user, String url) throws SQLException {
    Properties connectionProperties = new Properties();
    connectionProperties.put(Database.USER_PROPERTY, requireNonNull(user, "user").username());
    connectionProperties.put(Database.PASSWORD_PROPERTY, String.valueOf(user.password()));

    return DriverManager.getConnection(requireNonNull(url, "url"), connectionProperties);
  }
}
