/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * A Database implementation based on the MySQL database.
 */
public class MySQLDatabase extends AbstractDatabase {

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat("dd-MM-yyyy");
    }
  };
  private static final ThreadLocal timestampFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat("dd-MM-yyyy HH:mm");
    }
  };

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
  public String getSQLDateString(final Date value, final boolean isTimestamp) {
    return isTimestamp ?
            "str_to_date('" + ((DateFormat) timestampFormat.get()).format(value) + "', '%d-%m-%Y %H:%i')" :
            "str_to_date('" + ((DateFormat) dateFormat.get()).format(value) + "', '%d-%m-%Y')";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String host = getHost();
    if (host == null || host.length() == 0)
      throw new RuntimeException(DATABASE_HOST + " is required for database type " + getDatabaseType());
    final String port = getPort();
    if (port == null || port.length() == 0)
      throw new RuntimeException(DATABASE_PORT + " is required for database type " + getDatabaseType());
    final String sid = getSid();
    if (sid == null || sid.length() == 0)
      throw new RuntimeException(DATABASE_SID + " is required for database type " + getDatabaseType());

    return "jdbc:mysql://" + host + ":" + port + "/" + sid;
  }

  /** {@inheritDoc} */
  public String getAuthenticationInfo(final Properties connectionProperties) {
    return null;
  }

  /** {@inheritDoc} */
  public void shutdownEmbedded(final Properties connectionProperties) {}

  /** {@inheritDoc} */
  public boolean supportsIsValid() {
    return true;
  }

  /** {@inheritDoc} */
  public String getErrorMessage(final SQLException exception) {
    return exception.getMessage();
  }
}
