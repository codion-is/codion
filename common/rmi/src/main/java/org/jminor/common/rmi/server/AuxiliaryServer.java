/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.rmi.server;

import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

/**
 * Auxiliary servers to be run in conjunction with a {@link Server} must implement this interface,
 * as well as provide a parameterless constructor.
 */
public interface AuxiliaryServer {

  /**
   * Sets the {@link Server} instance to run alongside.
   * @param server the server.
   */
  void setServer(Server server);

  /**
   * Starts the server, returns when the server has completed the startup
   * @throws Exception in case of an exception
   */
  void startServer() throws Exception;

  /**
   * Stops the server, returns when the server has completed shutdown.
   * Finally calls {@link #setServer(Server)} with a null parameter.
   * @throws Exception in case of an exception
   */
  void stopServer() throws Exception;

  /**
   * Returns the {@link AuxiliaryServer} implementation found by the {@link ServiceLoader} of the given type.
   * @param classname the classname of the required auxiliary server
   * @return a {@link AuxiliaryServer} implementation of the given type from the {@link ServiceLoader}.
   * @throws IllegalStateException in case no such {@link AuxiliaryServer} implementation is available.
   */
  static AuxiliaryServer getAuxiliaryServer(final String classname) {
    requireNonNull(classname, "classname");
    final ServiceLoader<AuxiliaryServer> loader = ServiceLoader.load(AuxiliaryServer.class);
    for (final AuxiliaryServer server : loader) {
      if (server.getClass().getName().equals(classname)) {
        return server;
      }
    }

    throw new IllegalStateException("No auxiliary server of type: " + classname + " available");
  }
}
