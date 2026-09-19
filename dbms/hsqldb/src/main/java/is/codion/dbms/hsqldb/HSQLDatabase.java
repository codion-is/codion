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
package is.codion.dbms.hsqldb;

import is.codion.common.db.database.AbstractDatabase;
import is.codion.common.db.exception.ErrorType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static is.codion.common.utilities.user.User.user;
import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the HSQL database.
 */
final class HSQLDatabase extends AbstractDatabase {

	private static final String JDBC_URL_PREFIX = "jdbc:hsqldb:";
	private static final String JDBC_URL_PREFIX_MEM = "jdbc:hsqldb:mem:";
	private static final String JDBC_URL_PREFIX_FILE = "jdbc:hsqldb:file:";
	private static final String JDBC_URL_PREFIX_RES = "jdbc:hsqldb:res:";
	private static final String[] JDBC_URL_PREFIXES_SERVER = {"jdbc:hsqldb:hsql:", "jdbc:hsqldb:hsqls:", "jdbc:hsqldb:http:", "jdbc:hsqldb:https:"};
	private static final String COLUMN = "column: ";
	private static final int FOREIGN_KEY_NO_PARENT = -177;
	private static final int USER_NOT_FOUND = -4001;
	private static final String SHUTDOWN = "SHUTDOWN";
	private static final String SYSADMIN_USERNAME = "sa";

	static final String AUTO_INCREMENT_QUERY = "CALL IDENTITY()";
	static final String SEQUENCE_VALUE_QUERY = "CALL NEXT VALUE FOR ";

	HSQLDatabase(String url) {
		super(url);
	}

	@Override
	public String name() {
		String name = removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX_FILE, JDBC_URL_PREFIX_MEM,
						JDBC_URL_PREFIX_RES, JDBC_URL_PREFIX);

		return name.isEmpty() ? "private" : name;
	}

	@Override
	public String selectForUpdateClause() {
		return FOR_UPDATE;
	}

	@Override
	public String limitOffsetClause(Integer limit, Integer offset, boolean ordered) {
		return createLimitOffsetClause(limit, offset);
	}

	@Override
	public String autoIncrementQuery(String idSource) {
		return AUTO_INCREMENT_QUERY;
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		return SEQUENCE_VALUE_QUERY + requireNonNull(sequenceName);
	}

	/**
	 * The sql state defaults cover all but which way a foreign key is violated and an unknown user
	 */
	@Override
	protected ErrorType errorType(SQLException exception) {
		switch (exception.getErrorCode()) {
			case FOREIGN_KEY_NO_PARENT:
				return ErrorType.PARENT_MISSING;
			case USER_NOT_FOUND:
				return ErrorType.AUTHENTICATION;
			default:
				return super.errorType(exception);
		}
	}

	@Override
	protected String errorDetail(SQLException exception, ErrorType errorType) {
		String message = exception.getMessage();
		if (message == null) {
			return null;
		}
		switch (errorType) {
			case NULL_VALUE:
				// integrity constraint violation: NOT NULL check constraint ; SYS_CT_10093 table: PARENT column: NAME
				int columnIndex = message.indexOf(COLUMN);

				return columnIndex == -1 ? null : message.substring(columnIndex + COLUMN.length()).trim();
			case UNIQUE_CONSTRAINT:
				// integrity constraint violation: unique constraint or index violation ; PARENT_UK table: PARENT
			case PARENT_MISSING:
				// integrity constraint violation: foreign key no parent ; CHILD_FK table: CHILD value: 99
			case CHILD_EXISTS:
				// integrity constraint violation: foreign key no action ; CHILD_FK table: CHILD
			case CHECK_CONSTRAINT:
				// integrity constraint violation: check constraint ; PARENT_CK table: PARENT
				return between(message, " ; ", " table:");
			default:
				return null;
		}
	}

	@Override
	protected void closeDatabase() throws SQLException {
		for (String serverPrefix : JDBC_URL_PREFIXES_SERVER) {
			if (url().regionMatches(true, 0, serverPrefix, 0, serverPrefix.length())) {
				return;// shutting down a database on a server closes it for all its clients
			}
		}
		try (Connection connection = createConnection(user(SYSADMIN_USERNAME));
				 Statement statement = connection.createStatement()) {
			statement.execute(SHUTDOWN);
		}
	}
}