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
      return "jdbc:hsqldb:file:" + getHost() + (authentication == null ? "" : ";" + authentication);
    }
    else {
      return "jdbc:hsqldb:hsql//" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getAuthenticationInfo(final Properties connectionProperties) {
    if (connectionProperties != null) {
      final String username = (String) connectionProperties.get("user");
      final String password = (String) connectionProperties.get("password");
      if (username != null && username.length() > 0 && password != null && password.length() > 0) {
        return "user=" + username + ";" + "password=" + password;
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public void shutdownEmbedded(final Properties connectionProperties) {}

  @Override
  protected void validate(final String databaseType, final String host, final String port, final String sid, final boolean embedded) {
    if (embedded) {
      require(DATABASE_HOST, host);
    }
    else {
      require(DATABASE_HOST, host);
      require(DATABASE_PORT, port);
      require(DATABASE_SID, sid);
    }
  }
}