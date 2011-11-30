/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.i18n.Messages;

/**
 * An exception originating from a remote server
 */
public class ServerException extends Exception {

  private ServerException() {
    super();
  }

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
   * An exception indicating that a login has failed
   */
  public static final class LoginException extends ServerException {
    private LoginException(final String message) {
      super(message);
    }
  }

  /**
   * An exception indicating that the server is not accepting new connections
   */
  public static final class ServerFullException extends ServerException {
    private ServerFullException() {
      super(Messages.get(Messages.SERVER_FULL));
    }
  }
}
