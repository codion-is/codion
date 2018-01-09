/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

/**
 * A login proxy.
 */
public interface LoginProxy {

  /**
   * @return the String identifying the client type for which to use this login proxy
   */
  String getClientTypeId();

  /**
   * Performs login validation for the user specified by the remote client
   * and returns a remote client with the same clientId and user but possibly
   * a different databaseUser to propagate to further login procedures
   * @param remoteClient the client
   * @return a new client with the same clientId but not necessarily the same user or databaseUser
   * @throws ServerException.LoginException in case the login fails
   * @see RemoteClient#getDatabaseUser()
   */
  RemoteClient doLogin(final RemoteClient remoteClient) throws ServerException.LoginException;

  /**
   * Called after the given client has been disconnected
   * @param remoteClient the remote client
   */
  void doLogout(final RemoteClient remoteClient);

  /**
   * Disposes of all resources used by this LoginProxy, after a call to this
   * method the proxy should be regarded as unusable.
   * This method should be called by a server using this LoginProxy on shutdown,
   * giving the LoginProxy a chance to release resources in an orderly manner.
   * Any exception thrown by this method is ignored.
   */
  void close();
}