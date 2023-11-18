/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson.
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
  static final String SEQUENCE_VALUE_QUERY = "SELECT NEXT VALUE FOR ";

  private final boolean nowait;

  HSQLDatabase(String url) {
    this(url, true);
  }

  HSQLDatabase(String url, boolean nowait) {
    super(url);
    this.nowait = nowait;
  }

  @Override
  public String name() {
    String name = removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX_FILE, JDBC_URL_PREFIX_MEM,
            JDBC_URL_PREFIX_RES, JDBC_URL_PREFIX);

    return name.isEmpty() ? "private" : name;
  }

  @Override
  public String selectForUpdateClause() {
    if (nowait) {
      return FOR_UPDATE_NOWAIT;
    }

    return FOR_UPDATE;
  }

  @Override
  public String limitOffsetClause(Integer limit, Integer offset) {
    return createLimitOffsetClause(limit, offset);
  }

  @Override
  public String autoIncrementQuery(String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public String sequenceQuery(String sequenceName) {
    return SEQUENCE_VALUE_QUERY + requireNonNull(sequenceName, "sequenceName");
  }
}