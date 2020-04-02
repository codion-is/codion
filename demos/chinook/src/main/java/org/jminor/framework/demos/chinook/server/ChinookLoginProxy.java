/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolProvider;
import org.jminor.common.remote.LoginProxy;
import org.jminor.common.remote.RemoteClient;
import org.jminor.common.remote.exception.LoginException;
import org.jminor.common.remote.exception.ServerAuthenticationException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.local.LocalEntityConnections;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.framework.domain.Domain;

import static java.lang.String.valueOf;
import static org.jminor.common.Conjunction.AND;
import static org.jminor.common.db.Operator.LIKE;
import static org.jminor.common.remote.Servers.remoteClient;
import static org.jminor.framework.db.condition.Conditions.*;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

/**
 * A {@link org.jminor.common.LoggerProxy} implementation
 * authenticating via a user lookup table.
 */
public final class ChinookLoginProxy implements LoginProxy {

  /**
   * The actual user credentials to return for successfully
   * authenticated users.
   */
  private final User databaseUser = Users.parseUser("scott:tiger");

  /**
   * The Database instance we're connecting to.
   */
  private final Database database = Databases.getInstance();

  /**
   * The Domain on which to base the authentication connection.
   */
  private final Domain domain = new ChinookImpl();

  /**
   * The ConnectionPool used when authenticating users.
   */
  private final ConnectionPool connectionPool;

  public ChinookLoginProxy() throws DatabaseException {
    connectionPool = ConnectionPoolProvider.getConnectionPoolProvider()
            .createConnectionPool(databaseUser, database);
  }

  /**
   * Handles logins from clients with this id
   */
  @Override
  public String getClientTypeId() {
    return "org.jminor.framework.demos.chinook.ui.ChinookAppPanel";
  }

  @Override
  public RemoteClient doLogin(final RemoteClient remoteClient)
          throws LoginException {
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

  private void authenticateUser(final User user)
          throws LoginException {
    final EntityConnection connection = getConnectionFromPool();
    try {
      final int rows = connection.selectRowCount(
              condition(T_USER, conditionSet(AND,
                      propertyCondition(USER_USERNAME,
                              LIKE, user.getUsername()).setCaseSensitive(false),
                      propertyCondition(USER_PASSWORD_HASH,
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
      return LocalEntityConnections.createConnection(domain, database,
              connectionPool.getConnection());
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
