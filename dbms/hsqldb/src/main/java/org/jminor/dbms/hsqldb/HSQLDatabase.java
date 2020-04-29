/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.hsqldb;

import org.jminor.common.db.database.AbstractDatabase;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the HSQL database.
 */
public final class HSQLDatabase extends AbstractDatabase {

  static final String DRIVER_CLASS_NAME = "org.hsqldb.jdbcDriver";
  static final String AUTO_INCREMENT_QUERY = "IDENTITY()";
  static final String SEQUENCE_VALUE_QUERY = "select next value for ";
  static final String EMBEDDED_URL_PREFIX = "jdbc:hsqldb:file:";
  static final String NETWORKED_URL_PREFIX = "jdbc:hsqldb:hsql//";

  private final boolean embedded;

  HSQLDatabase() {
    super(Type.HSQL, DRIVER_CLASS_NAME);
    this.embedded = false;
  }

  private HSQLDatabase(final String databaseName) {
    super(Type.HSQL, DRIVER_CLASS_NAME, requireNonNull(databaseName, "databaseName"), null, null);
    this.embedded = true;
  }

  private HSQLDatabase(final String host, final Integer port, final String sid) {
    super(Type.HSQL, DRIVER_CLASS_NAME, requireNonNull(host, "host"), requireNonNull(port, "port"),
            requireNonNull(sid, "sid"));
    this.embedded = false;
  }

  @Override
  public boolean isEmbedded() {
    return embedded;
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public String getSequenceQuery(final String sequenceName) {
    return SEQUENCE_VALUE_QUERY + requireNonNull(sequenceName, "sequenceName");
  }

  @Override
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      return EMBEDDED_URL_PREFIX + getHost() + (authentication == null ? "" : ";" + authentication) + getUrlAppend();
    }
    else {
      return NETWORKED_URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication) + getUrlAppend();
    }
  }

  /**
   * Instantiates a new server-based HSQLDatabase.
   * @param host the host name
   * @param port the port number
   * @param dbname the db name
   * @return a database instance
   */
  public static HSQLDatabase hsqlServerDatabase(final String host, final Integer port, final String dbname) {
    return new HSQLDatabase(host, port, dbname);
  }

  /**
   * Instantiates a new embedded HSQLDatabase.
   * @param databaseName the path to the database files
   * @return a database instance
   */
  public static HSQLDatabase hsqlFileDatabase(final String databaseName) {
    return new HSQLDatabase(databaseName);
  }
}