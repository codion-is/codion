/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import java.util.ServiceLoader;

/**
 * A factory class for handing out EntityConnectionProviders according to system properties.
 */
public final class EntityConnectionProviders {

  private EntityConnectionProviders() {}

  /**
   * @return a unconfigured {@link EntityConnectionProvider} instance,
   * based on {@link org.jminor.framework.db.EntityConnectionProvider#CLIENT_CONNECTION_TYPE} configuration value
   * @see org.jminor.framework.db.EntityConnectionProvider#CLIENT_CONNECTION_TYPE
   */
  public static EntityConnectionProvider connectionProvider() {
    final String clientConnectionType = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    final ServiceLoader<EntityConnectionProvider> loader = ServiceLoader.load(EntityConnectionProvider.class);
    for (final EntityConnectionProvider provider : loader) {
      if (provider.getConnectionType().toString().equalsIgnoreCase(clientConnectionType)) {
        return provider;
      }
    }

    throw new IllegalArgumentException("No connection provider available for connection type: " + clientConnectionType);
  }
}
