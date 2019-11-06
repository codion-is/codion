/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.derby;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseProvider;

/**
 * Provides derby database implementations
 */
public final class DerbyDatabaseProvider implements DatabaseProvider {

  /** {@inheritDoc} */
  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.DERBY;
  }

  /** {@inheritDoc} */
  @Override
  public Database createDatabase() {
    return new DerbyDatabase();
  }
}
