/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.Util;

import java.util.Properties;

/**
 * A Database implementation based on the H2 database.
 */
public final class H2Database extends AbstractDatabase {

  static final String DRIVER_NAME = "org.h2.Driver";
  static final String AUTO_INCREMENT_QUERY = "CALL IDENTITY()";
  static final String SEQUENCE_VALUE_QUERY = "select next value for ";
  static final String SYSADMIN_USERNAME = "sa";
  static final String URL_PREFIX = "jdbc:h2:";

  private String urlAppend = "";

  /**
   * Instantiates a new H2Database.
   */
  public H2Database() {
    super(H2);
  }

  /**
   * Instantiates a new embedded H2Database.
   * @param databaseName the path to the database files
   */
  public H2Database(final String databaseName) {
    super(H2, databaseName, null, null, true);
  }

  /**
   * Instantiates a new networked H2Database.
   * @param host the host name
   * @param port the port number
   * @param databaseName the database name
   */
  public H2Database(final String host, final String port, final String databaseName) {
    super(H2, host, port, databaseName, false);
  }

  /**
   * @param urlAppend a string to append to the connection URL
   * @return this H2Database instance
   */
  public H2Database setUrlAppend(final String urlAppend) {
    this.urlAppend = urlAppend;
    return this;
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
      if (connectionProperties != null && (Util.nullOrEmpty((String) connectionProperties.get(USER_PROPERTY)))) {
        connectionProperties.put(USER_PROPERTY, SYSADMIN_USERNAME);
      }

      return URL_PREFIX + getHost() + (authentication == null ? "" : ";" + authentication) + urlAppend;
    }
    else {
      return URL_PREFIX + "//" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication) + urlAppend;
    }
  }
}
