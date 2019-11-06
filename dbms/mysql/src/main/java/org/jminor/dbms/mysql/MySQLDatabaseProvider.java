/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.mysql;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseProvider;

/**
 * Provides mysql database implementations
 */
public final class MySQLDatabaseProvider implements DatabaseProvider {

  /** {@inheritDoc} */
  @Override
  public Database.Type getDatabaseType() {
    return Database.Type.MYSQL;
  }

  /** {@inheritDoc} */
  @Override
  public Database createDatabase() {
    return new MySQLDatabase();
  }
}
