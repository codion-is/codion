/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.exception;

/**
 * An exception indication an authentication failure
 */
public final class AuthenticationException extends DatabaseException {

  /**
   * @param message the error message
   */
  public AuthenticationException(final String message) {
    super(message);
  }
}
