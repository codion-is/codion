/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

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
