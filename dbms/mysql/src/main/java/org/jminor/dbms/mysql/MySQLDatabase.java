/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.mysql;

import org.jminor.common.db.AbstractDatabase;

import java.sql.SQLException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the MySQL database.
 */
public final class MySQLDatabase extends AbstractDatabase {

  static final String AUTO_INCREMENT_QUERY = "select last_insert_id() from dual";
  private static final String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
  private static final String URL_PREFIX = "jdbc:mysql://";
  private static final int REFERENTIAL_CONSTRAINT_ERROR = 1452;
  private static final int UNIQUE_CONSTRAINT_ERROR1 = 1062;
  private static final int UNIQUE_CONSTRAINT_ERROR2 = 1586;

  /**
   * Instantiates a new MySQLDatabase.
   */
  public MySQLDatabase() {
    super(Type.MYSQL, DRIVER_CLASS_NAME);
  }

  /**
   * Instantiates a new MySQLDatabase.
   * @param host the host name
   * @param port the port number
   * @param dbname the db name
   */
  public MySQLDatabase(final String host, final Integer port, final String dbname) {
    super(Type.MYSQL, DRIVER_CLASS_NAME, requireNonNull(host, "host"),
            requireNonNull(port, "port"), requireNonNull(dbname, "dbname"));
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    return URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid() + getUrlAppend();
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsNowait() {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getErrorCode() == REFERENTIAL_CONSTRAINT_ERROR;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isUniqueConstraintException(final SQLException exception) {
    return exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR1 || exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR2;
  }
}
