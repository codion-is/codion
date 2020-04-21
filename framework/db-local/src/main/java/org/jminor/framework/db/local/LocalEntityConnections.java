/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.db.connection.DatabaseConnection;
import org.jminor.common.db.database.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;
import org.jminor.framework.domain.Domain;

import java.sql.Connection;

/**
 * A factory class for creating LocalEntityConnection instances.
 */
public final class LocalEntityConnections {

  private LocalEntityConnections() {}

  /**
   * Constructs a new LocalEntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @return a new LocalEntityConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws org.jminor.common.db.exception.AuthenticationException in case of an authentication error
   */
  public static LocalEntityConnection createConnection(final Domain domain, final Database database,
                                                       final User user) throws DatabaseException {
    return new DefaultLocalEntityConnection(domain, database, user,
            DatabaseConnection.CONNECTION_VALIDITY_CHECK_TIMEOUT.get())
            .setOptimisticLockingEnabled(LocalEntityConnection.USE_OPTIMISTIC_LOCKING.get())
            .setLimitForeignKeyFetchDepth(LocalEntityConnection.LIMIT_FOREIGN_KEY_FETCH_DEPTH.get());
  }

  /**
   * Constructs a new LocalEntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param connection the connection object to base the entity connection on, it is assumed to be in a valid state
   * @return a new LocalEntityConnection instance, wrapping the given connection
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required but could not be created
   * @see Database#supportsIsValid()
   */
  public static LocalEntityConnection createConnection(final Domain domain, final Database database,
                                                       final Connection connection) throws DatabaseException {
    return new DefaultLocalEntityConnection(domain, database, connection,
            DatabaseConnection.CONNECTION_VALIDITY_CHECK_TIMEOUT.get())
            .setOptimisticLockingEnabled(LocalEntityConnection.USE_OPTIMISTIC_LOCKING.get())
            .setLimitForeignKeyFetchDepth(LocalEntityConnection.LIMIT_FOREIGN_KEY_FETCH_DEPTH.get());
  }
}
