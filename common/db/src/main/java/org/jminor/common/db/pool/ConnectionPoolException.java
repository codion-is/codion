/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.exception.DatabaseException;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * An exception originating from a ConnectionPool
 */
public class ConnectionPoolException extends DatabaseException {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ConnectionPoolException.class.getName(), Locale.getDefault());

  ConnectionPoolException(final String reason) {
    super(reason);
  }

  /**
   * An exception indicating that no connection is available in the connection pool
   */
  public static final class NoConnectionAvailable extends ConnectionPoolException {

    private static final String MESSAGE = MESSAGES.getString("no_connection_available");

    /**
     * Instantiates a new NoConnectionAvailable exception
     * @param retries the number retries trying to check out a connection
     * @param checkoutTime the time used trying to check out a connection
     */
    public NoConnectionAvailable(final int retries, final long checkoutTime) {
      super(MESSAGE + " [" + retries + ", " + checkoutTime + "]");
    }
  }
}
