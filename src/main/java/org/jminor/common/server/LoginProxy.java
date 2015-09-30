/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

/**
 * A login proxy.
 */
public interface LoginProxy {

  /**
   * @return the String identifying the client type for which to use this login proxy
   */
  String getClientTypeID();

  /**
   * Performs login validation for the user specified by the client info
   * and returns a client info with the same clientID and user but possibly
   * a different databaseUser to propagate to further login procedures
   * @param clientInfo the client info
   * @return a new client info with the same clientID but not necessarily the same user or databaseUser
   * @throws ServerException.LoginException in case the login fails
   * @see org.jminor.common.server.ClientInfo#getDatabaseUser()
   */
  ClientInfo doLogin(final ClientInfo clientInfo) throws ServerException.LoginException;

  /**
   * Called after the given client has been disconnected
   * @param clientInfo the client info
   */
  void doLogout(final ClientInfo clientInfo);

  /**
   * Disposes of all resources used by this LoginProxy, after a call to this
   * method the proxy should be regarded as unusable.
   * This method should be called by a server using this LoginProxy on shutdown,
   * giving the LoginProxy a chance to release resources in an orderly manner.
   * Any exception thrown by this method is ignored.
   */
  void close();
}