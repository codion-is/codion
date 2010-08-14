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
public final class DerbyDatabase extends AbstractDatabase {

  static final String EMBEDDED_DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
  static final String NETWORKED_DRIVER_NAME = "org.apache.derby.jdbc.ClientDriver";
  static final String AUTO_INCREMENT_QUERY = "select IDENTITY_VAL_LOCAL() from ";
  static final String URL_PREFIX = "jdbc:derby:";

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

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName(isEmbedded() ? EMBEDDED_DRIVER_NAME : NETWORKED_DRIVER_NAME);
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return AUTO_INCREMENT_QUERY + idSource;
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      return URL_PREFIX + getHost() + (authentication == null ? "" : ";" + authentication);
    }
    else {
      return URL_PREFIX + "//" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void shutdownEmbedded(final Properties connectionProperties) {
    try {
      final String authentication = getAuthenticationInfo(connectionProperties);
      DriverManager.getConnection(URL_PREFIX + getHost() + ";shutdown=true"
               + (authentication == null ? "" : ";" + authentication)).close();
    }
    catch (SQLException e) {
      if (!e.getSQLState().equals("08006")) {//08006 is expected on Derby shutdown
        LOG.error("Embedded Derby database was did not successfully shut down!", e);
      }
    }
  }
}
