/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
  ConnectionRequest getConnectionRequest();

  /**
   * @return the user used when connecting to the underlying database
   */
  User getDatabaseUser();

  /**
   * @return the client hostname
   */
  String getClientHost();

  /**
   * @param clientHost the client hostname
   */
  void setClientHost(String clientHost);

  /**
   * Instantiates a new RemoteClient
   * @param connectionRequest the connection request
   * @return a new RemoteClient instance
   */
  static RemoteClient remoteClient(final ConnectionRequest connectionRequest) {
    return remoteClient(connectionRequest, connectionRequest.getUser());
  }

  /**
   * Instantiates a new RemoteClient
   * @param connectionRequest the connection request
   * @param databaseUser the user to use when connecting to the underlying database
   * @return a new RemoteClient instance
   */
  static RemoteClient remoteClient(final ConnectionRequest connectionRequest, final User databaseUser) {
    return new DefaultRemoteClient(connectionRequest, databaseUser);
  }

  /**
   * Instantiates a new RemoteClient based on the given client
   * but with the specified database user
   * @param remoteClient the remote client to copy
   * @param databaseUser the database user to use
   * @return a new RemoteClient instance
   */
  static RemoteClient remoteClient(final RemoteClient remoteClient, final User databaseUser) {
    final RemoteClient client = remoteClient(remoteClient.getConnectionRequest(), databaseUser);
    client.setClientHost(remoteClient.getClientHost());

    return client;
  }
}