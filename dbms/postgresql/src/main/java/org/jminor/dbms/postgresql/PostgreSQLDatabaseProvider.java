/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.postgresql;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseProvider;

/**
 * Provides postgresql database implementations
 */
public final class PostgreSQLDatabaseProvider implements DatabaseProvider {

  /** {@inheritDoc} */
  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.POSTGRESQL;
  }

  /** {@inheritDoc} */
  @Override
  public Database createDatabase() {
    return new PostgreSQLDatabase();
  }
}
