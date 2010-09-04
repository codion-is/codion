/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * Defines the methods available to remote clients.
 * @param <T> the type of remote interface this server supplies to clients
 */
public interface RemoteServer<T> extends Remote {

  String SERVER_ADMIN_SUFFIX = "-admin";

  /**
   * Establishes a connection to this RemoteServer
   * @param clientInfo the client info
   * @return a remote connection instance
   * @throws RemoteException in case of a RemoteException
   * @throws ServerException.ServerFullException in case the server isn't accepting more connections
   * @throws ServerException.LoginException in case the login fails
   */
  T connect(final ClientInfo clientInfo) throws RemoteException, ServerException.ServerFullException,
          ServerException.LoginException;

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
   * @return the server name
   * @throws RemoteException in case of a communication error
   */
  String getServerName() throws RemoteException;

  /**
   * @return the server port
   * @throws RemoteException in case of a RemoteException
   */
  int getServerPort() throws RemoteException;

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
}
