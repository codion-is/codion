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
import is.codion.common.db.database.GetValue;
import is.codion.common.db.database.SetValue;
import is.codion.common.db.exception.ErrorType;

import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.UUID;

/**
 * A SQLite embedded database implementation, quite experimental, based on the xerial/sqlite-jdbc driver.
 */
final class SQLiteDatabase extends AbstractDatabase {

	private static final String AUTO_INCREMENT_QUERY = "SELECT LAST_INSERT_ROWID()";
	// SQLite's own format, as its date and time functions write it, a fraction of a second only where there is one
	private static final DateTimeFormatter TIME_FORMAT = new DateTimeFormatterBuilder()
					.appendPattern("HH:mm:ss")
					.appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
					.toFormatter();
	private static final DateTimeFormatter DATE_TIME_FORMAT = new DateTimeFormatterBuilder()
					.append(DateTimeFormatter.ISO_LOCAL_DATE)
					.appendLiteral(' ')
					.append(TIME_FORMAT)
					.toFormatter();
	private static final SetValue<LocalDate> SET_LOCAL_DATE = new SetTemporal<>(Types.DATE, DateTimeFormatter.ISO_LOCAL_DATE);
	private static final SetValue<LocalTime> SET_LOCAL_TIME = new SetTemporal<>(Types.TIME, TIME_FORMAT);
	private static final SetValue<LocalDateTime> SET_LOCAL_DATE_TIME = new SetTemporal<>(Types.TIMESTAMP, DATE_TIME_FORMAT);
	private static final GetValue<LocalDate> GET_LOCAL_DATE = new GetLocalDate();
	private static final GetValue<LocalTime> GET_LOCAL_TIME = new GetLocalTime();
	private static final GetValue<LocalDateTime> GET_LOCAL_DATE_TIME = new GetLocalDateTime();
	private static final GetValue<OffsetDateTime> GET_OFFSET_DATE_TIME = new GetOffsetDateTime();
	private static final GetValue<OffsetTime> GET_OFFSET_TIME = new GetOffsetTime();
	private static final GetValue<UUID> GET_UUID = new GetUUID();
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
	private static final String CHECK_FAILED = "CHECK constraint failed: ";

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
	public String selectForUpdateClause(Lock lock) {
		return "";
	}

	@Override
	public String limitOffsetClause(@Nullable Integer limit, @Nullable Integer offset, boolean ordered) {
		return createLimitOffsetClause(limit, offset, NO_LIMIT);
	}

	/**
	 * Temporal values are parsed from the text they are stored as, ISO or SQLite's own format with a space separating
	 * the date and time, the one {@link #setter(int)} writes {@link LocalDate}, {@link LocalTime} and
	 * {@link LocalDateTime} values in, with or without {@link #LEGACY_JDBC}.
	 * The driver does not support reading {@link OffsetDateTime} and {@link OffsetTime} via {@code getObject(index, type)},
	 * and its {@code getDate()}, {@code getTime()} and {@code getTimestamp()}, which {@link #LEGACY_JDBC} reads
	 * {@link LocalDate}, {@link LocalTime} and {@link LocalDateTime} with, parse its own format only, not ISO text.
	 */
	@Override
	public GetValue<?> getter(int sqlType) {
		switch (sqlType) {
			case Types.DATE:
				return GET_LOCAL_DATE;
			case Types.TIME:
				return GET_LOCAL_TIME;
			case Types.TIMESTAMP:
				return GET_LOCAL_DATE_TIME;
			case Types.TIMESTAMP_WITH_TIMEZONE:
				return GET_OFFSET_DATE_TIME;
			case Types.TIME_WITH_TIMEZONE:
				return GET_OFFSET_TIME;
			default:
				return super.getter(sqlType);
		}
	}

