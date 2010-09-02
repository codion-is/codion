package org.jminor.common.server;

/**
 * A login proxy.
 */
public interface LoginProxy {

  /**
   * Performs login validation for the user specified by the client info
   * and returns a client info with the user to use in the actual login.
   * @param clientInfo the client info
   * @return a new client info
   * @throws LoginException in case the login fails
   */
  ClientInfo doLogin(final ClientInfo clientInfo) throws LoginException;
}