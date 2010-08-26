/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.i18n.Messages;

import java.sql.SQLException;

/**
 * An exception originating from a ConnectionPool
 */
public class ConnectionPoolException extends SQLException {

  ConnectionPoolException(final String reason) {
    super(reason);
  }

  /**
   * An exception indicating that no connection is available in the connection pool
   */
  public static final class NoConnectionAvailable extends ConnectionPoolException {

    /**
     * Instantates a new NoConnectionAvailable exception
     */
    public NoConnectionAvailable() {
      super(Messages.get(Messages.NO_CONNECTION_AVAILABLE));
    }
  }
}
