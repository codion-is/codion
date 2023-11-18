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
package is.codion.dbms.mysql;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

/**
 * A Database implementation based on the MySQL database.
 */
final class MySQLDatabase extends AbstractDatabase {

  private static final int REFERENTIAL_CONSTRAINT_ERROR = 1452;
  private static final int UNIQUE_CONSTRAINT_ERROR1 = 1062;
  private static final int UNIQUE_CONSTRAINT_ERROR2 = 1586;

  private static final String JDBC_URL_PREFIX = "jdbc:mysql://";

  static final String AUTO_INCREMENT_QUERY = "SELECT LAST_INSERT_ID() FROM DUAL";

  MySQLDatabase(String url) {
    super(url);
  }

  @Override
  public String name() {
    String name = removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX);
    if (name.contains("/")) {
      name = name.substring(name.lastIndexOf('/') + 1);
    }

    return name;
  }

  @Override
  public String autoIncrementQuery(String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public String selectForUpdateClause() {
    return FOR_UPDATE;
  }

  @Override
  public String limitOffsetClause(Integer limit, Integer offset) {
    return createLimitOffsetClause(limit, offset);
  }

  @Override
  public boolean isReferentialIntegrityException(SQLException exception) {
    return exception.getErrorCode() == REFERENTIAL_CONSTRAINT_ERROR;
  }

  @Override
  public boolean isUniqueConstraintException(SQLException exception) {
    return exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR1 || exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR2;
  }
}
