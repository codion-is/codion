/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

/**
 * Auxiliary servers to be run in conjunction with a {@link Server} must implement this interface.
 */
public interface AuxiliaryServer {

  /**
   * Starts the server, returns when the server has completed the startup
   * @throws Exception in case of an exception
   */
  void startServer() throws Exception;

  /**
   * Stops the server, returns when the server has completed shutdown.
   * @throws Exception in case of an exception
   */
  void stopServer() throws Exception;
}
