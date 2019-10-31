/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.server;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.remote.LoginProxy;
import org.jminor.common.remote.RemoteClient;
import org.jminor.common.remote.ServerException;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;

import static java.lang.String.valueOf;
import static org.jminor.common.Conjunction.AND;
import static org.jminor.common.db.ConditionType.LIKE;
import static org.jminor.common.remote.Servers.remoteClient;
import static org.jminor.framework.db.condition.Conditions.*;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

/**
 * A {@link org.jminor.common.LoggerProxy} implementation
 * authenticating via a user lookup table.
 */
public final class ChinookLoginProxy implements LoginProxy {

  /**
   * The actual user used to connect to the database
   */
  private static final User DATABASE_USER =
          new User("scott", "tiger".toCharArray());

  /**
   * Manages the database connection used to lookup users
   */
  private final LocalEntityConnectionProvider connectionProvider;

  public ChinookLoginProxy() {
    connectionProvider = (LocalEntityConnectionProvider)
            new LocalEntityConnectionProvider(Databases.getInstance())
                    .setClientTypeId(getClass().getName())
                    .setUser(DATABASE_USER)
                    .setDomainClassName(ChinookImpl.class.getName());
  }

  /**
   * Handles logins from clients with this id
   */
  @Override
  public String getClientTypeId() {
    return "org.jminor.framework.demos.chinook.client.ui.ChinookAppPanel";
  }

  @Override
  public RemoteClient doLogin(final RemoteClient remoteClient)
          throws ServerException.LoginException {
    authenticateUser(remoteClient.getUser());

    //Create a new RemoteClient based on the one received
    //but with the actual database user
    return remoteClient(remoteClient, DATABASE_USER);
  }

  @Override
  public void doLogout(final RemoteClient remoteClient) {}

  @Override
  public void close() {
    connectionProvider.disconnect();
  }

  private void authenticateUser(final User user)
          throws ServerException.LoginException {
    synchronized (connectionProvider) {
      try {
        final int rows = connectionProvider.getConnection().selectRowCount(
                entityCondition(T_USER, conditionSet(AND,
                        propertyCondition(USER_USERNAME,
                                LIKE, user.getUsername(), false),
                        propertyCondition(USER_PASSWORD_HASH,
                                LIKE, valueOf(user.getPassword()).hashCode()))));
        if (rows == 0) {
          throw ServerException.loginException("Wrong username or password");
        }
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
