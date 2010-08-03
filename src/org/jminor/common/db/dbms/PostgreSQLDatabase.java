/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.Util;

import java.util.Properties;

/**
 * A Database implementation based on the PostgreSQL database.
 */
public final class PostgreSQLDatabase extends AbstractDatabase {

  static final String DRIVER_NAME = "org.postgresql.Driver";
  static final String URL_PREFIX = "jdbc:postgresql://";
  static final String CHECK_QUERY = "select 1";

  public PostgreSQLDatabase() {
    super(POSTGRESQL);
  }

  public PostgreSQLDatabase(final String host, final String port, final String database) {
    super(POSTGRESQL, host, port, database, false);
  }

  public void loadDriver() throws ClassNotFoundException {
    Class.forName(DRIVER_NAME);
  }

  public String getAutoIncrementValueSQL(final String idSource) {
    return "select currval(" + idSource + ")";
  }

  public String getSequenceSQL(final String sequenceName) {
    return "select nextval(" + sequenceName + ")";
  }

  public String getURL(final Properties connectionProperties) {
    return URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid();
  }

  /**
   * @return false
   */
  @Override
  public boolean supportsIsValid() {
    return false;
  }

  @Override
  public String getCheckConnectionQuery() {
    return CHECK_QUERY;
  }

  @Override
  protected void validate(final String databaseType, final String host, final String port, final String sid, final boolean embedded) {
    Util.require(DATABASE_HOST, host);
    Util.require(DATABASE_PORT, port);
    Util.require(DATABASE_SID, sid);
  }
}
