/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.rmi.server;

import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

/**
 * Provides a {@link AuxiliaryServer} implementation.
 * @param <T> the server type
 */
public interface AuxiliaryServerProvider<T extends AuxiliaryServer> {

  /**
   * Creates a server instance using the given configuration.
   * @param server the parent server
   * @return a server
   */
  T createServer(Server server);

  /**
   * Returns the {@link AuxiliaryServerProvider} implementation found by the {@link ServiceLoader} of the given type.
   * @param classname the classname of the required auxiliary server provider
   * @return a {@link AuxiliaryServerProvider} implementation of the given type from the {@link ServiceLoader}.
   * @throws IllegalStateException in case no such {@link AuxiliaryServerProvider} implementation is available.
   */
  static AuxiliaryServerProvider getAuxiliaryServerProvider(final String classname) {
    requireNonNull(classname, "classname");
    final ServiceLoader<AuxiliaryServerProvider> loader = ServiceLoader.load(AuxiliaryServerProvider.class);
    for (final AuxiliaryServerProvider serverProvider : loader) {
      if (serverProvider.getClass().getName().equals(classname)) {
        return serverProvider;
      }
    }

    throw new IllegalStateException("No auxiliary server provider of type: " + classname + " available");
  }
}
