/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

/**
 * Auxiliary servers to be run in conjunction with a {@link Server} must implement this interface.
 */
public interface AuxiliaryServer {

  /**
   * Starts this server
   * @throws Exception in case of an exception
   */
  void startServer() throws Exception;

  /**
   * Stops this server
   * @throws Exception in case of an exception
   */
  void stopServer() throws Exception;

  /**
   * @return a String describing the server and its configuration, for logging purposes
   */
  String serverInformation();
}
