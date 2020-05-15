/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db;

import java.util.ServiceLoader;

/**
 * A factory class for handing out EntityConnectionProviders according to system properties.
 */
public final class EntityConnectionProviders {

  private EntityConnectionProviders() {}

  /**
   * @return a unconfigured {@link EntityConnectionProvider} instance,
   * based on {@link dev.codion.framework.db.EntityConnectionProvider#CLIENT_CONNECTION_TYPE} configuration value
   * @see dev.codion.framework.db.EntityConnectionProvider#CLIENT_CONNECTION_TYPE
   */
  public static EntityConnectionProvider connectionProvider() {
    final String clientConnectionType = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    final ServiceLoader<EntityConnectionProvider> loader = ServiceLoader.load(EntityConnectionProvider.class);
    for (final EntityConnectionProvider provider : loader) {
      if (provider.getConnectionType().equalsIgnoreCase(clientConnectionType)) {
        return provider;
      }
    }

    throw new IllegalArgumentException("No connection provider available for requested client connection type: " + clientConnectionType);
  }
}
