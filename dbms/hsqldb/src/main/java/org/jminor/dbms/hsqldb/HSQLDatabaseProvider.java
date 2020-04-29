/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.hsqldb;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

import static java.util.Objects.requireNonNull;

/**
 * Provides hsql database implementations
 */
public final class HSQLDatabaseProvider implements DatabaseProvider {

  @Override
  public boolean isCompatibleWith(final String driverClass) {
    return driverClass.startsWith("org.hsqldb");
  }

  @Override
  public Database createDatabase() {
    final String jdbcUrl = requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty());

    return new HSQLDatabase(jdbcUrl);
  }
}
