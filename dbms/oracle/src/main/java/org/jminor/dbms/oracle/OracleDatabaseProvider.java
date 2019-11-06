/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.oracle;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseProvider;

/**
 * Provides oracle database implementations
 */
public final class OracleDatabaseProvider implements DatabaseProvider {

  /** {@inheritDoc} */
  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.ORACLE;
  }

  /** {@inheritDoc} */
  @Override
  public Database createDatabase() {
    return new OracleDatabase();
  }
}
