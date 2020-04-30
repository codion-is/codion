/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.postgresql;

import org.jminor.common.db.database.AbstractDatabase;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the PostgreSQL database.
 */
final class PostgreSQLDatabase extends AbstractDatabase {

  private static final String INVALID_AUTHORIZATION_SPECIFICATION = "28000";
  private static final String INTEGRITY_CONSTRAINT_VIOLATION = "23000";
  private static final String UNIQUE_CONSTRAINT_ERROR = "23505";

  private static final String JDBC_URL_PREFIX = "jdbc:postgresql://";

  static final String CHECK_QUERY = "select 1";

  PostgreSQLDatabase(final String jdbcUrl) {
    super(jdbcUrl);
  }

  @Override
  public String getName() {
    String name = getURL();
    if (name.toLowerCase().startsWith(JDBC_URL_PREFIX)) {
      name = name.substring(JDBC_URL_PREFIX.length());
    }
    if (name.contains(";")) {
      name = name.substring(0, name.indexOf(";"));
    }

    return name;
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return "select currval('" + requireNonNull(idSource, "idSource") + "')";
  }

  @Override
  public String getSequenceQuery(final String sequenceName) {
    return "select nextval('" + requireNonNull(sequenceName, "sequenceName") + "')";
  }

  @Override
  public boolean isAuthenticationException(final SQLException exception) {
    return exception.getSQLState().equals(INVALID_AUTHORIZATION_SPECIFICATION);
  }

  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getSQLState().equals(INTEGRITY_CONSTRAINT_VIOLATION);
  }

  @Override
  public boolean isUniqueConstraintException(final SQLException exception) {
    return exception.getSQLState().equals(UNIQUE_CONSTRAINT_ERROR);
  }

  /**
   * @return false
   */
  @Override
  public boolean supportsIsValid() {
    return false;
  }

  /**
   * @return true
   */
  @Override
  public boolean subqueryRequiresAlias() {
    return true;
  }

  @Override
  public String getCheckConnectionQuery() {
    return CHECK_QUERY;
  }
}
