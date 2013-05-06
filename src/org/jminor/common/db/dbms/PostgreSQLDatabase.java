/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.AbstractDatabase;
import org.jminor.common.model.Util;

import java.util.Properties;

/**
 * A Database implementation based on the PostgreSQL database.
 */
public final class PostgreSQLDatabase extends AbstractDatabase {

  static final String DRIVER_CLASS_NAME = "org.postgresql.Driver";
  static final String URL_PREFIX = "jdbc:postgresql://";
  static final String CHECK_QUERY = "select 1";

  /**
   * Instantiates a new PostgreSQLDatabase.
   */
  public PostgreSQLDatabase() {
    super(POSTGRESQL, DRIVER_CLASS_NAME);
  }

  /**
   * Instantiates a new PostgreSQLDatabase.
   * @param host the host name
   * @param port the port number
   * @param database the database name
   */
  public PostgreSQLDatabase(final String host, final String port, final String database) {
    super(POSTGRESQL, DRIVER_CLASS_NAME, host, port, database, false);
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementValueSQL(final String idSource) {
    Util.rejectNullValue(idSource, "idSource");
    return "select currval('" + idSource + "')";
  }

  /** {@inheritDoc} */
  @Override
  public String getSequenceSQL(final String sequenceName) {
    Util.rejectNullValue(sequenceName, "sequenceName");
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
