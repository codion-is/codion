/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server.exception;

/**
 * An exception indicating that a login has failed due to an authentication error,
 * invalid username or password
 */
public final class ServerAuthenticationException extends LoginException {

  /**
   * Instantiates a new {@link ServerAuthenticationException}
   * @param message the exception message
   */
  public ServerAuthenticationException(final String message) {
    super(message);
  }
}
