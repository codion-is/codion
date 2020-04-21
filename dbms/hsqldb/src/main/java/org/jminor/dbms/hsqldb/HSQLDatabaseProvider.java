/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.hsqldb;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

/**
 * Provides hsql database implementations
 */
public final class HSQLDatabaseProvider implements DatabaseProvider {

  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.HSQL;
  }

  @Override
  public Database createDatabase() {
    return new HSQLDatabase();
  }
}
