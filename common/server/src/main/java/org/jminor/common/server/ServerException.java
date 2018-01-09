/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.i18n.Messages;

/**
 * An exception originating from a remote server
 */
public class ServerException extends Exception {

  private ServerException(final String message) {
    super(message);
  }

  /**
   * @return an exception indicating that the server is not accepting new connections
   */
  public static ServerFullException serverFullException() {
    return new ServerFullException();
  }

  /**
   * @param message the exception message
   * @return an exception indicating that a login has failed
   */
  public static LoginException loginException(final String message) {
    return new LoginException(message);
  }

  /**
   * @param message the exception message
   * @return an exception indicating an authentication failure
   */
  public static AuthenticationException authenticationException(final String message) {
    return new AuthenticationException(message);
  }

  /**
   * An exception indicating that a login has failed
   */
  public static class LoginException extends ServerException {
    /**
     * Instantiates a new {@link LoginException}
     * @param message the exception message
     */
    public LoginException(final String message) {
      super(message);
    }
  }

  /**
   * An exception indicating that a login has failed due to an authentication error,
   * invalid username or password
   */
  public static class AuthenticationException extends LoginException {
    /**
     * Instantiates a new {@link AuthenticationException}
     * @param message the exception message
     */
    public AuthenticationException(final String message) {
      super(message);
    }
  }

  /**
   * An exception indicating that the server is not accepting new connections
   */
  public static final class ServerFullException extends ServerException {
    /**
     * Instantiates a new {@link ServerFullException}
     * @param message the exception message
     */
    private ServerFullException() {
      super(Messages.get(Messages.SERVER_FULL));
    }
  }

  /**
   * An exception indicating that a connection validation has failed
   */
  public static class ConnectionValidationException extends ServerException {
    /**
     * Instantiates a new {@link ConnectionValidationException}
     * @param message the exception message
     */
    public ConnectionValidationException(final String message) {
      super(message);
    }
  }
}
