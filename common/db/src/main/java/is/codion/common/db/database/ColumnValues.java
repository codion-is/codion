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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.database;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.HashMap;
import java.util.Map;

import static is.codion.common.utilities.Text.nullOrEmpty;

/**
 * Provides the default {@link GetValue}/{@link SetValue} instances, keyed by {@link Types SQL type}.
 */
final class ColumnValues {

	private ColumnValues() {}

	static Map<Integer, GetValue<?>> getters(boolean legacy) {
		Map<Integer, GetValue<?>> getters = new HashMap<>();
		getters.put(Types.SMALLINT, new GetShort());
		getters.put(Types.INTEGER, new GetInteger());
		getters.put(Types.BIGINT, new GetLong());
		getters.put(Types.DOUBLE, new GetDouble());
		getters.put(Types.DECIMAL, new GetBigDecimal());
		getters.put(Types.DATE, legacy ? new GetLocalDateLegacy() : new GetLocalDate());
		getters.put(Types.TIMESTAMP, legacy ? new GetLocalDateTimeLegacy() : new GetLocalDateTime());
		getters.put(Types.TIME, legacy ? new GetLocalTimeLegacy() : new GetLocalTime());
		getters.put(Types.TIMESTAMP_WITH_TIMEZONE, legacy ? new GetOffsetDateTimeLegacy() : new GetOffsetDateTime());
		getters.put(Types.TIME_WITH_TIMEZONE, legacy ? new GetOffsetTimeLegacy() : new GetOffsetTime());
		getters.put(Types.VARCHAR, new GetString());
		getters.put(Types.BOOLEAN, new GetBoolean());
		getters.put(Types.CHAR, new GetCharacter());
		getters.put(Types.BLOB, new GetByteArray());

		return getters;
	}

	// Typed setters rather than setObject(index, value, type), which leaves converting the value to the
	// given type up to the driver, and drivers differ in which conversions they support. The temporal
	// types and OTHER use setObject(index, value), the driver inferring the type from the value.
	static Map<Integer, SetValue<?>> setters() {
		Map<Integer, SetValue<?>> setters = new HashMap<>();
		setters.put(Types.SMALLINT, new SetShort());
		setters.put(Types.INTEGER, new SetInteger());
		setters.put(Types.BIGINT, new SetLong());
		setters.put(Types.DOUBLE, new SetDouble());
		setters.put(Types.DECIMAL, new SetBigDecimal());
		setters.put(Types.VARCHAR, new SetString());
		setters.put(Types.CHAR, new SetCharacter());
		setters.put(Types.BOOLEAN, new SetBoolean());
		setters.put(Types.BLOB, new SetByteArray());
		int[] objectTypes = {Types.DATE, Types.TIME, Types.TIMESTAMP, Types.TIME_WITH_TIMEZONE,
						Types.TIMESTAMP_WITH_TIMEZONE, Types.OTHER};
		for (int type : objectTypes) {
			setters.put(type, new SetObject(type));
		}

		return setters;
	}

	private abstract static class AbstractSetValue<T> implements SetValue<T> {

		private final int nullType;

		private AbstractSetValue(int nullType) {
			this.nullType = nullType;
		}

		@Override
		public final void set(PreparedStatement statement, int index, @Nullable T value) throws SQLException {
			if (value == null) {
				setNull(statement, index);
			}
			else {
				setValue(statement, index, value);
			}
		}

		protected void setNull(PreparedStatement statement, int index) throws SQLException {
			statement.setNull(index, nullType);
		}

		protected abstract void setValue(PreparedStatement statement, int index, T value) throws SQLException;
	}

	private static final class SetShort extends AbstractSetValue<Short> {

		private SetShort() {
			super(Types.SMALLINT);
		}

		@Override
		protected void setValue(PreparedStatement statement, int index, Short value) throws SQLException {
			statement.setShort(index, value);
		}
	}

	private static final class SetInteger extends AbstractSetValue<Integer> {

		private SetInteger() {
			super(Types.INTEGER);
		}

		@Override
		protected void setValue(PreparedStatement statement, int index, Integer value) throws SQLException {
			statement.setInt(index, value);
		}
	}

