/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.AbstractDatabase;

import java.util.Properties;

/**
 * A Database implementation based on the HSQL database.
 */
public final class HSQLDatabase extends AbstractDatabase {

  static final String DRIVER_NAME = "org.hsqldb.jdbcDriver";
  static final String AUTO_INCREMENT_QUERY = "IDENTITY()";
  static final String SEQUENCE_VALUE_QUERY = "select next value for ";
  static final String EMBEDDED_URL_PREFIX = "jdbc:hsqldb:file:";
  static final String NETWORKED_URL_PREFIX = "jdbc:hsqldb:hsql//";

  /**
   * Instantiates a new H2Database.
   */
  public HSQLDatabase() {
    super(HSQL);
  }

  /**
   * Instantiates a new embedded HSQLDatabase.
   * @param databaseName the path to the database files
   */
  public HSQLDatabase(final String databaseName) {
    super(HSQL, databaseName, null, null, true);
  }

  /**
   * Instantiates a new networked HSQLDatabase.
   * @param host the host name
   * @param port the port number
   * @param sid the service identifier
   */
  public HSQLDatabase(final String host, final String port, final String sid) {
    super(HSQL, host, port, sid, false);
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName(DRIVER_NAME);
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  /** {@inheritDoc} */
  @Override
  public String getSequenceSQL(final String sequenceName) {
    return SEQUENCE_VALUE_QUERY + sequenceName;
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      return EMBEDDED_URL_PREFIX + getHost() + (authentication == null ? "" : ";" + authentication);
    }
    else {
      return NETWORKED_URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void shutdownEmbedded(final Properties connectionProperties) {}
}