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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.dbms.sqlserver;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the SQL Server (2000 or higher) database.
 */
final class SQLServerDatabase extends AbstractDatabase {

  static final String AUTO_INCREMENT_QUERY = "SELECT @@IDENTITY";

  private static final int AUTHENTICATION_ERROR = 18456;
  private static final int REFERENTIAL_INTEGRITY_ERROR = 547;
  private static final int UNIQUE_CONSTRAINT_ERROR1 = 2601;
  private static final int UNIQUE_CONSTRAINT_ERROR2 = 2627;

  private static final String JDBC_URL_PREFIX = "jdbc:sqlserver://";

  SQLServerDatabase(String url) {
    super(url);
  }

  @Override
  public String name() {
    String name = removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX);
    if (name.contains("\\")) {
      name = name.substring(name.lastIndexOf('\\') + 1);
      if (name.contains(":")) {
        name = name.substring(0, name.indexOf(':'));
      }
    }

    return name;
  }

  @Override
  public String selectForUpdateClause() {
    return "";
  }

  @Override
  public String limitOffsetClause(Integer limit, Integer offset) {
    return createOffsetFetchNextClause(limit, offset);
  }

  /**
   * @return true
   */
  @Override
  public boolean subqueryRequiresAlias() {
    return true;
  }

  @Override
  public String autoIncrementQuery(String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public boolean isAuthenticationException(SQLException exception) {
    return requireNonNull(exception).getErrorCode() == AUTHENTICATION_ERROR;
  }

  @Override
  public boolean isReferentialIntegrityException(SQLException exception) {
    return requireNonNull(exception).getErrorCode() == REFERENTIAL_INTEGRITY_ERROR;
  }

  @Override
  public boolean isUniqueConstraintException(SQLException exception) {
    return requireNonNull(exception).getErrorCode() == UNIQUE_CONSTRAINT_ERROR1 || exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR2;
  }
}
