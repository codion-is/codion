/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.AbstractDatabase;
import org.jminor.common.model.Util;

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
    super(MYSQL, DRIVER_CLASS_NAME);
  }

  /**
   * Instantiates a new MySQLDatabase.
   * @param host the host name
   * @param port the port number
   * @param dbname the db name
   */
  public MySQLDatabase(final String host, final String port, final String dbname) {
    super(MYSQL, DRIVER_CLASS_NAME, host, port, dbname);
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementValueSQL(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    Util.require("host", getHost());
    Util.require("port", getPort());
    Util.require("sid", getSid());
    return URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid();
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsNowait() {
    return false;
  }
}
