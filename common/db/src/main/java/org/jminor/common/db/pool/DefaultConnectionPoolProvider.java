/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.DatabaseConnections;
import org.jminor.common.db.exception.DatabaseException;

/**
 * Provides a default connection pool implementation
 */
public final class DefaultConnectionPoolProvider implements ConnectionPoolProvider {

  /** {@inheritDoc} */
  @Override
  public ConnectionPool createConnectionPool(final User user, final Database database) throws DatabaseException {
    return new DefaultConnectionPool(DatabaseConnections.connectionProvider(database, user, DatabaseConnection.CONNECTION_VALIDITY_CHECK_TIMEOUT.get()));
  }
}
