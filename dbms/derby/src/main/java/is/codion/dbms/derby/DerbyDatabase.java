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
package is.codion.dbms.derby;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.DriverManager;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the Derby database.
 */
final class DerbyDatabase extends AbstractDatabase {

	private static final String SHUTDOWN = "08006";
	private static final String STATEMENT_TIMEOUT = "XCL52";
	private static final String LOCK_TIMEOUT = "40XL1";
	private static final String DEADLOCK = "40001";
	private static final String AUTHENTICATION_FAILURE = "08004";
	private static final String TABLE_NOT_FOUND = "42X05";

	private static final String JDBC_URL_PREFIX_TCP = "jdbc:derby://";
	private static final String JDBC_URL_PREFIX_FILE = "jdbc:derby:";

	static final String AUTO_INCREMENT_QUERY = "VALUES IDENTITY_VAL_LOCAL()";

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
	public String sequenceQuery(String sequenceName) {
		return "VALUES NEXT VALUE FOR " + requireNonNull(sequenceName);
	}

	@Override
	public String selectForUpdateClause() {
		return FOR_UPDATE;
	}

	@Override
	public String limitOffsetClause(Integer limit, Integer offset, boolean ordered) {
		return createOffsetFetchNextClause(limit, offset);
	}

	@Override
	public String autoIncrementQuery(String idSource) {
		return AUTO_INCREMENT_QUERY;
	}

	/**
	 * The error code is a severity, the sql state identifying the error, most being covered by the defaults
	 */
	@Override
	protected ErrorType errorType(SQLException exception) {
		String sqlState = exception.getSQLState();
		if (sqlState == null) {
			return super.errorType(exception);
		}
		switch (sqlState) {
			case STATEMENT_TIMEOUT:
				return ErrorType.TIMEOUT;
			case LOCK_TIMEOUT:
			case DEADLOCK:
				return ErrorType.ROW_LOCKED;
			case AUTHENTICATION_FAILURE:
				return ErrorType.AUTHENTICATION;
			case TABLE_NOT_FOUND:
				return ErrorType.TABLE_NOT_FOUND;
			default:
				return super.errorType(exception);
		}
	}

	@Override
	protected String errorDetail(SQLException exception, ErrorType errorType) {
		String message = exception.getMessage();
		if (errorType == ErrorType.NULL_VALUE && message != null) {
			// Column 'NAME'  cannot accept a NULL value.
			return between(message, "'", "'");
		}

		return null;
	}

	@Override
	protected void closeDatabase() throws SQLException {
		if (url().startsWith(JDBC_URL_PREFIX_TCP)) {
			return;// shutting down a database on a server closes it for all its clients
		}
		try {
			DriverManager.getConnection(url() + ";shutdown=true");
		}
		catch (SQLException e) {
			if (!SHUTDOWN.equals(e.getSQLState())) {
				throw e;
			}
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
