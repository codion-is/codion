/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;

/**
 * Provides connection pool implementations
 */
public interface ConnectionPoolProvider {

  /**
   * @param user the user to base the pooled connections on
   * @param database the underlying database
   * @return a connection pool based on the given user
   * @throws DatabaseException in case of an exception
   */
  ConnectionPool createConnectionPool(final User user, final Database database) throws DatabaseException;
}
