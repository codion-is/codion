/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.Version;
import org.jminor.framework.domain.Entities;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.UUID;

/**
 * A factory class for handing out EntityConnectionProviders according to system properties.
 */
public final class EntityConnectionProviders {

  private EntityConnectionProviders() {}

  /**
   * Returns a EntityConnectionProvider according to system properties, using a randomly generated clientId
   * @param domainClass the domain model class name
   * @param clientTypeId the client type id
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final String domainClass, final String clientTypeId) {
    return connectionProvider(domainClass, clientTypeId, (Version) null);
  }

  /**
   * Returns a EntityConnectionProvider according to system properties, using a randomly generated clientId
   * @param domainClass the domain model class name
   * @param clientTypeId the client type id
   * @param clientVersion the client version, if any
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final String domainClass, final String clientTypeId,
                                                            final Version clientVersion) {
    return connectionProvider(domainClass, clientTypeId, UUID.randomUUID(), clientVersion);
  }

  /**
   * Returns a EntityConnectionProvider according to system properties
   * @param domainClass the domain model class name
   * @param clientTypeId the client type id
   * @param clientId the unique identifier for the client requesting the connection provider
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider connectionProvider(final String domainClass, final String clientTypeId, final UUID clientId) {
    return connectionProvider(domainClass, clientTypeId, clientId, null);
  }

  /**
   * Returns a remote or local EntityConnectionProvider according to system properties.
   * Loads classes by name, so these need to available on the classpath
   * @param domainClass the domain model class name
   * @param clientTypeId the client type id
   * @param clientId a unique client ID
   * @param clientVersion the client version, if any
   * @return a EntityConnectionProvider
   * @see org.jminor.framework.db.EntityConnectionProvider#CLIENT_DOMAIN_CLASS
   * @see org.jminor.framework.db.EntityConnectionProvider#CLIENT_CONNECTION_TYPE
   * @see org.jminor.framework.db.EntityConnectionProvider#REMOTE_CONNECTION_PROVIDER
   * @see org.jminor.framework.db.EntityConnectionProvider#LOCAL_CONNECTION_PROVIDER
   * @see org.jminor.framework.db.EntityConnectionProvider#HTTP_CONNECTION_PROVIDER
   */
  public static EntityConnectionProvider connectionProvider(final String domainClass, final String clientTypeId,
                                                            final UUID clientId, final Version clientVersion) {
    Objects.requireNonNull(domainClass, "domainClass");
    try {
      final String clientConnectionType = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
      switch (clientConnectionType) {
        case EntityConnectionProvider.CONNECTION_TYPE_REMOTE:
          return createRemoteConnectionProvider(domainClass, clientTypeId, clientId, clientVersion);
        case EntityConnectionProvider.CONNECTION_TYPE_HTTP:
          return createHttpConnectionProvider(domainClass, clientTypeId, clientId);
        case EntityConnectionProvider.CONNECTION_TYPE_LOCAL:
          return createLocalConnectionProvider(domainClass);
        default:
          throw new IllegalArgumentException("Unknown connection type: " + clientConnectionType);
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

  private static EntityConnectionProvider createRemoteConnectionProvider(final String domainClass, final String clientTypeId,
                                                                         final UUID clientId, final Version clientVersion)
          throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException, InstantiationException {
    final EntityConnectionProvider connectionProvider = (EntityConnectionProvider) Class.forName(
            EntityConnectionProvider.REMOTE_CONNECTION_PROVIDER.get()).getConstructor(
            String.class, UUID.class, String.class, Version.class)
            .newInstance(getDomainId(domainClass), clientId, clientTypeId, clientVersion);

    return connectionProvider;
  }

  private static EntityConnectionProvider createHttpConnectionProvider(final String domainClass, final String clientTypeId, final UUID clientId)
          throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException, InstantiationException {
    final EntityConnectionProvider connectionProvider = (EntityConnectionProvider) Class.forName(
            EntityConnectionProvider.HTTP_CONNECTION_PROVIDER.get()).getConstructor(String.class, String.class, UUID.class)
            .newInstance(getDomainId(domainClass), clientTypeId, clientId);

    return connectionProvider;
  }

  private static EntityConnectionProvider createLocalConnectionProvider(final String domainClass)
          throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException, InstantiationException {
    final Entities domain = (Entities) Class.forName(domainClass).getConstructor().newInstance();

    final EntityConnectionProvider connectionProvider = (EntityConnectionProvider) Class.forName(
            EntityConnectionProvider.LOCAL_CONNECTION_PROVIDER.get()).getConstructor(Entities.class).newInstance(domain);

    return connectionProvider;
  }

  private static String getDomainId(final String domainClass) {
    if (domainClass.contains(".")) {
      return domainClass.substring(domainClass.lastIndexOf('.') + 1);
    }

    return domainClass;
  }
}
