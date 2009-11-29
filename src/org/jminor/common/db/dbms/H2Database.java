/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class H2Database extends AbstractDatabase {

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

  public H2Database() {
    super(H2);
  }

  public H2Database(final String databaseName) {
    super(H2, databaseName, null, null, true);
  }

  public H2Database(final String host, final String port, final String databaseName) {
    super(H2, host, port, databaseName, false);
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName("org.h2.Driver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "CALL IDENTITY()";
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    return "select next value for " + sequenceName;
  }

  /** {@inheritDoc} */
  public String getSQLDateString(final Date value, final boolean isTimestamp) {
    return isTimestamp ?
            "PARSEDATETIME('" + ((DateFormat) timestampFormat.get()).format(value) + "','yyyy-MM-dd HH:mm:ss')" :
            "PARSEDATETIME('" + ((DateFormat) dateFormat.get()).format(value) + "','yyyy-MM-dd')";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      if (!connectionProperties.containsKey("user") || ((String) connectionProperties.get("user")).length() == 0)
        connectionProperties.put("user","sa");
      final String host = getHost();
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST + " is required for database type " + getDatabaseType());

      return "jdbc:h2:" + host + (authentication == null ? "" : ";" + authentication);
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

      return "jdbc:h2://" + host + ":" + port + "/" + sid + (authentication == null ? "" : ";" + authentication);
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
