/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.hsqldb;

import org.jminor.common.db.database.AbstractDatabase;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the HSQL database.
 */
final class HSQLDatabase extends AbstractDatabase {

  private static final String JDBC_URL_PREFIX = "jdbc:hsqldb:";
  private static final String JDBC_URL_PREFIX_MEM = "jdbc:hsqldb:mem:";
  private static final String JDBC_URL_PREFIX_FILE = "jdbc:hsqldb:file:";
  private static final String JDBC_URL_PREFIX_RES = "jdbc:hsqldb:res:";

  static final String AUTO_INCREMENT_QUERY = "IDENTITY()";
  static final String SEQUENCE_VALUE_QUERY = "select next value for ";

  HSQLDatabase(final String jdbcUrl) {
    super(jdbcUrl);
  }

  @Override
  public String getName() {
    final String name= removeUrlPrefixAndOptions(getUrl(), JDBC_URL_PREFIX_FILE, JDBC_URL_PREFIX_MEM,
            JDBC_URL_PREFIX_RES, JDBC_URL_PREFIX);

    return name.isEmpty() ? "private" : name;
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public String getSequenceQuery(final String sequenceName) {
    return SEQUENCE_VALUE_QUERY + requireNonNull(sequenceName, "sequenceName");
  }
}