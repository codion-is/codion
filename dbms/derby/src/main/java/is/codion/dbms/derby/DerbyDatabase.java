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
package is.codion.dbms.derby;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the Derby database.
 */
final class DerbyDatabase extends AbstractDatabase {

  private static final String TIMEOUT_ERROR_CODE = "XCL52";
  private static final int FOREIGN_KEY_ERROR = 23503;

  private static final String JDBC_URL_PREFIX_TCP = "jdbc:derby://";
  private static final String JDBC_URL_PREFIX_FILE = "jdbc:derby:";

  static final String AUTO_INCREMENT_QUERY = "SELECT IDENTITY_VAL_LOCAL() FROM ";

  DerbyDatabase(String url) {
    super(url);
  }

  @Override
  public String name() {
    String name = url();
    boolean tcp = name.startsWith(JDBC_URL_PREFIX_TCP);
    name = removeUrlPrefixOptionsAndParameters(name, JDBC_URL_PREFIX_TCP, JDBC_URL_PREFIX_FILE);
    if (tcp && name.contains("/")) {
      name = name.substring(name.indexOf('/') + 1);
    }

    return name;
  }

  @Override
  public String selectForUpdateClause() {
    return FOR_UPDATE;
  }

  @Override
  public String limitOffsetClause(Integer limit, Integer offset) {
    return createOffsetFetchNextClause(limit, offset);
  }

  @Override
  public String autoIncrementQuery(String idSource) {
    return AUTO_INCREMENT_QUERY + requireNonNull(idSource, "idSource");
  }

  @Override
  public boolean isReferentialIntegrityException(SQLException exception) {
    return exception.getErrorCode() == FOREIGN_KEY_ERROR;
  }

  @Override
  public boolean isTimeoutException(SQLException exception) {
    return TIMEOUT_ERROR_CODE.equals(exception.getSQLState());
  }
}
