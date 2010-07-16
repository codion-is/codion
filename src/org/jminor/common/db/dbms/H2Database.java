/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.Util;

import java.util.Properties;

/**
 * A Database implementation based on the H2 database.
 */
public class H2Database extends AbstractDatabase {

  public H2Database() {
    super(H2);
  }

  public H2Database(final String databaseName) {
    super(H2, databaseName, null, null, true);
  }

  public H2Database(final String host, final String port, final String databaseName) {
    super(H2, host, port, databaseName, false);
  }

  public void loadDriver() throws ClassNotFoundException {
    Class.forName("org.h2.Driver");
  }

  public String getAutoIncrementValueSQL(final String idSource) {
    return "CALL IDENTITY()";
  }

  public String getSequenceSQL(final String sequenceName) {
    return "select next value for " + sequenceName;
  }

  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      if (connectionProperties != null && (!connectionProperties.containsKey("user") || ((String) connectionProperties.get("user")).length() == 0)) {
        connectionProperties.put("user", "sa");
      }

      return "jdbc:h2:" + getHost() + (authentication == null ? "" : ";" + authentication);
    }
    else {
      return "jdbc:h2://" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication);
    }
  }

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
