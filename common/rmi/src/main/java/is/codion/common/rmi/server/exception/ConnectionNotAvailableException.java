/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server.exception;

import java.util.ResourceBundle;

/**
 * An exception indicating that the server is not accepting new connections
 */
public final class ConnectionNotAvailableException extends ServerException {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ConnectionNotAvailableException.class.getName());

  /**
   * Instantiates a new {@link ConnectionNotAvailableException}
   */
  public ConnectionNotAvailableException() {
    super(MESSAGES.getString("connection_not_available"));
  }
}
