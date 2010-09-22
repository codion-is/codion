/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Database;
import org.jminor.common.model.User;

import java.sql.Connection;
import java.sql.SQLException;

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
   * @throws java.sql.SQLException in case there is a problem connecting to the database
   * @throws ClassNotFoundException in case the JDBC driver class is not found
   */
  public static EntityConnection createConnection(final Database database, final User user) throws SQLException, ClassNotFoundException {
    return new EntityConnectionImpl(database, user);
  }

  /**
   * Constructs a new EntityConnection instance
   * @param connection the connection object to base the entity db connection on
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @return a new EntityConnection instance
   * @throws SQLException in case there is a problem connecting to the database
   */
  public static EntityConnection createConnection(final Connection connection, final Database database, final User user) throws SQLException {
    return new EntityConnectionImpl(connection, database, user);
  }
}
