/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.mariadb;

import org.jminor.common.db.database.AbstractDatabase;

import java.sql.SQLException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the MariaDB database.
 */
public final class MariaDbDatabase extends AbstractDatabase {

  static final String AUTO_INCREMENT_QUERY = "select last_insert_id() from dual";
  private static final String DRIVER_CLASS_NAME = "org.mariadb.jdbc.Driver";
  private static final String URL_PREFIX = "jdbc:mariadb://";
  private static final int REFERENTIAL_CONSTRAINT_ERROR = 1452;
  private static final int UNIQUE_CONSTRAINT_ERROR1 = 1062;
  private static final int UNIQUE_CONSTRAINT_ERROR2 = 1586;

  MariaDbDatabase() {
    super(Type.MARIADB, DRIVER_CLASS_NAME);
  }

  private MariaDbDatabase(final String host, final Integer port, final String dbname) {
    super(Type.MARIADB, DRIVER_CLASS_NAME, requireNonNull(host, "host"),
            requireNonNull(port, "port"), requireNonNull(dbname, "dbname"));
  }

  @Override
  public boolean isEmbedded() {
    return false;
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public String getURL(final Properties connectionProperties) {
    return URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid() + getUrlAppend();
  }

  @Override
  public SelectForUpdateSupport getSelectForUpdateSupport() {
    return SelectForUpdateSupport.FOR_UPDATE;
  }

  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getErrorCode() == REFERENTIAL_CONSTRAINT_ERROR;
  }

  @Override
  public boolean isUniqueConstraintException(final SQLException exception) {
    return exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR1 || exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR2;
  }

  /**
   * Instantiates a new MariaDbDatabase.
   * @param host the host name
   * @param port the port number
   * @param dbname the db name
   * @return a database instance
   */
  public static MariaDbDatabase mariaDbDatabase(final String host, final Integer port, final String dbname) {
    return new MariaDbDatabase(host, port, dbname);
  }
}
