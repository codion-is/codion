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
package is.codion.dbms.oracle;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the Oracle database.
 */
final class OracleDatabase extends AbstractDatabase {

	private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(OracleDatabase.class.getName());

	private static final String JDBC_URL_DRIVER_PREFIX = "jdbc:oracle:thin:";
	private static final String JDBC_URL_PREFIX = JDBC_URL_DRIVER_PREFIX + "@";
	private static final String JDBC_URL_WALLET_PREFIX = JDBC_URL_DRIVER_PREFIX + "/@";

	private static final Map<Integer, String> ERROR_CODE_MAP = new HashMap<>();

	private static final int UNIQUE_KEY_ERROR = 1;
	private static final int CHILD_RECORD_ERROR = 2292;
	private static final int NULL_VALUE_ERROR = 1400;
	private static final int INTEGRITY_CONSTRAINT_ERROR = 2291;
	private static final int NULL_VALUE_ERROR_2 = 1407;
	private static final int CHECK_CONSTRAINT_ERROR = 2290;
	private static final int MISSING_PRIVS_ERROR = 1031;
	private static final int LOGIN_CREDS_ERROR = 1017;
	private static final int TABLE_NOT_FOUND_ERROR = 942;
	private static final int UNABLE_TO_CONNECT_ERROR = 1045;
	private static final int VALUE_TOO_LARGE_ERROR = 1401;
	private static final int VIEW_HAS_ERRORS_ERROR = 4063;
	private static final int TIMEOUT_ERROR = 17016;

	static {
		ERROR_CODE_MAP.put(UNIQUE_KEY_ERROR, MESSAGES.getString("unique_key_error"));
		ERROR_CODE_MAP.put(CHILD_RECORD_ERROR, MESSAGES.getString("child_record_error"));
		ERROR_CODE_MAP.put(NULL_VALUE_ERROR, MESSAGES.getString("null_value_error"));
		ERROR_CODE_MAP.put(INTEGRITY_CONSTRAINT_ERROR, MESSAGES.getString("integrity_constraint_error"));
		ERROR_CODE_MAP.put(NULL_VALUE_ERROR_2, MESSAGES.getString("null_value_error"));
		ERROR_CODE_MAP.put(CHECK_CONSTRAINT_ERROR, MESSAGES.getString("check_constraint_error"));
		ERROR_CODE_MAP.put(MISSING_PRIVS_ERROR, MESSAGES.getString("missing_privileges_error"));
		ERROR_CODE_MAP.put(LOGIN_CREDS_ERROR, MESSAGES.getString("login_credentials_error"));
		ERROR_CODE_MAP.put(TABLE_NOT_FOUND_ERROR, MESSAGES.getString("table_not_found_error"));
		ERROR_CODE_MAP.put(UNABLE_TO_CONNECT_ERROR, MESSAGES.getString("user_cannot_connect"));
		ERROR_CODE_MAP.put(VALUE_TOO_LARGE_ERROR, MESSAGES.getString("value_too_large_for_column_error"));
		ERROR_CODE_MAP.put(VIEW_HAS_ERRORS_ERROR, MESSAGES.getString("view_has_errors_error"));
	}

	private final boolean nowait;

	OracleDatabase(String url) {
		this(url, true);
	}

	OracleDatabase(String url, boolean nowait) {
		super(url);
		this.nowait = nowait;
	}

	@Override
	public String name() {
		String name = removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX, JDBC_URL_WALLET_PREFIX);
		if (name.contains("/")) {//pluggable database
			name = name.substring(name.lastIndexOf('/') + 1);
		}

		return name.substring(name.lastIndexOf(':') + 1);
	}

	@Override
	public String autoIncrementQuery(String idSource) {
		return "SELECT " + requireNonNull(idSource, "idSource") + ".CURRVAL FROM DUAL";
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		return "SELECT " + requireNonNull(sequenceName, "sequenceName") + ".NEXTVAL FROM DUAL";
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
		return createOffsetFetchNextClause(limit, offset);
	}

	@Override
	public String errorMessage(SQLException exception, Operation operation) {
		requireNonNull(exception);
		if (exception.getErrorCode() == NULL_VALUE_ERROR || exception.getErrorCode() == NULL_VALUE_ERROR_2) {
			String exceptionMessage = exception.getMessage();
			int newlineIndex = exception.getMessage().indexOf('\n');
			if (newlineIndex != -1) {
				exceptionMessage = exceptionMessage.substring(0, newlineIndex);
			}
			String errorMsg = exceptionMessage;
			String columnName = errorMsg.substring(errorMsg.lastIndexOf('.') + 2, errorMsg.lastIndexOf(')') - 1);

			return MESSAGES.getString("value_missing") + ": " + columnName;
		}

		if (ERROR_CODE_MAP.containsKey(exception.getErrorCode())) {
			return ERROR_CODE_MAP.get(exception.getErrorCode());
		}

		return exception.getMessage();
	}

	@Override
	public boolean isAuthenticationException(SQLException exception) {
		return requireNonNull(exception).getErrorCode() == LOGIN_CREDS_ERROR;
	}

	@Override
	public boolean isReferentialIntegrityException(SQLException exception) {
		return requireNonNull(exception).getErrorCode() == CHILD_RECORD_ERROR || exception.getErrorCode() == INTEGRITY_CONSTRAINT_ERROR;
	}

	@Override
	public boolean isUniqueConstraintException(SQLException exception) {
		return requireNonNull(exception).getErrorCode() == UNIQUE_KEY_ERROR;
	}

	@Override
	public boolean isTimeoutException(SQLException exception) {
		return requireNonNull(exception).getErrorCode() == TIMEOUT_ERROR;
	}
}