	private static final class SetLong extends AbstractSetValue<Long> {

		private SetLong() {
			super(Types.BIGINT);
		}

		@Override
		protected void setValue(PreparedStatement statement, int index, Long value) throws SQLException {
			statement.setLong(index, value);
		}
	}

	private static final class SetDouble extends AbstractSetValue<Double> {

		private SetDouble() {
			super(Types.DOUBLE);
		}

		@Override
		protected void setValue(PreparedStatement statement, int index, Double value) throws SQLException {
			statement.setDouble(index, value);
		}
	}

	private static final class SetBigDecimal extends AbstractSetValue<BigDecimal> {

		private SetBigDecimal() {
			super(Types.DECIMAL);
		}

		@Override
		protected void setValue(PreparedStatement statement, int index, BigDecimal value) throws SQLException {
			statement.setBigDecimal(index, value);
		}
	}

	private static final class SetString extends AbstractSetValue<String> {

		private SetString() {
			super(Types.VARCHAR);
		}

		@Override
		protected void setValue(PreparedStatement statement, int index, String value) throws SQLException {
			statement.setString(index, value);
		}
	}

	private static final class SetCharacter extends AbstractSetValue<Character> {

		private SetCharacter() {
			super(Types.CHAR);
		}

		@Override
		protected void setValue(PreparedStatement statement, int index, Character value) throws SQLException {
			statement.setString(index, value.toString());
		}
	}

	private static final class SetBoolean extends AbstractSetValue<Boolean> {

		private SetBoolean() {
			super(Types.BOOLEAN);
		}

		@Override
		protected void setValue(PreparedStatement statement, int index, Boolean value) throws SQLException {
			statement.setBoolean(index, value);
		}
	}

	private static final class SetByteArray extends AbstractSetValue<byte[]> {

		private SetByteArray() {
			super(Types.BLOB);
		}

		// Leaves the null type up to the driver, a BLOB null being rejected by a PostgreSQL bytea column
		// and a VARBINARY null by a Derby blob column
		@Override
		protected void setNull(PreparedStatement statement, int index) throws SQLException {
			statement.setBytes(index, null);
		}

		@Override
		protected void setValue(PreparedStatement statement, int index, byte[] value) throws SQLException {
			statement.setBytes(index, value);
		}
	}

	private static final class SetObject extends AbstractSetValue<Object> {

		private SetObject(int type) {
			super(type);
		}

		@Override
		protected void setValue(PreparedStatement statement, int index, Object value) throws SQLException {
			statement.setObject(index, value);
		}
	}

	private static final class GetShort implements GetValue<Short> {

		@Override
		public @Nullable Short get(ResultSet resultSet, int index) throws SQLException {
			short value = resultSet.getShort(index);

			return value == 0 && resultSet.wasNull() ? null : value;
		}
	}

	private static final class GetInteger implements GetValue<Integer> {

		@Override
		public @Nullable Integer get(ResultSet resultSet, int index) throws SQLException {
			int value = resultSet.getInt(index);

			return value == 0 && resultSet.wasNull() ? null : value;
		}
	}

	private static final class GetLong implements GetValue<Long> {

		@Override
		public @Nullable Long get(ResultSet resultSet, int index) throws SQLException {
			long value = resultSet.getLong(index);

			return value == 0L && resultSet.wasNull() ? null : value;
		}
	}

	private static final class GetDouble implements GetValue<Double> {

		@Override
		public @Nullable Double get(ResultSet resultSet, int index) throws SQLException {
			double value = resultSet.getDouble(index);

			return Double.compare(value, 0d) == 0 && resultSet.wasNull() ? null : value;
		}
	}

	private static final class GetBigDecimal implements GetValue<BigDecimal> {

		@Override
		public @Nullable BigDecimal get(ResultSet resultSet, int index) throws SQLException {
			BigDecimal value = resultSet.getBigDecimal(index);

			//strip trailing zeros to match DefaultEntity.adjustDecimalFractionDigits, which strips them on set;
			//BigDecimal equality is scale-sensitive, so the load and set paths must normalize identically
			return value == null ? null : value.stripTrailingZeros();
		}
	}

	private static final class GetString implements GetValue<String> {

