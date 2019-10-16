/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.postgresql;

import org.jminor.common.db.AbstractDatabase;

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

  /**
   * Instantiates a new PostgreSQLDatabase.
   */
  public PostgreSQLDatabase() {
    super(Type.POSTGRESQL, DRIVER_CLASS_NAME);
  }

  /**
   * Instantiates a new PostgreSQLDatabase.
   * @param host the host name
   * @param port the port number
   * @param database the database name
   */
  public PostgreSQLDatabase(final String host, final Integer port, final String database) {
    super(Type.POSTGRESQL, DRIVER_CLASS_NAME, requireNonNull(host, "host"),
            requireNonNull(port, "port"), requireNonNull(database, "database"), false);
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return "select currval('" + requireNonNull(idSource, "idSource") + "')";
  }

  /** {@inheritDoc} */
  @Override
  public String getSequenceQuery(final String sequenceName) {
    return "select nextval('" + requireNonNull(sequenceName, "sequenceName") + "')";
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    return URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid() + getUrlAppend();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isAuthenticationException(final SQLException exception) {
    return exception.getSQLState().equals(INVALID_AUTHORIZATION_SPECIFICATION);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getSQLState().equals(INTEGRITY_CONSTRAINT_VIOLATION);
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public String getCheckConnectionQuery() {
    return CHECK_QUERY;
  }
}
