/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.Util;

import org.apache.log4j.Logger;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A Database implementation based on the Derby database.
 */
public class DerbyDatabase extends AbstractDatabase {

  private static final Logger LOG = Util.getLogger(DerbyDatabase.class);

  public DerbyDatabase() {
    super(DERBY);
  }

  public DerbyDatabase(final String databaseName) {
    super(DERBY, databaseName, null, null, true);
  }

  public DerbyDatabase(final String host, final String port, final String sid) {
    super(DERBY, host, port, sid, false);
  }

  public void loadDriver() throws ClassNotFoundException {
    Class.forName(isEmbedded() ? "org.apache.derby.jdbc.EmbeddedDriver" : "org.apache.derby.jdbc.ClientDriver");
  }

  public String getAutoIncrementValueSQL(final String idSource) {
    return "select IDENTITY_VAL_LOCAL() from " + idSource;
  }

  public String getSequenceSQL(final String sequenceName) {
    throw new RuntimeException("Sequence support is not implemented for database type: " + getDatabaseType());
  }

  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      return "jdbc:derby:" + getHost() + (authentication == null ? "" : ";" + authentication);
    }
    else {
      return "jdbc:derby://" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication);
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
  public void shutdownEmbedded(final Properties connectionProperties) {
    try {
      final String authentication = getAuthenticationInfo(connectionProperties);
      DriverManager.getConnection("jdbc:derby:" + getHost() + ";shutdown=true"
               + (authentication == null ? "" : ";" + authentication));
    }
    catch (SQLException e) {
      if (!e.getSQLState().equals("08006")) {//08006 is expected on Derby shutdown
        LOG.error("Embedded Derby database was did not successfully shut down!", e);
      }
    }
  }

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
