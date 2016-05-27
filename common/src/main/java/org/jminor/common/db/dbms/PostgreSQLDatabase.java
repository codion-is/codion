/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.Util;
import org.jminor.common.db.AbstractDatabase;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * A Database implementation based on the PostgreSQL database.
 */
public final class PostgreSQLDatabase extends AbstractDatabase {

  private static final String INVALID_AUTHORIZATION_SPECIFICATION = "28000";

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
  public PostgreSQLDatabase(final String host, final String port, final String database) {
    super(Type.POSTGRESQL, DRIVER_CLASS_NAME, host, port, database, false);
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementValueSQL(final String idSource) {
    Objects.requireNonNull(idSource, "idSource");
    return "select currval('" + idSource + "')";
  }

  /** {@inheritDoc} */
  @Override
  public String getSequenceSQL(final String sequenceName) {
    Objects.requireNonNull(sequenceName, "sequenceName");
    return "select nextval('" + sequenceName + "')";
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    Util.require("host", getHost());
    Util.require("port", getPort());
    Util.require("sid", getSid());
    return URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid();
  }

  /**
   * @param exception the exception
   * @return true if this exception represents a login credentials failure
   */
  @Override
  public boolean isAuthenticationException(final SQLException exception) {
    return exception.getSQLState().equals(INVALID_AUTHORIZATION_SPECIFICATION);
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
