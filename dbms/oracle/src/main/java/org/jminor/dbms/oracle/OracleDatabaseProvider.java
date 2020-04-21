/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.oracle;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

/**
 * Provides oracle database implementations
 */
public final class OracleDatabaseProvider implements DatabaseProvider {

  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.ORACLE;
  }

  @Override
  public Database createDatabase() {
    return new OracleDatabase();
  }
}
