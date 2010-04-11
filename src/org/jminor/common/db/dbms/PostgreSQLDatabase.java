/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * A Database implementation based on the PostgreSQL database.
 */
public class PostgreSQLDatabase extends AbstractDatabase {

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

  public PostgreSQLDatabase() {
    super(POSTGRESQL);
  }

  public PostgreSQLDatabase(final String host, final String port, final String database) {
    super(POSTGRESQL, host, port, database, false);
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName("org.postgresql.Driver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "select currval(" + idSource + ")";
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    return "select nextval(" + sequenceName + ")";
  }

  /** {@inheritDoc} */
  public String getSQLDateString(final Date value, final boolean isTimestamp) {
    return isTimestamp ?
            "to_date('" + ((DateFormat) timestampFormat.get()).format(value) + "', 'DD-MM-YYYY HH24:MI')" :
            "to_date('" + ((DateFormat) dateFormat.get()).format(value) + "', 'DD-MM-YYYY')";
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

    return "jdbc:postgresql://" + host + ":" + port + "/" + sid;
  }

  /** {@inheritDoc} */
  public boolean supportsIsValid() {
    return false;
  }

  /** {@inheritDoc} */
  public String getCheckConnectionQuery() {
    return "select 1";
  }
}
