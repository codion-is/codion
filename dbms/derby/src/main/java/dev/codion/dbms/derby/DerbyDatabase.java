/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.derby;

import org.jminor.common.db.database.AbstractDatabase;

import java.sql.DriverManager;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the Derby database.
 */
final class DerbyDatabase extends AbstractDatabase {

  private static final String SHUTDOWN_ERROR_CODE = "08006";
  private static final int FOREIGN_KEY_ERROR = 23503;

  private static final String JDBC_URL_PREFIX_TCP = "jdbc:derby://";
  private static final String JDBC_URL_PREFIX_FILE = "jdbc:derby:";

  static final String AUTO_INCREMENT_QUERY = "select IDENTITY_VAL_LOCAL() from ";

  DerbyDatabase(final String jdbcUrl) {
    super(jdbcUrl);
  }

  @Override
  public String getName() {
    String name = getUrl();
    final boolean tcp = name.startsWith(JDBC_URL_PREFIX_TCP);
    name = removeUrlPrefixAndOptions(name, JDBC_URL_PREFIX_TCP, JDBC_URL_PREFIX_FILE);
    if (tcp && name.contains("/")) {
      name = name.substring(name.indexOf('/') + 1);
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

  @Override
  public void shutdownEmbedded() {
    try {
      DriverManager.getConnection(getUrl() + ";shutdown=true").close();
    }
    catch (final SQLException e) {
      if (!e.getSQLState().equals(SHUTDOWN_ERROR_CODE)) {//08006 is expected on Derby shutdown
        System.err.println("Embedded Derby database did not successfully shut down: " + e.getMessage());
      }
    }
  }
}
