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

  static final String DRIVER_NAME = "org.hsqldb.jdbcDriver";
  static final String AUTO_INCREMENT_QUERY = "IDENTITY()";
  static final String SEQUENCE_VALUE_QUERY = "select next value for ";
  static final String EMBEDDED_URL_PREFIX = "jdbc:hsqldb:file:";
  static final String NETWORKED_URL_PREFIX = "jdbc:hsqldb:hsql//";

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
    Class.forName(DRIVER_NAME);
  }

  public String getAutoIncrementValueSQL(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  public String getSequenceSQL(final String sequenceName) {
    return SEQUENCE_VALUE_QUERY + sequenceName;
  }

  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      return EMBEDDED_URL_PREFIX + getHost() + (authentication == null ? "" : ";" + authentication);
    }
    else {
      return NETWORKED_URL_PREFIX + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication);
    }
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