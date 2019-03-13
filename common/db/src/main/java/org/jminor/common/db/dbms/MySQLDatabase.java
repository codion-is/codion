/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.AbstractDatabase;

import java.util.Objects;
import java.util.Properties;

/**
 * A Database implementation based on the MySQL database.
 */
public final class MySQLDatabase extends AbstractDatabase {

  static final String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
  static final String AUTO_INCREMENT_QUERY = "select last_insert_id() from dual";
  static final String URL_PREFIX = "jdbc:mysql://";

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
    super(Type.MYSQL, DRIVER_CLASS_NAME, Objects.requireNonNull(host, "host"),
            Objects.requireNonNull(port, "port"), Objects.requireNonNull(dbname, "dbname"));
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    return URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid();
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsNowait() {
    return false;
  }
}
