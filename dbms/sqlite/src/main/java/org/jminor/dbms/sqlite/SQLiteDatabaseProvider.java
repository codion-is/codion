/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.sqlite;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseProvider;

/**
 * Provides sqlite database implementations
 */
public final class SQLiteDatabaseProvider implements DatabaseProvider {

  /** {@inheritDoc} */
  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.SQLITE;
  }

  /** {@inheritDoc} */
  @Override
  public Database createDatabase() {
    return new SQLiteDatabase();
  }
}
