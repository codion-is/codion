/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.model.User;
import org.jminor.common.model.Version;
import org.jminor.framework.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * A factory class for handing out EntityConnectionProviders according to system properties.
 */
public final class EntityConnectionProviders {

  private static final String REMOTE_CONNECTION_PROVIDER =
          Configuration.getStringValue(Configuration.REMOTE_CONNECTION_PROVIDER);
  private static final String LOCAL_CONNECTION_PROVIDER =
          Configuration.getStringValue(Configuration.LOCAL_CONNECTION_PROVIDER);

  private EntityConnectionProviders() {}

  /**
   * Returns a EntityConnectionProvider according to system properties, using a randomly generated clientID
   * @param user the user for the connection
   * @param clientTypeID the client type id
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final User user, final String clientTypeID) {
    return connectionProvider(user, clientTypeID, (Version) null);
  }

  /**
   * Returns a EntityConnectionProvider according to system properties, using a randomly generated clientID
   * @param user the user for the connection
   * @param clientTypeID the client type id
   * @param clientVersion the client version, if any
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final User user, final String clientTypeID,
                                                            final Version clientVersion) {
    return connectionProvider(user, clientTypeID, UUID.randomUUID(), clientVersion);
  }

  /**
   * Returns a EntityConnectionProvider according to system properties
   * @param user the user for the connection
   * @param clientTypeID the client type id
   * @param clientID the unique identifier for the client requesting the connection provider
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final User user, final String clientTypeID, final UUID clientID) {
    return connectionProvider(user, clientTypeID, clientID, null);
  }

  /**
   * Returns a remote or local EntityConnectionProvider according to system properties.
   * Loads classes by name, so these need to available on the classpath
   * @param user the user for the connection
   * @param clientTypeID the client type id
   * @param clientID a unique client ID
   * @param clientVersion the client version, if any
   * @return a EntityConnectionProvider
   * @see org.jminor.framework.Configuration#CLIENT_CONNECTION_TYPE
   * @see org.jminor.framework.Configuration#REMOTE_CONNECTION_PROVIDER
   * @see org.jminor.framework.Configuration#LOCAL_CONNECTION_PROVIDER
   * @see org.jminor.framework.db.local.LocalEntityConnectionProvider
   * @see org.jminor.framework.db.remote.RemoteEntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final User user, final String clientTypeID, final UUID clientID,
                                                            final Version clientVersion) {
    try {
      if (Configuration.getStringValue(Configuration.CLIENT_CONNECTION_TYPE).equals(Configuration.CONNECTION_TYPE_REMOTE)) {
        final String serverHostName = Configuration.getStringValue(Configuration.SERVER_HOST_NAME);
        final boolean scheduleValidityCheck = Configuration.getBooleanValue(Configuration.CONNECTION_SCHEDULE_VALIDATION);

        return (EntityConnectionProvider) Class.forName(REMOTE_CONNECTION_PROVIDER).getConstructor(
                String.class, User.class, UUID.class, String.class, Version.class, boolean.class)
                .newInstance(serverHostName, user, clientID, clientTypeID, clientVersion, scheduleValidityCheck);
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
}