		@Override
		public @Nullable String get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getString(index);
		}
	}

	private static final class GetBoolean implements GetValue<Boolean> {

		@Override
		public @Nullable Boolean get(ResultSet resultSet, int index) throws SQLException {
			boolean value = resultSet.getBoolean(index);

			return !value && resultSet.wasNull() ? null : value;
		}
	}

	private static final class GetCharacter implements GetValue<Character> {

		@Override
		public @Nullable Character get(ResultSet resultSet, int index) throws SQLException {
			String string = resultSet.getString(index);
			if (nullOrEmpty(string)) {
				return null;
			}

			return Character.valueOf(string.charAt(0));
		}
	}

	private static final class GetByteArray implements GetValue<byte[]> {

		@Override
		public @Nullable byte[] get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getBytes(index);
		}
	}

	private static final class GetLocalDate implements GetValue<LocalDate> {

		@Override
		public @Nullable LocalDate get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, LocalDate.class);
		}
	}

	private static final class GetLocalDateTime implements GetValue<LocalDateTime> {

		@Override
		public @Nullable LocalDateTime get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, LocalDateTime.class);
		}
	}

	private static final class GetOffsetDateTime implements GetValue<OffsetDateTime> {

		@Override
		public @Nullable OffsetDateTime get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, OffsetDateTime.class);
		}
	}

	private static final class GetLocalTime implements GetValue<LocalTime> {

		@Override
		public @Nullable LocalTime get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, LocalTime.class);
		}
	}

	private static final class GetOffsetTime implements GetValue<OffsetTime> {

		@Override
		public @Nullable OffsetTime get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, OffsetTime.class);
		}
	}

	// Pre-JDBC-4.2 temporal getters (Database.LEGACY_JDBC) for a platform whose java.sql predates 4.2 — notably
	// Android, whose ResultSet lacks getObject(int, Class) AND whose java.sql.Timestamp/Date/Time lack the java.time
	// bridges (toLocalDateTime() etc.). We rebuild the java.time value from the deprecated java.util.Date field
	// accessors (getYear()/getMonth()/getDate()/getHours()/... plus Timestamp.getNanos()), which ARE present on
	// Android — this is exactly what the missing toLocalDate()/toLocalDateTime()/toLocalTime() bridges do internally.
	// The Offset types have no such field accessors, so they fall back to getObject(int) + cast (best-effort; a modern
	// driver returns the java.time type directly).

	private static final class GetLocalDateLegacy implements GetValue<LocalDate> {

		@Override
		public @Nullable LocalDate get(ResultSet resultSet, int index) throws SQLException {
			java.sql.Date value = resultSet.getDate(index);

			return value == null ? null : LocalDate.of(value.getYear() + 1900, value.getMonth() + 1, value.getDate());
		}
	}

	private static final class GetLocalDateTimeLegacy implements GetValue<LocalDateTime> {

		@Override
		public @Nullable LocalDateTime get(ResultSet resultSet, int index) throws SQLException {
			Timestamp value = resultSet.getTimestamp(index);

			return value == null ? null : LocalDateTime.of(value.getYear() + 1900, value.getMonth() + 1, value.getDate(),
							value.getHours(), value.getMinutes(), value.getSeconds(), value.getNanos());
		}
	}

	private static final class GetLocalTimeLegacy implements GetValue<LocalTime> {

		@Override
		public @Nullable LocalTime get(ResultSet resultSet, int index) throws SQLException {
			Time value = resultSet.getTime(index);

			return value == null ? null : LocalTime.of(value.getHours(), value.getMinutes(), value.getSeconds());
		}
	}

	private static final class GetOffsetDateTimeLegacy implements GetValue<OffsetDateTime> {

		@Override
		public @Nullable OffsetDateTime get(ResultSet resultSet, int index) throws SQLException {
			return (OffsetDateTime) resultSet.getObject(index);
		}
	}

	private static final class GetOffsetTimeLegacy implements GetValue<OffsetTime> {

		@Override
		public @Nullable OffsetTime get(ResultSet resultSet, int index) throws SQLException {
			return (OffsetTime) resultSet.getObject(index);
		}
	}
}
