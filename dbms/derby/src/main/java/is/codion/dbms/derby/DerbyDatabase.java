/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.derby;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.DriverManager;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the Derby database.
 */
final class DerbyDatabase extends AbstractDatabase {

  private static final String SHUTDOWN_ERROR_CODE = "08006";
  private static final String TIMEOUT_ERROR_CODE = "XCL52";
  private static final int FOREIGN_KEY_ERROR = 23503;

  private static final String JDBC_URL_PREFIX_TCP = "jdbc:derby://";
  private static final String JDBC_URL_PREFIX_FILE = "jdbc:derby:";

  static final String AUTO_INCREMENT_QUERY = "select IDENTITY_VAL_LOCAL() from ";

  DerbyDatabase(String jdbcUrl) {
    super(jdbcUrl);
  }

  @Override
  public String getName() {
    String name = getUrl();
    boolean tcp = name.startsWith(JDBC_URL_PREFIX_TCP);
    name = removeUrlPrefixOptionsAndParameters(name, JDBC_URL_PREFIX_TCP, JDBC_URL_PREFIX_FILE);
    if (tcp && name.contains("/")) {
      name = name.substring(name.indexOf('/') + 1);
    }

    return name;
  }

  @Override
  public String getSelectForUpdateClause() {
    return "for update";
  }

  @Override
  public String getLimitOffsetClause(Integer limit, Integer offset) {
    return createOffsetFetchNextClause(limit, offset);
  }

  @Override
  public String getAutoIncrementQuery(String idSource) {
    return AUTO_INCREMENT_QUERY + requireNonNull(idSource, "idSource");
  }

  @Override
  public boolean isReferentialIntegrityException(SQLException exception) {
    return exception.getErrorCode() == FOREIGN_KEY_ERROR;
  }

  @Override
  public boolean isTimeoutException(SQLException exception) {
    return TIMEOUT_ERROR_CODE.equals(exception.getSQLState());
  }

  @Override
  public void shutdownEmbedded() {
    try {
      DriverManager.getConnection(getUrl() + ";shutdown=true").close();
    }
    catch (SQLException e) {
      if (!e.getSQLState().equals(SHUTDOWN_ERROR_CODE)) {//08006 is expected on Derby shutdown
        System.err.println("Embedded Derby database did not successfully shut down: " + e.getMessage());
      }
    }
  }
}
