/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.mariadb;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

/**
 * Provides mariadb database implementations
 */
public final class MariaDbDatabaseProvider implements DatabaseProvider {

  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.MARIADB;
  }

  @Override
  public Database createDatabase() {
    return new MariaDbDatabase();
  }
}
