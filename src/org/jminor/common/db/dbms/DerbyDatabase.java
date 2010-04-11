/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * A Database implementation based on the Derby database.
 */
public class DerbyDatabase extends AbstractDatabase {

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd");
    }
  };
  private static final ThreadLocal timestampFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
  };

  public DerbyDatabase() {
    super(DERBY);
  }

  public DerbyDatabase(final String databaseName) {
    super(DERBY, databaseName, null, null, true);
  }

  public DerbyDatabase(final String host, final String port, final String sid) {
    super(DERBY, host, port, sid, false);
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName(isEmbedded() ? "org.apache.derby.jdbc.EmbeddedDriver" : "org.apache.derby.jdbc.ClientDriver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "select IDENTITY_VAL_LOCAL() from " + idSource;
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    throw new RuntimeException("Sequence support is not implemented for database type: " + getDatabaseType());
  }

  /** {@inheritDoc} */
  public String getSQLDateString(final Date value, final boolean isTimestamp) {
    return "DATE('" + (isTimestamp ? ((DateFormat) timestampFormat.get()).format(value) : ((DateFormat) dateFormat.get()).format(value)) + "')";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      final String host = getHost();
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST + " is required for database type " + getDatabaseType());

      return "jdbc:derby:" + host + (authentication == null ? "" : ";" + authentication);
    }
    else {
      final String host = getHost();
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST + " is required for database type " + getDatabaseType());
      final String port = getPort();
      if (port == null || port.length() == 0)
        throw new RuntimeException(DATABASE_PORT + " is required for database type " + getDatabaseType());
      final String sid = getSid();
      if (sid == null || sid.length() == 0)
        throw new RuntimeException(DATABASE_SID + " is required for database type " + getDatabaseType());

      return "jdbc:derby://" + host + ":" + port + "/" + sid + (authentication == null ? "" : ";" + authentication);
    }
  }

  /** {@inheritDoc} */
  public String getAuthenticationInfo(final Properties connectionProperties) {
    if (connectionProperties != null) {
      final String username = (String) connectionProperties.get("user");
      final String password = (String) connectionProperties.get("password");
      if (username != null && username.length() > 0 && password != null && password.length() > 0)
        return "user=" + username + ";" + "password=" + password;
    }

    return null;
  }

  /** {@inheritDoc} */
  public void shutdownEmbedded(final Properties connectionProperties) {
    try {
      final String authentication = getAuthenticationInfo(connectionProperties);
      DriverManager.getConnection("jdbc:derby:" + getHost() + ";shutdown=true"
               + (authentication == null ? "" : ";" + authentication));
    }
    catch (SQLException e) {
      if (e.getSQLState().equals("08006"))//08006 is expected on Derby shutdown
        System.out.println("Embedded Derby database successfully shut down!");
      else
        e.printStackTrace();
    }
  }
}
