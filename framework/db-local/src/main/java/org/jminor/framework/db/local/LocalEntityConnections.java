/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.domain.Entities;

import java.sql.Connection;

/**
 * A factory class providing a LocalEntityConnection instances.
 */
public final class LocalEntityConnections {

  private LocalEntityConnections() {}

  /**
   * Constructs a new EntityConnection instance
   * @param entities the domain model entities
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @return a new LocalEntityConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   */
  public static LocalEntityConnection createConnection(final Entities entities, final Database database, final User user)
          throws DatabaseException {
    return new LocalEntityConnection(entities, database, user, EntityConnection.USE_OPTIMISTIC_LOCKING.get(),
            EntityConnection.LIMIT_FOREIGN_KEY_FETCH_DEPTH.get(), EntityConnection.CONNECTION_VALIDITY_CHECK_TIMEOUT.get());
  }

  /**
   * Constructs a new EntityConnection instance
   * @param entities the domain model entities
   * @param database the Database instance
   * @param connection the connection object to base the entity connection on, it is assumed to be in a valid state
   * @return a new LocalEntityConnection instance, wrapping the given connection
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required but could not be created
   * @see org.jminor.common.db.Database#supportsIsValid()
   */
  public static LocalEntityConnection createConnection(final Entities entities, final Database database, final Connection connection)
          throws DatabaseException {
    return new LocalEntityConnection(entities, database, connection, EntityConnection.USE_OPTIMISTIC_LOCKING.get(),
            EntityConnection.LIMIT_FOREIGN_KEY_FETCH_DEPTH.get(), EntityConnection.CONNECTION_VALIDITY_CHECK_TIMEOUT.get());
  }

  /**
   * @param entities the domain model entities
   * @return A {@link MethodLogger} implementation tailored for LocalEntityConnections
   */
  public static MethodLogger createLogger(final Entities entities) {
    return new MethodLogger(EntityConnection.CONNECTION_LOG_SIZE.get(),
            false, new LocalEntityConnection.EntityArgumentStringProvider(entities));
  }
}
