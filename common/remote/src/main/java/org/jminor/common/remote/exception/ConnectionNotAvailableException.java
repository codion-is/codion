/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.exception;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * An exception indicating that the server is not accepting new connections
 */
public final class ConnectionNotAvailableException extends ServerException {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ConnectionNotAvailableException.class.getName(), Locale.getDefault());

  /**
   * Instantiates a new {@link ConnectionNotAvailableException}
   * @param message the exception message
   */
  public ConnectionNotAvailableException() {
    super(MESSAGES.getString("connection_not_available"));
  }
}
