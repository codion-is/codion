/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.derby;

import org.jminor.common.db.database.AbstractDatabase;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the Derby database.
 */
final class DerbyDatabase extends AbstractDatabase {

  private static final int FOREIGN_KEY_ERROR = 23503;

  private static final String JDBC_URL_PREFIX_TCP = "jdbc:derby://";
  private static final String JDBC_URL_PREFIX_FILE = "jdbc:derby:";

  static final String AUTO_INCREMENT_QUERY = "select IDENTITY_VAL_LOCAL() from ";

  DerbyDatabase(final String jdbcUrl) {
    super(jdbcUrl);
  }

  @Override
  public String getName() {
    String name = getURL();
    if (name.toLowerCase().startsWith(JDBC_URL_PREFIX_TCP)) {
      name = name.substring(JDBC_URL_PREFIX_TCP.length());
    }
    if (name.toLowerCase().startsWith(JDBC_URL_PREFIX_FILE)) {
      name = name.substring(JDBC_URL_PREFIX_FILE.length());
    }
    if (name.contains(";")) {
      name = name.substring(0, name.indexOf(";"));
    }

    return name;
  }

  @Override
  public SelectForUpdateSupport getSelectForUpdateSupport() {
    return SelectForUpdateSupport.FOR_UPDATE;
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY + requireNonNull(idSource, "idSource");
  }

  /**
   * @param exception the exception
   * @return true if this exception is a referential integrity error
   */
  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getErrorCode() == FOREIGN_KEY_ERROR;
  }
}
