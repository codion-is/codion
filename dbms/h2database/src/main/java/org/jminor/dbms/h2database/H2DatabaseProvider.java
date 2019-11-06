/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.h2database;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseProvider;

/**
 * Provides h2 database implementations
 */
public final class H2DatabaseProvider implements DatabaseProvider {

  /** {@inheritDoc} */
  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.H2;
  }

  /** {@inheritDoc} */
  @Override
  public Database createDatabase() {
    return new H2Database();
  }
}
