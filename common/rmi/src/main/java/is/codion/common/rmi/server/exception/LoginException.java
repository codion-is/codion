/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server.exception;

/**
 * An exception indicating that a login has failed
 */
public class LoginException extends ServerException {

  /**
   * Instantiates a new {@link LoginException}
   * @param message the exception message
   */
  public LoginException(String message) {
    super(message);
  }
}
