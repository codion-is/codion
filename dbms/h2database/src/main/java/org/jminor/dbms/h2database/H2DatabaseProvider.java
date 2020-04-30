/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.h2database;

import org.jminor.common.Text;
import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Provides h2 database implementations
 */
public final class H2DatabaseProvider implements DatabaseProvider {

  private static final String DRIVER_NAME = "org.h2.Driver";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClass").startsWith(DRIVER_NAME);
  }

  @Override
  public String getDatabaseClassName() {
    return H2Database.class.getName();
  }

  @Override
  public H2Database createDatabase() {
    return new H2Database(requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty()),
            Text.parseCommaSeparatedValues(H2Database.DATABASE_INIT_SCRIPT.get()));
  }

  public H2Database createDatabase(final String jdbcUrl, final String initScript) {
    return new H2Database(jdbcUrl, initScript == null ? emptyList() : singletonList(initScript));
  }
}
