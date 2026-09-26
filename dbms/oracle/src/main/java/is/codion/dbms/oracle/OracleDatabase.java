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
package is.codion.dbms.oracle;

import is.codion.common.db.database.AbstractDatabase;
import is.codion.common.db.database.ClientInfo;
import is.codion.common.db.database.SetValue;
import is.codion.common.db.exception.ErrorType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the Oracle database.
 */
final class OracleDatabase extends AbstractDatabase {

	private static final String CLIENT_IDENTIFIER = "OCSID.CLIENTID";
	private static final String MODULE = "OCSID.MODULE";
	private static final Pattern DESCRIPTOR_NAME = Pattern.compile("\\(\\s*(?:SERVICE_NAME|SID)\\s*=\\s*([^)\\s]+)", Pattern.CASE_INSENSITIVE);

	private static final String DOCUMENTATION_LINK = "https://docs.oracle.com/error-help";
	private static final int MAXIMUM_STATEMENT_PARAMETERS = 65_535;
	private static final SetValue<Boolean> SET_BOOLEAN = new SetBoolean();

	private static final Map<Integer, ErrorType> ERROR_TYPES = new HashMap<>();

	static {
		ERROR_TYPES.put(1, ErrorType.UNIQUE_CONSTRAINT);
		ERROR_TYPES.put(2291, ErrorType.PARENT_MISSING);
		ERROR_TYPES.put(2292, ErrorType.CHILD_EXISTS);
		ERROR_TYPES.put(1400, ErrorType.NULL_VALUE);// cannot insert NULL
		ERROR_TYPES.put(1407, ErrorType.NULL_VALUE);// cannot update to NULL
		ERROR_TYPES.put(2290, ErrorType.CHECK_CONSTRAINT);
		ERROR_TYPES.put(12899, ErrorType.VALUE_TOO_LARGE);// value too large for column
		ERROR_TYPES.put(1438, ErrorType.VALUE_TOO_LARGE);// value larger than specified precision
		ERROR_TYPES.put(1031, ErrorType.MISSING_PRIVILEGES);
		ERROR_TYPES.put(1045, ErrorType.MISSING_PRIVILEGES);// user lacks CREATE SESSION privilege
		ERROR_TYPES.put(1017, ErrorType.AUTHENTICATION);
		ERROR_TYPES.put(28000, ErrorType.ACCOUNT_LOCKED);
		ERROR_TYPES.put(28001, ErrorType.PASSWORD_EXPIRED);
		ERROR_TYPES.put(54, ErrorType.ROW_LOCKED);// resource busy and acquire with NOWAIT specified
		ERROR_TYPES.put(942, ErrorType.TABLE_NOT_FOUND);
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
		// what follows the @, being preceded by the driver type and optionally the credentials
		String name = url().substring(url().indexOf('@') + 1);
		if (name.startsWith("(")) {
			// (DESCRIPTION=(ADDRESS=...)(CONNECT_DATA=(SERVICE_NAME=service)))
			Matcher matcher = DESCRIPTOR_NAME.matcher(name);

			return matcher.find() ? matcher.group(1) : name;
		}
		name = removeUrlPrefixOptionsAndParameters(name);
		if (name.contains("/")) {
			// //host:port/service:server mode
			name = name.substring(name.lastIndexOf('/') + 1);

			return name.contains(":") ? name.substring(0, name.indexOf(':')) : name;
		}

		// host:port:sid or a tns alias
		return name.substring(name.lastIndexOf(':') + 1);
	}

	@Override
	public String autoIncrementQuery(String idSource) {
		return "SELECT " + requireNonNull(idSource) + ".CURRVAL FROM DUAL";
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		return "SELECT " + requireNonNull(sequenceName) + ".NEXTVAL FROM DUAL";
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
		return createOffsetFetchNextClause(limit, offset);
	}

	@Override
	public int maximumParameters() {
		return MAXIMUM_STATEMENT_PARAMETERS;
	}

