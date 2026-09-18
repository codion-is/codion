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
 * Copyright (c) 2018 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.dbms.sqlite;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

/**
 * A SQLite embedded database implementation, quite experimental, based on the xerial/sqlite-jdbc driver.
 */
final class SQLiteDatabase extends AbstractDatabase {

	private static final String AUTO_INCREMENT_QUERY = "SELECT LAST_INSERT_ROWID()";
	// the driver reports the primary result code only, SQLITE_CONSTRAINT for all constraints,
	// the extended one being available as the name the message starts with
	private static final String UNIQUE = "[SQLITE_CONSTRAINT_UNIQUE]";
	private static final String PRIMARY_KEY = "[SQLITE_CONSTRAINT_PRIMARYKEY]";
	private static final String FOREIGN_KEY = "[SQLITE_CONSTRAINT_FOREIGNKEY]";
	private static final String NOT_NULL = "[SQLITE_CONSTRAINT_NOTNULL]";
	private static final String CHECK = "[SQLITE_CONSTRAINT_CHECK]";
	private static final String BUSY = "[SQLITE_BUSY";// along with its extended codes
	private static final String LOCKED = "[SQLITE_LOCKED";
	private static final String NO_SUCH_TABLE = "(no such table: ";
	private static final String NOT_NULL_FAILED = "NOT NULL constraint failed: ";

	/**
	 * An offset requires a limit, a negative one meaning no limit
	 */
	private static final String NO_LIMIT = "-1";

	private static final String JDBC_URL_PREFIX = "jdbc:sqlite:";

	SQLiteDatabase(String url) {
		super(url);
	}

	@Override
	public String name() {
		return removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX);
	}

	@Override
	public String autoIncrementQuery(String idSource) {
		return AUTO_INCREMENT_QUERY;
	}

	@Override
	public String selectForUpdateClause() {
		return "";
	}

	@Override
	public String limitOffsetClause(Integer limit, Integer offset, boolean ordered) {
		return createLimitOffsetClause(limit, offset, NO_LIMIT);
	}

	@Override
	protected ErrorType errorType(SQLException exception) {
		String message = exception.getMessage();
		if (message == null) {
			return super.errorType(exception);
		}
		if (message.startsWith(UNIQUE) || message.startsWith(PRIMARY_KEY)) {
			return ErrorType.UNIQUE_CONSTRAINT;
		}
		if (message.startsWith(FOREIGN_KEY)) {
			// which way is not reported
			return ErrorType.REFERENTIAL_INTEGRITY;
		}
		if (message.startsWith(NOT_NULL)) {
			return ErrorType.NULL_VALUE;
		}
		if (message.startsWith(CHECK)) {
			return ErrorType.CHECK_CONSTRAINT;
		}
		if (message.startsWith(BUSY) || message.startsWith(LOCKED)) {
			return ErrorType.ROW_LOCKED;
		}
		if (message.contains(NO_SUCH_TABLE)) {
			return ErrorType.TABLE_NOT_FOUND;
		}

		return super.errorType(exception);
	}

	@Override
	protected String errorDetail(SQLException exception, ErrorType errorType) {
		String message = exception.getMessage();
		if (errorType == ErrorType.NULL_VALUE && message != null) {
			// [SQLITE_CONSTRAINT_NOTNULL] A NOT NULL constraint failed (NOT NULL constraint failed: table.column)
			int beginIndex = message.indexOf(NOT_NULL_FAILED);
			int endIndex = message.lastIndexOf(')');
			if (beginIndex != -1 && endIndex > beginIndex) {
				String column = message.substring(beginIndex + NOT_NULL_FAILED.length(), endIndex);

				return column.substring(column.lastIndexOf('.') + 1);
			}
		}

		return null;
	}
}
