/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

import org.jminor.common.remote.client.ConnectionRequest;
import org.jminor.common.remote.server.exception.ConnectionNotAvailableException;
import org.jminor.common.remote.server.exception.ConnectionValidationException;
import org.jminor.common.remote.server.exception.LoginException;
import org.jminor.common.remote.server.exception.ServerAuthenticationException;
import org.jminor.common.user.User;

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
   * Specifies the client host connection parameter
   */
  String CLIENT_HOST_KEY = "clientHost";

  /**
   * Establishes a connection to this Server
   * @param connectionRequest the information required for establishing a connection
   * @return a remote connection instance
   * @throws RemoteException in case of a communication error
   * @throws ConnectionNotAvailableException in case the server isn't accepting more connections
   * @throws LoginException in case the login fails
   * @throws ConnectionValidationException in case connection validation fails
   */
  T connect(ConnectionRequest connectionRequest) throws RemoteException,
          ConnectionNotAvailableException, LoginException, ConnectionValidationException;

  /**
   * Returns the admin interface used to administer this server
   * @param user the admin user credentials
   * @return the admin interface
   * @throws RemoteException in case of a communication error
   * @throws ServerAuthenticationException in case authentication fails
   */
  A getServerAdmin(User user) throws RemoteException, ServerAuthenticationException;

  /**
   * Disconnects the connection identified by the given key.
   * @param clientId the UUID identifying the client that should be disconnected
   * @throws RemoteException in case of a communication error
   */
  void disconnect(UUID clientId) throws RemoteException;

  /**
   * @return static information about this server
   * @throws RemoteException in case of an exception
   */
  ServerInformation getServerInformation() throws RemoteException;

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
