/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.sqlserver;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseProvider;

/**
 * Provides sql server database implementations
 */
public final class SQLServerDatabaseProvider implements DatabaseProvider {

  /** {@inheritDoc} */
  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.SQLSERVER;
  }

  /** {@inheritDoc} */
  @Override
  public Database createDatabase() {
    return new SQLServerDatabase();
  }
}
