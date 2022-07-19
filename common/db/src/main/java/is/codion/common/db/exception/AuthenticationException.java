/*
 * Copyright (c) 2015 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

/**
 * An exception indication an authentication failure
 */
public final class AuthenticationException extends DatabaseException {

  /**
   * @param message the error message
   */
  public AuthenticationException(String message) {
    super(message);
  }
}
