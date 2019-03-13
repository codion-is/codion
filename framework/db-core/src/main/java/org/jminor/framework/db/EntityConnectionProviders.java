/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import java.lang.reflect.InvocationTargetException;

/**
 * A factory class for handing out EntityConnectionProviders according to system properties.
 */
public final class EntityConnectionProviders {

  private EntityConnectionProviders() {}

  /**
   * @return a unconfigured {@link EntityConnectionProvider} instance,
   * based on {@link org.jminor.framework.db.EntityConnectionProvider#CLIENT_CONNECTION_TYPE} configuration value
   * @see org.jminor.framework.db.EntityConnectionProvider#CLIENT_CONNECTION_TYPE
   * @see org.jminor.framework.db.EntityConnectionProvider#REMOTE_CONNECTION_PROVIDER
   * @see org.jminor.framework.db.EntityConnectionProvider#LOCAL_CONNECTION_PROVIDER
   * @see org.jminor.framework.db.EntityConnectionProvider#HTTP_CONNECTION_PROVIDER
   */
  public static EntityConnectionProvider connectionProvider() {
    try {
      final String clientConnectionType = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
      switch (clientConnectionType) {
        case EntityConnectionProvider.CONNECTION_TYPE_REMOTE:
          return createConnectionProvider(EntityConnectionProvider.REMOTE_CONNECTION_PROVIDER.get());
        case EntityConnectionProvider.CONNECTION_TYPE_HTTP:
          return createConnectionProvider(EntityConnectionProvider.HTTP_CONNECTION_PROVIDER.get());
        case EntityConnectionProvider.CONNECTION_TYPE_LOCAL:
          return createConnectionProvider(EntityConnectionProvider.LOCAL_CONNECTION_PROVIDER.get());
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

  private static EntityConnectionProvider createConnectionProvider(final String classname)
          throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException, InstantiationException {
    return (EntityConnectionProvider) Class.forName(classname).getConstructor().newInstance();
  }
}
