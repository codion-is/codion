/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.exception;

/**
 * An exception indicating that a connection validation has failed
 */
public final class ConnectionValidationException extends ServerException {

  /**
   * Instantiates a new {@link ConnectionValidationException}
   * @param message the exception message
   */
  public ConnectionValidationException(final String message) {
    super(message);
  }
}