	/**
	 * Boolean nulls are bound as {@link Types#BIT}, since drivers before 23 do not support {@link Types#BOOLEAN},
	 * ojdbc11 21.9 failing with {@code Invalid column type: 16}.
	 */
	@Override
	public SetValue<?> setter(int sqlType) {
		return sqlType == Types.BOOLEAN ? SET_BOOLEAN : super.setter(sqlType);
	}

	/**
	 * A query timeout is reported as {@code ORA-01013: user requested cancel of current operation},
	 * which is not mapped, the driver throwing a {@link java.sql.SQLTimeoutException} for a timeout only.
	 */
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
				// ORA-01400: cannot insert NULL into ("SCHEMA"."TABLE"."COLUMN")
				// ORA-01407: cannot update ("SCHEMA"."TABLE"."COLUMN") to NULL
				return lastQuoted(message, "\")");
			case VALUE_TOO_LARGE:
				// ORA-12899: value too large for column "SCHEMA"."TABLE"."COLUMN" (actual: 16, maximum: 10)
				return lastQuoted(message, "\" (");
			case UNIQUE_CONSTRAINT:
				// ORA-00001: unique constraint (SCHEMA.CONSTRAINT) violated
			case PARENT_MISSING:
				// ORA-02291: integrity constraint (SCHEMA.CONSTRAINT) violated - parent key not found
			case CHILD_EXISTS:
				// ORA-02292: integrity constraint (SCHEMA.CONSTRAINT) violated - child record found
			case CHECK_CONSTRAINT:
				// ORA-02290: check constraint (SCHEMA.CONSTRAINT) violated
				return constraint(message);
			default:
				return null;
		}
	}

	/**
	 * The driver appends a link to the documentation of the error in question
	 */
	@Override
	protected String message(SQLException exception) {
		String message = exception.getMessage();
		if (message == null) {
			return null;
		}
		int linkIndex = message.indexOf(DOCUMENTATION_LINK);

		return linkIndex == -1 ? message : message.substring(0, linkIndex).trim();
	}

	/**
	 * <p>The client identifier is what auditing reads, via
	 * {@code SYS_CONTEXT('USERENV', 'CLIENT_IDENTIFIER')}, and it shows up in {@code V$SESSION.CLIENT_IDENTIFIER}
	 * along with the module. The host needs no stamping, the driver reporting it as {@code V$SESSION.MACHINE}.
	 * <p>The driver rejects the standard JDBC property names outright, with {@code ORA-17253}, and declares
	 * none of its own, so these {@code OCSID} names are the only way in. It sends them with the next
	 * statement rather than on their own, so the stamp costs no round trip.
	 * <p>Verified against ojdbc11 23.7.0.25.01 and the {@code gvenzl/oracle-xe} image, 2026-08-22.
	 */
	@Override
	public void clientInfo(Connection connection, ClientInfo clientInfo) {
		clientInfoProperty(connection, CLIENT_IDENTIFIER, clientInfo.user());
		clientInfoProperty(connection, MODULE, clientInfo.clientType());
	}

	/**
	 * @return the quoted text ending at the given suffix, which starts with the closing quote, null if not found
	 */
	private static String constraint(String message) {
		String constraint = between(message, "constraint (", ")");

		return constraint == null ? null : constraint.substring(constraint.lastIndexOf('.') + 1);
	}

	private static final class SetBoolean implements SetValue<Boolean> {

		@Override
		public void set(PreparedStatement statement, int index, Boolean value) throws SQLException {
			if (value == null) {
				statement.setNull(index, Types.BIT);
			}
			else {
				statement.setBoolean(index, value);
			}
		}
	}

	private static String lastQuoted(String message, String suffix) {
		int endIndex = message.indexOf(suffix);
		if (endIndex == -1) {
			return null;
		}
		int beginIndex = message.lastIndexOf('"', endIndex - 1);

		return beginIndex == -1 ? null : message.substring(beginIndex + 1, endIndex);
	}
}
