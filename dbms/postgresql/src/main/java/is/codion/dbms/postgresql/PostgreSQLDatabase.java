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
package is.codion.dbms.postgresql;

import is.codion.common.db.database.AbstractDatabase;
import is.codion.common.db.database.ClientInfo;
import is.codion.common.db.database.SetValue;
import is.codion.common.db.exception.ErrorType;

import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the PostgreSQL database.
 */
final class PostgreSQLDatabase extends AbstractDatabase {

	private static final String INVALID_PASSWORD = "28P01";
	private static final String FOREIGN_KEY_VIOLATION = "23503";
	private static final String QUERY_CANCELED = "57014";
	private static final String LOCK_NOT_AVAILABLE = "55P03";
	private static final String INSUFFICIENT_PRIVILEGE = "42501";
	private static final String UNDEFINED_TABLE = "42P01";

	private static final String APPLICATION_NAME = "ApplicationName";
	private static final String JDBC_URL_PREFIX = "jdbc:postgresql:";
	private static final int MAXIMUM_STATEMENT_PARAMETERS = 65_535;
	private static final SetValue<Object> SET_OTHER = new SetOther();

	// The messages are subject to the lc_messages server setting, only the english ones are parsed
	private static final String STILL_REFERENCED = "is still referenced from";
	private static final String NOT_PRESENT = "is not present in";
	private static final String COLUMN = "column \"";
	private static final String CONSTRAINT = "constraint \"";
	private static final String DETAIL_KEY = "Detail: Key ";
	private static final String ALREADY_EXISTS = " already exists.";

	PostgreSQLDatabase(String url) {
		super(url);
	}

	@Override
	public String name() {
		// jdbc:postgresql://host:port/database or jdbc:postgresql:database
		return databaseOrHost(removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX));
	}

	@Override
	public String selectForUpdateClause(Lock lock) {
		return lock == Lock.NOWAIT ? FOR_UPDATE_NOWAIT : FOR_UPDATE;
	}

	@Override
	public String limitOffsetClause(Integer limit, Integer offset, boolean ordered) {
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

	/**
	 * <p>The driver honours {@code ApplicationName} alone, so the user is folded into it, landing in
	 * {@code application_name}: visible in {@code pg_stat_activity} and readable from a trigger via
	 * {@code current_setting('application_name')}.
	 * <p>Note that this driver issues a {@code SET application_name} of its own, unlike the ones which send
	 * client info along with the next statement, so the stamp costs a round trip per connection check out.
	 * It also accepts unknown property names and silently discards them, so a mistake here reports nothing.
	 * <p>Verified against pgjdbc 42.7.11 and PostgreSQL 18.4, 2026-08-22.
	 */
	@Override
	public void clientInfo(Connection connection, ClientInfo clientInfo) {
		clientInfoProperty(connection, APPLICATION_NAME, clientInfo.clientType() + " (" + clientInfo.user() + ")");
	}

	@Override
	public int maximumParameters() {
		return MAXIMUM_STATEMENT_PARAMETERS;
	}

	/**
	 * Values of {@link Types#OTHER} are bound with that type, the driver binding a value of a class it does not
	 * know as its string representation, for the database to convert, a PostGIS geometry for example, which the
	 * default {@code setObject(index, value)} rejects with {@code Can't infer the SQL type}.
	 */
	@Override
	public SetValue<?> setter(int sqlType) {
		return sqlType == Types.OTHER ? SET_OTHER : super.setter(sqlType);
	}

	@Override
	protected ErrorType errorType(SQLException exception) {
		String sqlState = exception.getSQLState();
		if (sqlState == null) {
			return super.errorType(exception);
		}
		switch (sqlState) {
			case FOREIGN_KEY_VIOLATION:
				return foreignKeyViolation(exception);
			case INVALID_PASSWORD:
				return ErrorType.AUTHENTICATION;
			case QUERY_CANCELED:
				return ErrorType.TIMEOUT;
			case LOCK_NOT_AVAILABLE:
				return ErrorType.ROW_LOCKED;
			case INSUFFICIENT_PRIVILEGE:
				return ErrorType.MISSING_PRIVILEGES;
			case UNDEFINED_TABLE:
				return ErrorType.TABLE_NOT_FOUND;
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
				//null value in column "column_name" of relation "table_name" violates not-null constraint
				return between(message, COLUMN, "\"");
			case UNIQUE_CONSTRAINT:
				//Detail: Key (col1, col2)=(val1, val2) already exists.
				return between(message, DETAIL_KEY, ALREADY_EXISTS);
			case REFERENTIAL_INTEGRITY:
			case PARENT_MISSING:
			case CHILD_EXISTS:
				//insert or update on table "child" violates foreign key constraint "child_fk"
				//update or delete on table "parent" violates foreign key constraint "child_fk" on table "child"
			case CHECK_CONSTRAINT:
				//new row for relation "table_name" violates check constraint "table_name_ck"
				return between(message, CONSTRAINT, "\"");
			default:
				return null;
		}
	}

	/**
	 * The same state is reported whether the referenced row is missing or the row being updated or deleted
	 * is referenced, the detail telling the two apart.
	 */
	private static ErrorType foreignKeyViolation(SQLException exception) {
		String message = exception.getMessage();
		if (message != null) {
			if (message.contains(STILL_REFERENCED)) {
				return ErrorType.CHILD_EXISTS;
			}
			if (message.contains(NOT_PRESENT)) {
				return ErrorType.PARENT_MISSING;
			}
		}

		return ErrorType.REFERENTIAL_INTEGRITY;
	}

	private static final class SetOther implements SetValue<Object> {

		@Override
		public void set(PreparedStatement statement, int index, @Nullable Object value) throws SQLException {
			if (value == null) {
				// an untyped null, as by default
				statement.setObject(index, null);
			}
			else {
				statement.setObject(index, value, Types.OTHER);
			}
		}
	}
}
