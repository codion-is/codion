/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.postgresql;

import org.jminor.common.db.database.AbstractDatabase;

import java.sql.SQLException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the PostgreSQL database.
 */
public final class PostgreSQLDatabase extends AbstractDatabase {

  private static final String INVALID_AUTHORIZATION_SPECIFICATION = "28000";
  private static final String INTEGRITY_CONSTRAINT_VIOLATION = "23000";
  private static final String UNIQUE_CONSTRAINT_ERROR = "23505";

  static final String DRIVER_CLASS_NAME = "org.postgresql.Driver";
  static final String URL_PREFIX = "jdbc:postgresql://";
  static final String CHECK_QUERY = "select 1";

  PostgreSQLDatabase() {
    super(Type.POSTGRESQL, DRIVER_CLASS_NAME);
  }

  private PostgreSQLDatabase(final String host, final Integer port, final String database) {
    super(Type.POSTGRESQL, DRIVER_CLASS_NAME, requireNonNull(host, "host"),
            requireNonNull(port, "port"), requireNonNull(database, "database"));
  }

  @Override
  public boolean isEmbedded() {
    return false;
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
  public String getURL(final Properties connectionProperties) {
    return URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid() + getUrlAppend();
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

  /**
   * Instantiates a new PostgreSQLDatabase.
   * @param host the host name
   * @param port the port number
   * @param database the database name
   * @return a database instance
   */
  public static PostgreSQLDatabase postgreSqlDatabase(final String host, final Integer port, final String database) {
    return new PostgreSQLDatabase(host, port, database);
  }
}
