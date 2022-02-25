/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server.exception;

/**
 * An exception indicating that a connection validation has failed
 */
public final class ConnectionValidationException extends ServerException {

  /**
   * Instantiates a new {@link ConnectionValidationException}
   * @param message the exception message
   */
  public ConnectionValidationException(String message) {
    super(message);
  }
}
