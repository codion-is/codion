/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.User;
import org.jminor.framework.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * A factory class for handing out EntityConnectionProviders according to system properties.
 */
public final class EntityConnectionProviders {

  private EntityConnectionProviders() {}

  private static final String REMOTE_CONNECTION_PROVIDER =
          Configuration.getStringValue(Configuration.REMOTE_CONNECTION_PROVIDER);
  private static final String LOCAL_CONNECTION_PROVIDER =
          Configuration.getStringValue(Configuration.LOCAL_CONNECTION_PROVIDER);

  /**
   * Creates a {@code EntityConnectionProvider} wrapping the given connection.
   * Note that the {@code StateObserver} returned by {@link EntityConnectionProvider#getConnectedObserver} is not enabled.
   * Note also that disconnecting this {@code EntityConnectionProvider} renders it unusable, since it does not perform reconnections.
   * @param connection the connection to wrap
   * @return a {@code EntityConnectionProvider} wrapping the connection
   */
  public static EntityConnectionProvider connectionProvider(final EntityConnection connection) {
    return new ConnectionWrapper(connection);
  }

  /**
   * Returns a EntityConnectionProvider according to system properties, using a randomly generated clientID
   * @param user the user for the connection
   * @param clientTypeID the client type id
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final User user, final String clientTypeID) {
    return connectionProvider(user, clientTypeID, UUID.randomUUID());
  }

  /**
   * Returns a EntityConnectionProvider according to system properties
   * @param user the user for the connection
   * @param clientTypeID the client type id
   * @param clientID the unique identifier for the client requesting the connection provider
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final User user, final String clientTypeID, final UUID clientID) {
    return connectionProvider(user, clientID, clientTypeID);
  }

  /**
   * Returns a remote or local EntityConnectionProvider according to system properties.
   * Loads classes by name, so these need to available on the classpath
   * @param user the user for the connection
   * @param clientID a unique client ID
   * @param clientTypeID the client type id
   * @return a EntityConnectionProvider
   * @see org.jminor.framework.Configuration#CLIENT_CONNECTION_TYPE
   * @see org.jminor.framework.Configuration#REMOTE_CONNECTION_PROVIDER
   * @see org.jminor.framework.Configuration#LOCAL_CONNECTION_PROVIDER
   * @see org.jminor.framework.db.local.LocalEntityConnectionProvider
   * @see org.jminor.framework.db.remote.RemoteEntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final User user, final UUID clientID,
                                                            final String clientTypeID) {
    try {
      if (Configuration.getStringValue(Configuration.CLIENT_CONNECTION_TYPE).equals(Configuration.CONNECTION_TYPE_REMOTE)) {
        return (EntityConnectionProvider) Class.forName(REMOTE_CONNECTION_PROVIDER).getConstructor(
                User.class, UUID.class, String.class).newInstance(user, clientID, clientTypeID);
      }
      else {
        return (EntityConnectionProvider) Class.forName(LOCAL_CONNECTION_PROVIDER).getConstructor(
                User.class).newInstance(user);
      }
    }
    catch (final InvocationTargetException ite) {
      if (ite.getTargetException() instanceof RuntimeException) {
        throw (RuntimeException) ite.getTargetException();
      }

      throw new RuntimeException("Exception while initializing connection provider", ite);
    }
    catch (final RuntimeException re) {
      throw re;
    }
    catch (final Exception e) {
      throw new RuntimeException("Exception while initializing connection provider", e);
    }
  }

  private static final class ConnectionWrapper implements EntityConnectionProvider {

    private final EntityConnection connection;
    private final StateObserver connectedState = States.state().getObserver();

    private ConnectionWrapper(final EntityConnection connection) {
      this.connection = connection;
    }

    @Override
    public EntityConnection getConnection() {
      return connection;
    }

    @Override
    public String getDescription() {
      return connection.getUser().toString();
    }

    @Override
    public String getServerHostName() {
      return ConnectionWrapper.class.getName();
    }

    @Override
    public boolean isConnected() {
      return connection.isConnected();
    }

    @Override
    public StateObserver getConnectedObserver() {
      return connectedState;
    }

    @Override
    public void disconnect() {
      connection.disconnect();
    }

    @Override
    public void setUser(final User user) {
      throw new UnsupportedOperationException();
    }

    @Override
    public User getUser() {
      return connection.getUser();
    }
  }
}
