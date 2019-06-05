/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.AbstractDatabase;
import org.jminor.common.db.Database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * A Database implementation based on the Derby database.
 */
public final class DerbyDatabase extends AbstractDatabase {

  private static final String SHUTDOWN_ERROR_CODE = "08006";
  private static final int FOREIGN_KEY_ERROR = 23503;

  static final String DRIVER_CLASS_NAME = "org.apache.derby.jdbc.ClientDriver";
  static final String EMBEDDED_DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
  static final String AUTO_INCREMENT_QUERY = "select IDENTITY_VAL_LOCAL() from ";
  static final String URL_PREFIX = "jdbc:derby:";

  private static final Logger LOG = LoggerFactory.getLogger(DerbyDatabase.class);

  static final boolean EMBEDDED = Database.DATABASE_EMBEDDED.get();

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
    super(Type.DERBY, EMBEDDED_DRIVER_CLASS_NAME, Objects.requireNonNull(databaseName, "databaseName"),
            null, null, true);
  }

  /**
   * Instantiates a new networked DerbyDatabase.
   * @param host the host name
   * @param port the port number
   * @param sid the service identifier
   */
  public DerbyDatabase(final String host, final Integer port, final String sid) {
    super(Type.DERBY, DRIVER_CLASS_NAME, Objects.requireNonNull(host, "host"), Objects.requireNonNull(port),
            Objects.requireNonNull(sid, "sid"), false);
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsNowait() {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY + Objects.requireNonNull(idSource, "idSource");
  }

  /** {@inheritDoc} */
  @Override
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
    catch (final SQLException e) {
      if (!e.getSQLState().equals(SHUTDOWN_ERROR_CODE)) {//08006 is expected on Derby shutdown
        LOG.error("Embedded Derby database was did not successfully shut down!", e);
      }
    }
  }

  /**
   * @param exception the exception
   * @return true if this exception is a referential integrity error
   */
  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getErrorCode() == FOREIGN_KEY_ERROR;
  }
}
