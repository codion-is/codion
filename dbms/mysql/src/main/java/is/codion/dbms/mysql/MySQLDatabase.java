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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.dbms.mysql;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * A Database implementation based on the MySQL database.
 */
final class MySQLDatabase extends AbstractDatabase {

	private static final Map<Integer, ErrorType> ERROR_TYPES = new HashMap<>();

	static {
		ERROR_TYPES.put(1062, ErrorType.UNIQUE_CONSTRAINT);// duplicate entry
		ERROR_TYPES.put(1586, ErrorType.UNIQUE_CONSTRAINT);// duplicate entry, with key name
		ERROR_TYPES.put(1452, ErrorType.PARENT_MISSING);// cannot add or update a child row
		ERROR_TYPES.put(1216, ErrorType.PARENT_MISSING);// the same, without the detail
		ERROR_TYPES.put(1451, ErrorType.CHILD_EXISTS);// cannot delete or update a parent row
		ERROR_TYPES.put(1217, ErrorType.CHILD_EXISTS);// the same, without the detail
		ERROR_TYPES.put(1048, ErrorType.NULL_VALUE);// column cannot be null
		ERROR_TYPES.put(1364, ErrorType.NULL_VALUE);// field does not have a default value
		ERROR_TYPES.put(3819, ErrorType.CHECK_CONSTRAINT);
		ERROR_TYPES.put(1406, ErrorType.VALUE_TOO_LARGE);// data too long for column
		ERROR_TYPES.put(1264, ErrorType.VALUE_TOO_LARGE);// out of range value for column
		ERROR_TYPES.put(1044, ErrorType.MISSING_PRIVILEGES);// access denied to database
		ERROR_TYPES.put(1142, ErrorType.MISSING_PRIVILEGES);// command denied for table
		ERROR_TYPES.put(1143, ErrorType.MISSING_PRIVILEGES);// command denied for column
		ERROR_TYPES.put(1045, ErrorType.AUTHENTICATION);// access denied for user
		ERROR_TYPES.put(3118, ErrorType.ACCOUNT_LOCKED);
		ERROR_TYPES.put(1862, ErrorType.PASSWORD_EXPIRED);// when logging in
		ERROR_TYPES.put(1820, ErrorType.PASSWORD_EXPIRED);// when executing a statement, logging in being allowed
		ERROR_TYPES.put(1205, ErrorType.ROW_LOCKED);// lock wait timeout exceeded
		ERROR_TYPES.put(3572, ErrorType.ROW_LOCKED);// lock could not be acquired immediately and NOWAIT is set
		ERROR_TYPES.put(3024, ErrorType.TIMEOUT);// max_execution_time exceeded
		ERROR_TYPES.put(1146, ErrorType.TABLE_NOT_FOUND);
	}

	private static final String DUPLICATE_ENTRY = "Duplicate entry ";
	private static final String FOR_KEY = " for key";
	private static final String JDBC_URL_PREFIX = "jdbc:mysql://";

	/**
	 * An offset requires a limit, this being the maximum one
	 */
	private static final String NO_LIMIT = "18446744073709551615";

	static final String AUTO_INCREMENT_QUERY = "SELECT LAST_INSERT_ID() FROM DUAL";

	private final boolean nowait;

	MySQLDatabase(String url) {
		this(url, true);
	}

	MySQLDatabase(String url, boolean nowait) {
		super(url);
		this.nowait = nowait;
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
		if (nowait) {
			return FOR_UPDATE_NOWAIT;
		}

		return FOR_UPDATE;
	}

	@Override
	public String limitOffsetClause(Integer limit, Integer offset, boolean ordered) {
		return createLimitOffsetClause(limit, offset, NO_LIMIT);
	}

	@Override
	protected ErrorType errorType(SQLException exception) {
		ErrorType errorType = ERROR_TYPES.get(exception.getErrorCode());

		return errorType == null ? super.errorType(exception) : errorType;
	}

	@Override
	protected String errorDetail(SQLException exception, ErrorType errorType) {
		String message = exception.getMessage();
		if (message == null) {
			return null;
		}
		switch (errorType) {
			case NULL_VALUE:
				// Column 'name' cannot be null
				// Field 'name' doesn't have a default value
			case VALUE_TOO_LARGE:
				// Data too long for column 'name' at row 1
				// Out of range value for column 'name' at row 1
				return between(message, "'", "'");
			case UNIQUE_CONSTRAINT:
				// Duplicate entry 'A-b' for key 'name'
				return between(message, DUPLICATE_ENTRY, FOR_KEY);
			default:
				return null;
		}
	}

	private static String between(String message, String prefix, String suffix) {
		int prefixIndex = message.indexOf(prefix);
		if (prefixIndex == -1) {
			return null;
		}
		int beginIndex = prefixIndex + prefix.length();
		int endIndex = message.indexOf(suffix, beginIndex);

		return endIndex == -1 ? null : message.substring(beginIndex, endIndex);
	}
}
