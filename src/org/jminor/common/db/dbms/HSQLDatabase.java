/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import java.util.Properties;

/**
 * A Database implementation based on the HSQL database.
 */
public class HSQLDatabase extends AbstractDatabase {

  public HSQLDatabase() {
    super(HSQL);
  }

  public HSQLDatabase(final String databaseName) {
    super(HSQL, databaseName, null, null, true);
  }

  public HSQLDatabase(final String host, final String port, final String sid) {
    super(HSQL, host, port, sid, false);
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName("org.hsqldb.jdbcDriver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "IDENTITY()";
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    return "select next value for " + sequenceName;
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      final String host = getHost();
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST + " is required for database type " + getDatabaseType());

      return "jdbc:hsqldb:file:" + host + (authentication == null ? "" : ";" + authentication);
    }
    else {
      final String host = getHost();
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST + " is required for embedded database type " + getDatabaseType());
      final String port = getPort();
      if (port == null || port.length() == 0)
        throw new RuntimeException(DATABASE_PORT + " is required for embedded database type " + getDatabaseType());
      final String sid = getSid();
      if (sid == null || sid.length() == 0)
        throw new RuntimeException(DATABASE_SID + " is required for embedded database type " + getDatabaseType());

      return "jdbc:hsqldb:hsql//" + host + ":" + port + "/" + sid + (authentication == null ? "" : ";" + authentication);
    }
  }

  /** {@inheritDoc} */
  @Override
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
  @Override
  public void shutdownEmbedded(final Properties connectionProperties) {}
}