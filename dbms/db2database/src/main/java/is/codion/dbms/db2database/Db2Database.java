/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.db2database;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Db2 database implementation.
 */
final class Db2Database extends AbstractDatabase {

  private static final int REFERENTIAL_INTEGRITY_INSERT_UPDATE = -530;
  private static final int REFERENTIAL_INTEGRITY_DELETE_1 = -532;
  private static final int REFERENTIAL_INTEGRITY_DELETE_2 = -536;

  private static final String JDBC_URL_PREFIX = "jdbc:db2:";

  Db2Database(String jdbcUrl) {
    super(jdbcUrl);
  }

  @Override
  public String getName() {
    String name = removeUrlPrefixOptionsAndParameters(getUrl(), JDBC_URL_PREFIX);
    if (name.contains("/")) {
      name = name.substring(name.lastIndexOf('/') + 1);
    }

    return name;
  }

  @Override
  public String getAutoIncrementQuery(String idSource) {
    return "select previous value for " + requireNonNull(idSource, "idSource");
  }

  @Override
  public String getSequenceQuery(String sequenceName) {
    return "select next value for " + requireNonNull(sequenceName, "sequenceName");
  }

  @Override
  public String getSelectForUpdateClause() {
    return "for update";
  }

  @Override
  public String getLimitOffsetClause(Integer limit, Integer offset) {
    return createLimitOffsetClause(limit, offset);
  }

  /**
   * @param exception the exception
   * @return true if this exception is a referential integrity error
   */
  @Override
  public boolean isReferentialIntegrityException(SQLException exception) {
    int errorCode = exception.getErrorCode();

    return errorCode == REFERENTIAL_INTEGRITY_INSERT_UPDATE ||
            errorCode == REFERENTIAL_INTEGRITY_DELETE_1 ||
            errorCode == REFERENTIAL_INTEGRITY_DELETE_2;
  }
}
