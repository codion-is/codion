/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.User;
import org.jminor.common.Version;
import org.jminor.common.server.Server;
import org.jminor.framework.domain.Entities;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * A factory class for handing out EntityConnectionProviders according to system properties.
 */
public final class EntityConnectionProviders {

  private EntityConnectionProviders() {}

  /**
   * Returns a EntityConnectionProvider according to system properties, using a randomly generated clientId
   * @param entities the domain model entities
   * @param user the user for the connection
   * @param clientTypeId the client type id
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final Entities entities, final User user, final String clientTypeId) {
    return connectionProvider(entities, user, clientTypeId, (Version) null);
  }

  /**
   * Returns a EntityConnectionProvider according to system properties, using a randomly generated clientId
   * @param entities the domain model entities
   * @param user the user for the connection
   * @param clientTypeId the client type id
   * @param clientVersion the client version, if any
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final Entities entities, final User user, final String clientTypeId,
                                                            final Version clientVersion) {
    return connectionProvider(entities, user, clientTypeId, UUID.randomUUID(), clientVersion);
  }

  /**
   * Returns a EntityConnectionProvider according to system properties
   * @param entities the domain model entities
   * @param user the user for the connection
   * @param clientTypeId the client type id
   * @param clientId the unique identifier for the client requesting the connection provider
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final Entities entities, final User user, final String clientTypeId,
                                                            final UUID clientId) {
    return connectionProvider(entities, user, clientTypeId, clientId, null);
  }

  /**
   * Returns a remote or local EntityConnectionProvider according to system properties.
   * Loads classes by name, so these need to available on the classpath
   * @param entities the domain model entities
   * @param user the user for the connection
   * @param clientTypeId the client type id
   * @param clientId a unique client ID
   * @param clientVersion the client version, if any
   * @return a EntityConnectionProvider
   * @see org.jminor.framework.db.EntityConnectionProvider#CLIENT_CONNECTION_TYPE
   * @see org.jminor.framework.db.EntityConnectionProvider#REMOTE_CONNECTION_PROVIDER
   * @see org.jminor.framework.db.EntityConnectionProvider#LOCAL_CONNECTION_PROVIDER
   */
  public static EntityConnectionProvider connectionProvider(final Entities entities, final User user, final String clientTypeId,
                                                            final UUID clientId, final Version clientVersion) {
    try {
      final String clientConnectionType = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
      if (clientConnectionType.equals(EntityConnectionProvider.CONNECTION_TYPE_REMOTE)) {
        final String serverHostName = Server.SERVER_HOST_NAME.get();
        final boolean scheduleValidityCheck = EntityConnectionProvider.CONNECTION_SCHEDULE_VALIDATION.get();

        return (EntityConnectionProvider) Class.forName(EntityConnectionProvider.REMOTE_CONNECTION_PROVIDER.get()).getConstructor(
                Entities.class, String.class, User.class, UUID.class, String.class, Version.class, boolean.class)
                .newInstance(entities, serverHostName, user, clientId, clientTypeId, clientVersion, scheduleValidityCheck);
      }
      else if (clientConnectionType.equals(EntityConnectionProvider.CONNECTION_TYPE_LOCAL)) {
        return (EntityConnectionProvider) Class.forName(EntityConnectionProvider.LOCAL_CONNECTION_PROVIDER.get()).getConstructor(
                Entities.class, User.class).newInstance(entities, user);
      }
      else if (clientConnectionType.equals(EntityConnectionProvider.CONNECTION_TYPE_HTTP)) {
        final String serverHostName = Server.SERVER_HOST_NAME.get();

        return (EntityConnectionProvider) Class.forName(EntityConnectionProvider.HTTP_CONNECTION_PROVIDER.get()).getConstructor(
                Entities.class, String.class, User.class, UUID.class).newInstance(entities, serverHostName, user, clientId);
      }

      throw new IllegalArgumentException("Unknown connection provider type: " + clientConnectionType);
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
