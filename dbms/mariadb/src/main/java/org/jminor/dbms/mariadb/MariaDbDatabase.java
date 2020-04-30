/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.mariadb;

import org.jminor.common.db.database.AbstractDatabase;

import java.sql.SQLException;

/**
 * A Database implementation based on the MariaDB database.
 */
final class MariaDbDatabase extends AbstractDatabase {

  static final String AUTO_INCREMENT_QUERY = "select last_insert_id() from dual";
  private static final int REFERENTIAL_CONSTRAINT_ERROR = 1452;
  private static final int UNIQUE_CONSTRAINT_ERROR1 = 1062;
  private static final int UNIQUE_CONSTRAINT_ERROR2 = 1586;

  MariaDbDatabase(final String jdbUrl) {
    super(jdbUrl);
  }

  @Override
  public String getName() {
    return getURL();
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public SelectForUpdateSupport getSelectForUpdateSupport() {
    return SelectForUpdateSupport.FOR_UPDATE;
  }

  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getErrorCode() == REFERENTIAL_CONSTRAINT_ERROR;
  }

  @Override
  public boolean isUniqueConstraintException(final SQLException exception) {
    return exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR1 || exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR2;
  }
}
