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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.dbms.postgresql;

import is.codion.common.db.database.AbstractDatabase;
import is.codion.common.resource.MessageBundle;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * A Database implementation based on the PostgreSQL database.
 */
final class PostgreSQLDatabase extends AbstractDatabase {

	private static final MessageBundle MESSAGES =
					messageBundle(PostgreSQLDatabase.class, getBundle(PostgreSQLDatabase.class.getName()));

	private static final Map<String, String> ERROR_CODE_MAP = new HashMap<>();

	private static final String INVALID_PASS = "28P01";
	private static final String FOREIGN_KEY_VIOLATION = "23503";
	private static final String FOREIGN_KEY_VIOLATION_DELETE = "23503_delete";
	private static final String UNIQUE_CONSTRAINT_ERROR = "23505";
	private static final String TIMEOUT_ERROR = "57014";//query_cancelled
	private static final String NULL_VALUE_ERROR = "23502";
	private static final String CHECK_CONSTRAINT_ERROR = "23514";
	private static final String VALUE_TOO_LARGE_ERROR = "22001";
	private static final String MISSING_PRIVS_ERROR = "42501";

	private static final String JDBC_URL_PREFIX = "jdbc:postgresql://";
	private static final String UNIQUE_KEY_ERROR = "unique_key_error";
	private static final int MAXIMUM_STATEMENT_PARAMETERS = 65_535;

	static {
		ERROR_CODE_MAP.put(UNIQUE_CONSTRAINT_ERROR, MESSAGES.getString(UNIQUE_KEY_ERROR));
		ERROR_CODE_MAP.put(FOREIGN_KEY_VIOLATION, MESSAGES.getString("foreign_key_violation"));
		ERROR_CODE_MAP.put(FOREIGN_KEY_VIOLATION_DELETE, MESSAGES.getString("foreign_key_violation_delete"));
		ERROR_CODE_MAP.put(NULL_VALUE_ERROR, MESSAGES.getString("null_value_error"));
		ERROR_CODE_MAP.put(CHECK_CONSTRAINT_ERROR, MESSAGES.getString("check_constraint_error"));
		ERROR_CODE_MAP.put(MISSING_PRIVS_ERROR, MESSAGES.getString("missing_privileges_error"));
		ERROR_CODE_MAP.put(VALUE_TOO_LARGE_ERROR, MESSAGES.getString("value_too_large_for_column_error"));
	}

	private final boolean nowait;

	PostgreSQLDatabase(String url, boolean nowait) {
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
		return "SELECT CURRVAL('" + requireNonNull(idSource) + "')";
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		return "SELECT NEXTVAL('" + requireNonNull(sequenceName) + "')";
	}

	@Override
	public boolean isAuthenticationException(SQLException exception) {
		return INVALID_PASS.equals(requireNonNull(exception).getSQLState());
	}

	@Override
	public boolean isReferentialIntegrityException(SQLException exception) {
		return FOREIGN_KEY_VIOLATION.equals(requireNonNull(exception).getSQLState());
	}

	@Override
	public boolean isUniqueConstraintException(SQLException exception) {
		return UNIQUE_CONSTRAINT_ERROR.equals(requireNonNull(exception).getSQLState());
	}

	@Override
	public boolean isTimeoutException(SQLException exception) {
		return TIMEOUT_ERROR.equals(requireNonNull(exception).getSQLState());
	}

	@Override
	public boolean subqueryRequiresAlias() {
		return true;
	}

	@Override
	public int maximumNumberOfParameters() {
		return MAXIMUM_STATEMENT_PARAMETERS;
	}

	@Override
	public String errorMessage(SQLException exception, Operation operation) {
		requireNonNull(exception);
		requireNonNull(operation);
		String sqlState = exception.getSQLState();
		if (NULL_VALUE_ERROR.equals(sqlState)) {
			return createNullValueErrorMessage(exception.getMessage());
		}
		if (UNIQUE_CONSTRAINT_ERROR.equals(sqlState)) {
			return createUniqueConstraintErrorMessage(exception.getMessage());
		}
		if (FOREIGN_KEY_VIOLATION.equals(sqlState)) {
			return createForeignKeyViolationErrorMessage(operation);
		}
		if (ERROR_CODE_MAP.containsKey(sqlState)) {
			return ERROR_CODE_MAP.get(sqlState);
		}

		return super.errorMessage(exception, operation);
	}

	private static String createNullValueErrorMessage(String exceptionMessage) {
		int indexOfColumn = exceptionMessage.indexOf("column \"");
		int indexOfRelation = exceptionMessage.indexOf("\" of relation");
		if (indexOfColumn != -1 && indexOfRelation != -1) {
			//null value in column "column_name" of relation "table_name" violates not-null constraint
			String columnName = exceptionMessage.substring(indexOfColumn + 8, indexOfRelation);

			return MESSAGES.getString("value_missing") + ": " + columnName;
		}

		return exceptionMessage;
	}

	private static String createUniqueConstraintErrorMessage(String exceptionMessage) {
		int indexOfDetail = exceptionMessage.indexOf("Detail: Key");
		int indexOfAlreadyExists = exceptionMessage.indexOf(" already exists.");
		if (indexOfDetail != -1 && indexOfAlreadyExists != -1) {
			//Detail: Key (col1, col2)=(val1, val2) already exists.
			String values = exceptionMessage.substring(indexOfDetail + 11, indexOfAlreadyExists);

			return MESSAGES.getString(UNIQUE_KEY_ERROR) + ": " + values;
		}

		return MESSAGES.getString(UNIQUE_KEY_ERROR);
	}

	private static String createForeignKeyViolationErrorMessage(Operation operation) {
		if (operation == Operation.DELETE) {
			return ERROR_CODE_MAP.get(FOREIGN_KEY_VIOLATION_DELETE);
		}

		return ERROR_CODE_MAP.get(FOREIGN_KEY_VIOLATION);
	}
}
