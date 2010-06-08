/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import java.util.Properties;

/**
 * A Database implementation based on the MySQL database.
 */
public class MySQLDatabase extends AbstractDatabase {

  public MySQLDatabase() {
    super(MYSQL);
  }

  public MySQLDatabase(final String host, final String port, final String dbname) {
    super(MYSQL, host, port, dbname);
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName("com.mysql.jdbc.Driver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "select last_insert_id() from dual";
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    throw new RuntimeException("Sequence support is not implemented for database type: " + getDatabaseType());
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    return "jdbc:mysql://" + getHost() + ":" + getPort() + "/" + getSid();
  }

  @Override
  protected void validate(final String databaseType, final String host, final String port, final String sid, final boolean embedded) {
    require(DATABASE_HOST, host);
    require(DATABASE_PORT, port);
    require(DATABASE_SID, sid);
  }
}
