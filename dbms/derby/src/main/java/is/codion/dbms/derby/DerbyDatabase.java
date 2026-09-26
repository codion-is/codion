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
import is.codion.common.db.database.GetValue;
import is.codion.common.db.database.SetValue;
import is.codion.common.db.exception.ErrorType;

import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

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

	private static final Map<Integer, GetValue<?>> GETTERS = Map.of(
					Types.DATE, new GetLocalDate(),
					Types.TIME, new GetLocalTime(),
					Types.TIMESTAMP, new GetLocalDateTime());
	private static final Map<Integer, SetValue<?>> SETTERS = Map.of(
					Types.DATE, new SetLocalDate(),
					Types.TIME, new SetLocalTime(),
					Types.TIMESTAMP, new SetLocalDateTime());

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
	 * Derby does not support java.time, the temporal values are read via {@link Date}, {@link Time} and {@link Timestamp}.
	 * <p>Note that Derby interprets timestamps in the default time zone, a local date time falling into a daylight
	 * saving gap being moved forward, whichever way it is set.
	 */
	@Override
	public GetValue<?> getter(int sqlType) {
		GetValue<?> getter = GETTERS.get(sqlType);

		return getter == null ? super.getter(sqlType) : getter;
	}

	/**
	 * Derby does not support java.time, the temporal values are set via {@link Date}, {@link Time} and {@link Timestamp}.
	 */
	@Override
	public SetValue<?> setter(int sqlType) {
		SetValue<?> setter = SETTERS.get(sqlType);

		return setter == null ? super.setter(sqlType) : setter;
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
		if (message == null) {
			return null;
		}
		switch (errorType) {
			case NULL_VALUE:
				// Column 'NAME'  cannot accept a NULL value.
				return between(message, "'", "'");
			case UNIQUE_CONSTRAINT:
				// ... a duplicate key value in a unique or primary key constraint or unique index identified by 'PARENT_UK' defined on 'PARENT'.
				return between(message, "identified by '", "'");
			case REFERENTIAL_INTEGRITY:
				// DELETE on table 'PARENT' caused a violation of foreign key constraint 'CHILD_FK' for key (1).
			case CHECK_CONSTRAINT:
				// The check constraint 'PARENT_CK' was violated while performing an INSERT or UPDATE on table '"APP"."PARENT"'.
				return between(message, "constraint '", "'");
			default:
				return null;
		}
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

	private static final class GetLocalDate implements GetValue<LocalDate> {

		@Override
		public LocalDate get(ResultSet resultSet, int index) throws SQLException {
			Date date = resultSet.getDate(index);

			return date == null ? null : date.toLocalDate();
		}
	}

	private static final class GetLocalTime implements GetValue<LocalTime> {

		@Override
		public LocalTime get(ResultSet resultSet, int index) throws SQLException {
			Time time = resultSet.getTime(index);

			return time == null ? null : time.toLocalTime();
		}
	}

	private static final class GetLocalDateTime implements GetValue<LocalDateTime> {

		@Override
		public LocalDateTime get(ResultSet resultSet, int index) throws SQLException {
			Timestamp timestamp = resultSet.getTimestamp(index);

			return timestamp == null ? null : timestamp.toLocalDateTime();
		}
	}

	private static final class SetLocalDate implements SetValue<LocalDate> {

		@Override
		public void set(PreparedStatement statement, int index, LocalDate value) throws SQLException {
			statement.setDate(index, value == null ? null : Date.valueOf(value));
		}
	}

	private static final class SetLocalTime implements SetValue<LocalTime> {

		@Override
		public void set(PreparedStatement statement, int index, LocalTime value) throws SQLException {
			statement.setTime(index, value == null ? null : Time.valueOf(value));
		}
	}

	private static final class SetLocalDateTime implements SetValue<LocalDateTime> {

		@Override
		public void set(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
			statement.setTimestamp(index, value == null ? null : Timestamp.valueOf(value));
		}
	}
}
