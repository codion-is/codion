/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.AbstractDatabase;

import java.util.Properties;

/**
 * A Database implementation based on the MySQL database.
 */
public final class MySQLDatabase extends AbstractDatabase {

  static final String DRIVER_NAME = "com.mysql.jdbc.Driver";
  static final String AUTO_INCREMENT_QUERY = "select last_insert_id() from dual";
  static final String URL_PREFIX = "jdbc:mysql://";

  /**
   * Instantiates a new MySQLDatabase.
   */
  public MySQLDatabase() {
    super(MYSQL);
  }

  /**
   * Instantiates a new MySQLDatabase.
   * @param host the host name
   * @param port the port number
   * @param dbname the db name
   */
  public MySQLDatabase(final String host, final String port, final String dbname) {
    super(MYSQL, host, port, dbname);
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName(DRIVER_NAME);
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    return URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid();
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsNowait() {
    return false;
  }
}
