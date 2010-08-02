/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.Util;

import java.util.Properties;

/**
 * A Database implementation based on the HSQL database.
 */
public final class HSQLDatabase extends AbstractDatabase {

  public HSQLDatabase() {
    super(HSQL);
  }

  public HSQLDatabase(final String databaseName) {
    super(HSQL, databaseName, null, null, true);
  }

  public HSQLDatabase(final String host, final String port, final String sid) {
    super(HSQL, host, port, sid, false);
  }

  public void loadDriver() throws ClassNotFoundException {
    Class.forName("org.hsqldb.jdbcDriver");
  }

  public String getAutoIncrementValueSQL(final String idSource) {
    return "IDENTITY()";
  }

  public String getSequenceSQL(final String sequenceName) {
    return "select next value for " + sequenceName;
  }

  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      return "jdbc:hsqldb:file:" + getHost() + (authentication == null ? "" : ";" + authentication);
    }
    else {
      return "jdbc:hsqldb:hsql//" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication);
    }
  }

  @Override
  public String getAuthenticationInfo(final Properties connectionProperties) {
    if (connectionProperties != null) {
      final String username = (String) connectionProperties.get("user");
      final String password = (String) connectionProperties.get("password");
      if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
        return "user=" + username + ";" + "password=" + password;
      }
    }

    return null;
  }

  @Override
  public void shutdownEmbedded(final Properties connectionProperties) {}

  @Override
  protected void validate(final String databaseType, final String host, final String port, final String sid, final boolean embedded) {
    if (embedded) {
      Util.require(DATABASE_HOST, host);
    }
    else {
      Util.require(DATABASE_HOST, host);
      Util.require(DATABASE_PORT, port);
      Util.require(DATABASE_SID, sid);
    }
  }
}