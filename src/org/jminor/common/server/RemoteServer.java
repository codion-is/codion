/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;
import org.jminor.common.model.Version;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * A server for serving remote interfaces
 * @param <T> the type of remote interface this server supplies to clients
 * todo isn't it inconvenient that a class with the same name exists in java.rmi.server.RemoteServer?
 */
public interface RemoteServer<T extends Remote> extends Remote {

  /**
   * Establishes a connection to this RemoteServer
   * @param user the user
   * @param clientID a UUID identifying the client
   * @param clientTypeID a String identifying the client
   * @return a remote connection instance
   * @throws RemoteException in case of a RemoteException
   * @throws ServerException.ServerFullException in case the server isn't accepting more connections
   * @throws ServerException.LoginException in case the login fails
   */
  T connect(final User user, final UUID clientID, final String clientTypeID) throws RemoteException,
          ServerException.ServerFullException, ServerException.LoginException;

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
}
