/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.User;
import org.jminor.common.Version;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * A server for serving remote interfaces
 * @param <T> the type of remote interface this server supplies to clients
 * @param <A> the type of the admin interface this server supplies
 */
public interface Server<T extends Remote, A extends Remote> extends Remote {

  /**
   * Establishes a connection to this Server
   * @param connectionInfo the information required for establishing a connection
   * @return a remote connection instance
   * @throws RemoteException in case of a communitation error
   * @throws ServerException.ServerFullException in case the server isn't accepting more connections
   * @throws ServerException.LoginException in case the login fails
   * @throws ServerException.ConnectionValidationException in case connection validation fails
   */
  T connect(final ConnectionInfo connectionInfo) throws RemoteException,
          ServerException.ServerFullException, ServerException.LoginException, ServerException.ConnectionValidationException;

  /**
   * Returns the admin intarface used to administer this server
   * @param user the admin user credentials
   * @return the admin interface
   * @throws RemoteException in case of a communitation error
   * @throws ServerException.AuthenticationException in case authentication fails
   */
  A getServerAdmin(final User user) throws RemoteException, ServerException.AuthenticationException ;

  /**
   * Disconnects the connection identified by the given key.
   * @param clientID the UUID identifying the client that should be disconnected
   * @throws RemoteException in case of a communication error
   */
  void disconnect(final UUID clientID) throws RemoteException;

  /**
   * @return static information about this server
   * @throws RemoteException in case of an exception
   */
  ServerInfo getServerInfo() throws RemoteException;

  /**
   * @return the server load as number of service requests per second
   * @throws RemoteException in case of an exception
   */
  int getServerLoad() throws RemoteException;

  /**
   * @return true if there are connections available
   * @throws RemoteException in case of an exception
   */
  boolean connectionsAvailable() throws RemoteException;

  /**
   * Encapsulates static server information
   */
  interface ServerInfo {
    /**
     * @return the server name
     */
    String getServerName();

    /**
     * @return a unique identifier for this server
     */
    UUID getServerID();

    /**
     * @return the server Version
     */
    Version getServerVersion();

    /**
     * @return the server port
     */
    int getServerPort();

    /**
     * @return the time of server startup
     */
    long getStartTime();
  }

  /**
   * Auxiliary servers to be run in conjunction with a Server must implement this interface,
   * as well as provide a constructor with the following signature: (Server, String, Integer)
   * for the server, file document root and port respectively
   */
  interface AuxiliaryServer {

    /**
     * Starts the server, returns when the server has completed the startup
     * @throws Exception in case of an exception
     */
    void startServer() throws Exception;

    /**
     * Stops the server, returns when the server has completed shutdown
     * @throws Exception in case of an exception
     */
    void stopServer() throws Exception;
  }
}
