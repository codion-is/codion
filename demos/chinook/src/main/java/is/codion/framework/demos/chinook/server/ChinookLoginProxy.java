/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPool;
import is.codion.common.rmi.server.LoginProxy;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;

import java.sql.Types;

import static is.codion.common.Conjunction.AND;
import static is.codion.common.db.Operator.LIKE;
import static is.codion.common.db.pool.ConnectionPoolProvider.getConnectionPoolProvider;
import static is.codion.common.rmi.server.RemoteClient.remoteClient;
import static is.codion.framework.db.condition.Conditions.*;
import static is.codion.framework.db.local.LocalEntityConnections.createConnection;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.property.Properties.columnProperty;
import static is.codion.framework.domain.property.Properties.primaryKeyProperty;
import static java.lang.String.valueOf;

/**
 * A {@link is.codion.common.LoggerProxy} implementation
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
    return "is.codion.framework.demos.chinook.ui.ChinookAppPanel";
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
              condition(Authentication.T_USER, combination(AND,
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
      return createConnection(domain, database, connectionPool.getConnection(databaseUser));
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
