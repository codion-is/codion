/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import java.rmi.Remote;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

/**
 * Provides a {@link AuxiliaryServer} implementation.
 * @param <C> the remote connection type provided by the parent server
 * @param <A> the admin connection type provided by the parent server
 * @param <T> the parent server type
 */
public interface AuxiliaryServerFactory<C extends Remote, A extends ServerAdmin, T extends AuxiliaryServer> {

  /**
   * Creates a server instance using the given configuration.
   * @param server the parent server
   * @return a server
   */
  T createServer(Server<C, A> server);

  /**
   * Returns the {@link AuxiliaryServerFactory} implementation found by the {@link ServiceLoader} of the given type.
   * @param classname the classname of the required auxiliary server factory
   * @param <C> the remote connection type provided by the parent server
   * @param <A> the admin connection type provided by the parent server
   * @param <T> the auxiliary server type
   * @return a {@link AuxiliaryServerFactory} implementation of the given type from the {@link ServiceLoader}.
   * @throws IllegalStateException in case no such {@link AuxiliaryServerFactory} implementation is available.
   */
  static <C extends Remote, A extends ServerAdmin, T extends AuxiliaryServer> AuxiliaryServerFactory<C, A, T> auxiliaryServerFactory(String classname) {
    requireNonNull(classname, "classname");
    for (AuxiliaryServerFactory<C, A, T> serverProvider : ServiceLoader.load(AuxiliaryServerFactory.class)) {
      if (serverProvider.getClass().getName().equals(classname)) {
        return serverProvider;
      }
    }

    throw new IllegalStateException("No auxiliary server factory of type: " + classname + " available");
  }
}
