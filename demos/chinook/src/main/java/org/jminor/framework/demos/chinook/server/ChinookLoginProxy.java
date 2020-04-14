/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.remote.exception.LoginException;
import org.jminor.common.remote.exception.ServerAuthenticationException;
import org.jminor.common.remote.server.LoginProxy;
import org.jminor.common.remote.server.RemoteClient;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.domain.Domain;

import java.sql.Types;

import static java.lang.String.valueOf;
import static org.jminor.common.Conjunction.AND;
import static org.jminor.common.db.Operator.LIKE;
import static org.jminor.common.db.pool.ConnectionPoolProvider.getConnectionPoolProvider;
import static org.jminor.common.remote.server.Servers.remoteClient;
import static org.jminor.framework.db.condition.Conditions.*;
import static org.jminor.framework.db.local.LocalEntityConnections.createConnection;
import static org.jminor.framework.domain.entity.KeyGenerators.automatic;
import static org.jminor.framework.domain.property.Properties.columnProperty;
import static org.jminor.framework.domain.property.Properties.primaryKeyProperty;

/**
 * A {@link org.jminor.common.LoggerProxy} implementation
 * authenticating via a user lookup table.
 */
public final class ChinookLoginProxy implements LoginProxy {

  /**
   * The Database instance we're connecting to.
   */
  private final Database database = Databases.getInstance();

  /**
   * The actual user credentials to return for successfully authenticated users.
   * Also used for user lookup.
   */
  private final User databaseUser = Users.parseUser("scott:tiger");

  /**
   * The Domain containing the authentication table.
   */
  private final Domain domain = new Authentication();

  /**
   * The ConnectionPool used when authenticating users.
   */
  private final ConnectionPool connectionPool;

  public ChinookLoginProxy() throws DatabaseException {
    connectionPool = getConnectionPoolProvider().createConnectionPool(database, databaseUser);
  }

  /**
   * Handles logins from clients with this id
   */
  @Override
  public String getClientTypeId() {
    return "org.jminor.framework.demos.chinook.ui.ChinookAppPanel";
  }

  @Override
  public RemoteClient doLogin(final RemoteClient remoteClient) throws LoginException {
    authenticateUser(remoteClient.getUser());

    //Create a new RemoteClient based on the one received
    //but with the actual database user
    return remoteClient(remoteClient, databaseUser);
  }

  @Override
  public void doLogout(final RemoteClient remoteClient) {}

  @Override
  public void close() {
    connectionPool.close();
  }

  private void authenticateUser(final User user) throws LoginException {
    final EntityConnection connection = getConnectionFromPool();
    try {
      final int rows = connection.selectRowCount(
              condition(Authentication.T_USER, conditionSet(AND,
                      propertyCondition(Authentication.USER_USERNAME,
                              LIKE, user.getUsername()).setCaseSensitive(false),
                      propertyCondition(Authentication.USER_PASSWORD_HASH,
                              LIKE, valueOf(user.getPassword()).hashCode()))));
      if (rows == 0) {
        throw new ServerAuthenticationException("Wrong username or password");
      }
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
    finally {
      connection.disconnect();//returns the underlying connection to the pool
    }
  }

  private EntityConnection getConnectionFromPool() {
    try {
      return createConnection(domain, database, connectionPool.getConnection());
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static final class Authentication extends Domain {

    private static final String T_USER = "chinook.user";
    private static final String USER_USERID = "userid";
    private static final String USER_USERNAME = "username";
    private static final String USER_PASSWORD_HASH = "passwordhash";

    private Authentication() {
      define(T_USER,
              primaryKeyProperty(USER_USERID),
              columnProperty(USER_USERNAME, Types.VARCHAR)
                      .nullable(false)
                      .maximumLength(20),
              columnProperty(USER_PASSWORD_HASH, Types.INTEGER)
                      .nullable(false))
              .keyGenerator(automatic(T_USER));
    }
  }
}
