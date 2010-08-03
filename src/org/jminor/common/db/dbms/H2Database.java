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

  private String urlAppend = "";

  public H2Database() {
    super(H2);
  }

  public H2Database(final String databaseName) {
    super(H2, databaseName, null, null, true);
  }

  public H2Database(final String host, final String port, final String databaseName) {
    super(H2, host, port, databaseName, false);
  }

  public H2Database setUrlAppend(final String urlAppend) {
    this.urlAppend = urlAppend;
    return this;
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
      if (connectionProperties != null && (Util.nullOrEmpty((String) connectionProperties.get("user")))) {
        connectionProperties.put("user", "sa");
      }

      return "jdbc:h2:" + getHost() + (authentication == null ? "" : ";" + authentication) + urlAppend;
    }
    else {
      return "jdbc:h2://" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication) + urlAppend;
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
