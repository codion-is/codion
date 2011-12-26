/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;

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
   * @throws ClassNotFoundException in case the JDBC driver class is not found
   */
  public static EntityConnection createConnection(final Database database, final User user) throws DatabaseException, ClassNotFoundException {
    return new EntityConnectionImpl(database, user);
  }

  /**
   * Constructs a new EntityConnection instance
   * @param database the Database instance
   * @param connection the connection object to base the entity connection on
   * @return a new EntityConnection instance, wrapping the given connection
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required
   * but could not be created
   * @see org.jminor.common.db.Database#supportsIsValid()
   */
  public static EntityConnection createConnection(final Database database, final Connection connection) throws DatabaseException {
    return new EntityConnectionImpl(database, connection);
  }
}
