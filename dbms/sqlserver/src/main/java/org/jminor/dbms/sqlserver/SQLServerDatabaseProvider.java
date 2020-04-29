/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.sqlserver;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

import static java.util.Objects.requireNonNull;

/**
 * Provides sql server database implementations
 */
public final class SQLServerDatabaseProvider implements DatabaseProvider {

  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.SQLSERVER;
  }

  @Override
  public Database createDatabase() {
    final String jdbcUrl = requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty());

    return new SQLServerDatabase(jdbcUrl);
  }
}
