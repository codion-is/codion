package org.jminor.common.server;

/**
 * A login proxy.
 */
public interface LoginProxy {

  /**
   * Performs login validation for the user specified by the client info
   * and returns a client info with the same clientID and the user
   * to propatage for further login procedures, which may or may not be
   * the same user
   * @param clientInfo the client info
   * @return a new client info with the same clientID but not necessarily the same user
   * @throws ServerException.LoginException in case the login fails
   */
  ClientInfo doLogin(final ClientInfo clientInfo) throws ServerException.LoginException;
}