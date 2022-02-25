/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.hsqldb;

import is.codion.common.db.database.AbstractDatabase;

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

  private final boolean nowait;

  HSQLDatabase(final String jdbcUrl) {
    this(jdbcUrl, true);
  }

  HSQLDatabase(final String jdbcUrl, final boolean nowait) {
    super(jdbcUrl);
    this.nowait = nowait;
  }

  @Override
  public String getName() {
    String name= removeUrlPrefixOptionsAndParameters(getUrl(), JDBC_URL_PREFIX_FILE, JDBC_URL_PREFIX_MEM,
            JDBC_URL_PREFIX_RES, JDBC_URL_PREFIX);

    return name.isEmpty() ? "private" : name;
  }

  @Override
  public String getSelectForUpdateClause() {
    if (nowait) {
      return FOR_UPDATE_NOWAIT;
    }

    return FOR_UPDATE;
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