/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.Util;
import org.jminor.common.db.AbstractDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A Database implementation based on the Derby database.
 */
public final class DerbyDatabase extends AbstractDatabase {

  static final String DRIVER_CLASS_NAME = "org.apache.derby.jdbc.ClientDriver";
  static final String EMBEDDED_DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
  static final String AUTO_INCREMENT_QUERY = "select IDENTITY_VAL_LOCAL() from ";
  static final String URL_PREFIX = "jdbc:derby:";

  private static final Logger LOG = LoggerFactory.getLogger(DerbyDatabase.class);

  static final boolean EMBEDDED = Boolean.TRUE.toString().equals(System.getProperty(DATABASE_EMBEDDED, Boolean.FALSE.toString()));

  /**
   * Instantiates a new DerbyDatabase.
   */
  public DerbyDatabase() {
    super(Type.DERBY, EMBEDDED ? EMBEDDED_DRIVER_CLASS_NAME : DRIVER_CLASS_NAME);
  }

  /**
   * Instantiates a new embedded DerbyDatabase.
   * @param databaseName the path to the database files
   */
  public DerbyDatabase(final String databaseName) {
    super(Type.DERBY, EMBEDDED_DRIVER_CLASS_NAME, databaseName, null, null, true);
  }

  /**
   * Instantiates a new networked DerbyDatabase.
   * @param host the host name
   * @param port the port number
   * @param sid the service identifier
   */
  public DerbyDatabase(final String host, final String port, final String sid) {
    super(Type.DERBY, DRIVER_CLASS_NAME, host, port, sid, false);
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsNowait() {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementValueSQL(final String idSource) {
    Util.rejectNullValue(idSource, "idSource");
    return AUTO_INCREMENT_QUERY + idSource;
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      Util.require("host", getHost());
      return URL_PREFIX + getHost() + (authentication == null ? "" : ";" + authentication);
    }
    else {
      Util.require("host", getHost());
      Util.require("port", getPort());
      Util.require("sid", getSid());
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
    catch (final SQLException e) {
      if (!e.getSQLState().equals("08006")) {//08006 is expected on Derby shutdown
        LOG.error("Embedded Derby database was did not successfully shut down!", e);
      }
    }
  }
}
