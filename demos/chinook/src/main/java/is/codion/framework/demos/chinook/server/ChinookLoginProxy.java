/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.rmi.server.LoginProxy;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.db.condition.Conditions.where;
import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.property.Properties.columnProperty;
import static is.codion.framework.domain.property.Properties.primaryKeyProperty;
import static java.lang.String.valueOf;

/**
 * A {@link is.codion.common.rmi.server.LoginProxy} implementation
 * authenticating via a user lookup table.
 */
public final class ChinookLoginProxy implements LoginProxy {

  /**
   * The Database instance we're connecting to.
   */
  private final Database database = DatabaseFactory.getDatabase();

  /**
   * The actual user credentials to return for successfully authenticated users.
   * Also used for user lookup.
   */
  private final User databaseUser = User.parseUser("scott:tiger");

  /**
   * The Domain containing the authentication table.
   */
  private final Domain domain = new Authentication();

  /**
   * The ConnectionPool used when authenticating users.
   */
  private final ConnectionPoolWrapper connectionPool;

  public ChinookLoginProxy() throws DatabaseException {
    connectionPool = ConnectionPoolFactory.connectionPoolFactory().createConnectionPoolWrapper(database, databaseUser);
  }

  /**
   * Handles logins from clients with this id
   */
  @Override
  public String getClientTypeId() {
    return "is.codion.framework.demos.chinook.ui.ChinookAppPanel";
  }

  @Override
  public RemoteClient login(final RemoteClient remoteClient) throws LoginException {
    authenticateUser(remoteClient.getUser());

    //Create a new RemoteClient based on the one received
    //but with the actual database user
    return remoteClient.withDatabaseUser(databaseUser);
  }

  @Override
  public void logout(final RemoteClient remoteClient) {}

  @Override
  public void close() {
    connectionPool.close();
  }

  private void authenticateUser(final User user) throws LoginException {
    try (final EntityConnection connection = getConnectionFromPool()) {
      final int rows = connection.rowCount(where(Authentication.User.USERNAME)
              .equalTo(user.getUsername()).caseSensitive(false)
                      .and(where(Authentication.User.PASSWORD_HASH)
                              .equalTo(valueOf(user.getPassword()).hashCode())));
      if (rows == 0) {
        throw new ServerAuthenticationException("Wrong username or password");
      }
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private EntityConnection getConnectionFromPool() {
    try {
      return localEntityConnection(domain, database, connectionPool.getConnection(databaseUser));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static final class Authentication extends DefaultDomain {

    private static final DomainType DOMAIN = domainType(Authentication.class);

    interface User {
      EntityType<Entity> TYPE = DOMAIN.entityType("chinook.user");
      Attribute<Integer> ID = TYPE.integerAttribute("userid");
      Attribute<String> USERNAME = TYPE.stringAttribute("username");
      Attribute<Integer> PASSWORD_HASH = TYPE.integerAttribute("passwordhash");
    }

    private Authentication() {
      super(DOMAIN);
      define(User.TYPE,
              primaryKeyProperty(User.ID),
              columnProperty(User.USERNAME),
              columnProperty(User.PASSWORD_HASH))
              .readOnly();
    }
  }
}
