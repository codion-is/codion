/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;

import java.sql.Connection;

/**
 * A factory class providing EntityConnection instances.
 */
public final class LocalEntityConnections {

  private LocalEntityConnections() {}

  /**
   * Constructs a new EntityConnection instance
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @return a new EntityConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   */
  public static EntityConnection createConnection(final Database database, final User user) throws DatabaseException {
    return new LocalEntityConnection(database, user,
            Configuration.getBooleanValue(Configuration.USE_OPTIMISTIC_LOCKING),
            Configuration.getBooleanValue(Configuration.LIMIT_FOREIGN_KEY_FETCH_DEPTH),
            Configuration.getIntValue(Configuration.CONNECTION_VALIDITY_CHECK_TIMEOUT));
  }

  /**
   * Constructs a new EntityConnection instance
   * @param database the Database instance
   * @param connection the connection object to base the entity connection on, it is assumed to be in a valid state
   * @return a new EntityConnection instance, wrapping the given connection
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required
   * but could not be created
   * @see org.jminor.common.db.Database#supportsIsValid()
   */
  public static EntityConnection createConnection(final Database database, final Connection connection) throws DatabaseException {
    return new LocalEntityConnection(database, connection,
            Configuration.getBooleanValue(Configuration.USE_OPTIMISTIC_LOCKING),
            Configuration.getBooleanValue(Configuration.LIMIT_FOREIGN_KEY_FETCH_DEPTH),
            Configuration.getIntValue(Configuration.CONNECTION_VALIDITY_CHECK_TIMEOUT));
  }

  /**
   * @return A {@link MethodLogger} implementation tailored for EntityConnections
   */
  public static MethodLogger createLogger() {
    return new MethodLogger(Configuration.getIntValue(Configuration.SERVER_CONNECTION_LOG_SIZE),
            false, new LocalEntityConnection.EntityArgumentStringProvider());
  }
}
