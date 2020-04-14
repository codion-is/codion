/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server.exception;

/**
 * An exception indicating that a login has failed
 */
public class LoginException extends ServerException {

  /**
   * Instantiates a new {@link LoginException}
   * @param message the exception message
   */
  public LoginException(final String message) {
    super(message);
  }
}
