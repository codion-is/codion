/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.derby;

import org.jminor.common.db.database.AbstractDatabase;
import org.jminor.common.db.database.Database;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

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

  private final boolean embedded;

  DerbyDatabase() {
    super(Type.DERBY, Database.DATABASE_EMBEDDED.get() ? EMBEDDED_DRIVER_CLASS_NAME : DRIVER_CLASS_NAME);
    this.embedded = Database.DATABASE_EMBEDDED.get();
  }

  private DerbyDatabase(final String databaseName) {
    super(Type.DERBY, EMBEDDED_DRIVER_CLASS_NAME, requireNonNull(databaseName, "databaseName"), null, null);
    this.embedded = true;
  }

  /**
   * Instantiates a new networked DerbyDatabase.
   * @param host the host name
   * @param port the port number
   * @param sid the service identifier
   */
  private DerbyDatabase(final String host, final Integer port, final String sid) {
    super(Type.DERBY, DRIVER_CLASS_NAME, requireNonNull(host, "host"), requireNonNull(port),
            requireNonNull(sid, "sid"));
    this.embedded = false;
  }

  @Override
  public boolean isEmbedded() {
    return embedded;
  }

  @Override
  public SelectForUpdateSupport getSelectForUpdateSupport() {
    return SelectForUpdateSupport.FOR_UPDATE;
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY + requireNonNull(idSource, "idSource");
  }

  @Override
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      return URL_PREFIX + getHost() + (authentication == null ? "" : ";" + authentication) + getUrlAppend();
    }
    else {
      return URL_PREFIX + "//" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication) + getUrlAppend();
    }
  }

  @Override
  public void shutdownEmbedded(final Properties connectionProperties) {
    try {
      final String authentication = getAuthenticationInfo(connectionProperties);
      DriverManager.getConnection(URL_PREFIX + getHost() + ";shutdown=true"
              + (authentication == null ? "" : ";" + authentication)).close();
    }
    catch (final SQLException e) {
      if (!e.getSQLState().equals(SHUTDOWN_ERROR_CODE)) {//08006 is expected on Derby shutdown
        System.err.println("Embedded Derby database was did not successfully shut down: " + e.getMessage());
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

  /**
   * Instantiates a new server-based DerbyDatabase.
   * @param host the host name
   * @param port the port number
   * @param dbname the db name
   * @return a database instance
   */
  public static DerbyDatabase derbyServerDatabase(final String host, final Integer port, final String dbname) {
    return new DerbyDatabase(host, port, dbname);
  }

  /**
   * Instantiates a new embedded DerbyDatabase.
   * @param databaseName the path to the database files
   * @return a database instance
   */
  public static DerbyDatabase derbyFileDatabase(final String databaseName) {
    return new DerbyDatabase(databaseName);
  }
}
