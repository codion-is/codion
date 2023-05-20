/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.user.User;

/**
 * Contains basic information about a remote client
 */
public interface RemoteClient extends ConnectionRequest {

  /**
   * @return the initial connection request this client is based on
   */
  ConnectionRequest connectionRequest();

  /**
   * @return the user used when connecting to the underlying database
   */
  User databaseUser();

  /**
   * @return the client hostname
   */
  String getClientHost();

  /**
   * @param clientHost the client hostname
   */
  void setClientHost(String clientHost);

  /**
   * Instantiates a new RemoteClient based on this instance
   * but with the specified database user
   * @param databaseUser the database user to use
   * @return a new RemoteClient instance
   */
  RemoteClient withDatabaseUser(User databaseUser);

  /**
   * @return a copy of this remote client with copies of its user instances
   */
  RemoteClient copy();

  /**
   * Instantiates a new RemoteClient
   * @param connectionRequest the connection request
   * @return a new RemoteClient instance
   */
  static RemoteClient remoteClient(ConnectionRequest connectionRequest) {
    return remoteClient(connectionRequest, connectionRequest.user());
  }

  /**
   * Instantiates a new RemoteClient
   * @param connectionRequest the connection request
   * @param databaseUser the user to use when connecting to the underlying database
   * @return a new RemoteClient instance
   */
  static RemoteClient remoteClient(ConnectionRequest connectionRequest, User databaseUser) {
    return new DefaultRemoteClient(connectionRequest, databaseUser);
  }
}