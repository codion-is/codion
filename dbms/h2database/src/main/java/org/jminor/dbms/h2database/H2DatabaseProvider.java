/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.h2database;

import org.jminor.common.Text;
import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Provides h2 database implementations
 */
public final class H2DatabaseProvider implements DatabaseProvider {

  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.H2;
  }

  @Override
  public H2Database createDatabase() {
    final String jdbcUrl = requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty());
    final List<String> initScripts = Text.parseCommaSeparatedValues(H2Database.DATABASE_INIT_SCRIPT.get());

    return new H2Database(jdbcUrl, initScripts);
  }

  public H2Database createDatabase(final String jdbcUrl, final String initScript) {
    return new H2Database(jdbcUrl, initScript == null ? emptyList() : singletonList(initScript));
  }
}