	/**
	 * {@link LocalDate}, {@link LocalTime} and {@link LocalDateTime} values are written as text in SQLite's own format,
	 * as its date and time functions write them, {@code current_timestamp} for example: {@code yyyy-MM-dd},
	 * {@code HH:mm:ss} and {@code yyyy-MM-dd HH:mm:ss}, a fraction of a second following where there is one, without
	 * trailing zeros. A value is then always the same text, which orders as the values do, so that conditions,
	 * comparing the text in SQL, compare the values, those written by SQLite's functions or in a script in the same
	 * format included.
	 */
	@Override
	public SetValue<?> setter(int sqlType) {
		switch (sqlType) {
			case Types.DATE:
				return SET_LOCAL_DATE;
			case Types.TIME:
				return SET_LOCAL_TIME;
			case Types.TIMESTAMP:
				return SET_LOCAL_DATE_TIME;
			default:
				return super.setter(sqlType);
		}
	}

	/**
	 * The driver neither supports reading a {@link UUID} via {@code getObject(index, UUID.class)}, nor returns one from
	 * {@code getObject(index)}, with {@link #LEGACY_JDBC}. A {@link UUID} is therefore parsed from the text the driver
	 * stores it as, its {@link UUID#toString()}, written via {@code setObject()}.
	 */
	@Override
	public <T> GetValue<T> getter(Class<T> type) {
		if (type == UUID.class) {
			return (GetValue<T>) GET_UUID;
		}

		return super.getter(type);
	}

	@Override
	protected @Nullable ErrorType errorType(SQLException exception) {
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
	protected @Nullable String errorDetail(SQLException exception, ErrorType errorType) {
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
		if (errorType == ErrorType.CHECK_CONSTRAINT && message != null) {
			// [SQLITE_CONSTRAINT_CHECK] A CHECK constraint failed (CHECK constraint failed: parent_ck)
			return between(message, CHECK_FAILED, ")");
		}

		return null;
	}

	private static final class GetOffsetDateTime implements GetValue<OffsetDateTime> {

		@Override
		public @Nullable OffsetDateTime get(ResultSet resultSet, int index) throws SQLException {
			String value = resultSet.getString(index);

			return value == null ? null : OffsetDateTime.parse(value.replace(' ', 'T'));
		}
	}

	private static final class SetTemporal<T extends TemporalAccessor> implements SetValue<T> {

		private final int sqlType;
		private final DateTimeFormatter formatter;

		private SetTemporal(int sqlType, DateTimeFormatter formatter) {
			this.sqlType = sqlType;
			this.formatter = formatter;
		}

		@Override
		public void set(PreparedStatement statement, int index, @Nullable T value) throws SQLException {
			if (value == null) {
				statement.setNull(index, sqlType);
			}
			else {
				statement.setString(index, formatter.format(value));
			}
		}
	}

	private static final class GetLocalDate implements GetValue<LocalDate> {

		@Override
		public @Nullable LocalDate get(ResultSet resultSet, int index) throws SQLException {
			String value = resultSet.getString(index);

			return value == null ? null : LocalDate.parse(value);
		}
	}

	private static final class GetLocalTime implements GetValue<LocalTime> {

		@Override
		public @Nullable LocalTime get(ResultSet resultSet, int index) throws SQLException {
			String value = resultSet.getString(index);

			return value == null ? null : LocalTime.parse(value);
		}
	}

	private static final class GetLocalDateTime implements GetValue<LocalDateTime> {

		@Override
		public @Nullable LocalDateTime get(ResultSet resultSet, int index) throws SQLException {
			String value = resultSet.getString(index);

			return value == null ? null : LocalDateTime.parse(value.replace(' ', 'T'));
		}
	}

	private static final class GetUUID implements GetValue<UUID> {

		@Override
		public @Nullable UUID get(ResultSet resultSet, int index) throws SQLException {
			String value = resultSet.getString(index);

			return value == null ? null : UUID.fromString(value);
		}
	}

	private static final class GetOffsetTime implements GetValue<OffsetTime> {

		@Override
		public @Nullable OffsetTime get(ResultSet resultSet, int index) throws SQLException {
			String value = resultSet.getString(index);

			return value == null ? null : OffsetTime.parse(value);
		}
	}
}
