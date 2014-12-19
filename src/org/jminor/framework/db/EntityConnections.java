/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Database;
import org.jminor.common.db.DefaultDatabaseConnection;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;
import org.jminor.common.model.tools.MethodLogger;

import java.sql.Connection;

/**
 * A factory class providing EntityConnection instances.
 */
public final class EntityConnections {

  private EntityConnections() {}

  /**
   * Constructs a new EntityConnection instance
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @return a new EntityConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   */
  public static EntityConnection createConnection(final Database database, final User user) throws DatabaseException {
    return new DefaultEntityConnection(database, user);
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
    return new DefaultEntityConnection(database, connection);
  }

  /**
   * @return A {@link MethodLogger} implementation tailored for EntityConnections
   */
  public static MethodLogger createLogger() {
    return new DefaultEntityConnection.Logger();
  }

  /**
   * Sets the internal connection for the given EntityConnection, note that no checking
   * of validity or open transactions is performed on the given connection, it is used 'as is'
   * @param entityConnection the entity connection for which to set the internal connection
   * @param connection the internal connection to use
   */
  public static void setConnection(final EntityConnection entityConnection, final Connection connection) {
    ((DefaultDatabaseConnection) entityConnection.getDatabaseConnection()).setConnection(connection);
  }
}
