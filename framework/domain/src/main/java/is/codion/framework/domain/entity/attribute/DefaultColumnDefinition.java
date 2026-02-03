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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.attribute.Column.Converter;
import is.codion.framework.domain.entity.attribute.Column.Generator;
import is.codion.framework.domain.entity.attribute.Column.GetValue;
import is.codion.framework.domain.entity.attribute.Column.SetParameter;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.common.utilities.Text.nullOrEmpty;
import static java.util.Objects.requireNonNull;

final class DefaultColumnDefinition<T> extends AbstractValueAttributeDefinition<T> implements ColumnDefinition<T> {

	@Serial
	private static final long serialVersionUID = 1;

	private static final Converter<Object, Object> DEFAULT_CONVERTER = new DefaultConverter();
	private static final Map<Class<?>, Integer> TYPE_MAP = createTypeMap();
	private static final Map<Integer, GetValue<?>> GETTERS = createGetters();
	private static final Map<Integer, SetParameter<?>> SETTERS = new ConcurrentHashMap<>();

	private final int type;
	private final int keyIndex;
	private final boolean withDefault;
	private final boolean insertable;
	private final boolean updatable;
	private final boolean searchable;
	private final boolean groupBy;
	private final boolean aggregate;
	private final boolean selected;
	private final boolean generated;

	private final transient String name;
	private final transient String expression;
	private final transient GetValue<Object> getValue;
	private final transient SetParameter<Object> setParameter;
	private final transient Converter<T, Object> converter;
	private final transient @Nullable Generator<T> generator;

	private DefaultColumnDefinition(DefaultColumnDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.type = builder.type;
		this.keyIndex = builder.keyIndex;
		this.withDefault = builder.withDefault;
		this.insertable = builder.insertable;
		this.updatable = builder.updatable;
		this.searchable = builder.searchable;
		this.name = builder.name;
		this.expression = builder.expression == null ? builder.name : builder.expression;
		this.getValue = builder.getValue;
		this.setParameter = builder.setParameter;
		this.converter = builder.converter;
		this.generator = builder.generator;
		this.generated = builder.generator != null;
		this.groupBy = builder.groupBy;
		this.aggregate = builder.aggregate;
		this.selected = builder.selected;
	}

	@Override
	public Column<T> attribute() {
		return (Column<T>) super.attribute();
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String expression() {
		return expression;
	}

	@Override
	public int type() {
		return type;
	}

	@Override
	public boolean generated() {
		return generated;
	}

	@Override
	public Generator<T> generator() {
		if (generator == null) {
			throw new IllegalStateException("Generator is not available");
		}

		return generator;
	}

	@Override
	public <C> Converter<C, T> converter() {
		return (Converter<C, T>) converter;
	}

	@Override
	public boolean withDefault() {
		return withDefault;
	}

	@Override
	public boolean insertable() {
		return insertable;
	}

	@Override
	public boolean updatable() {
		return updatable;
	}

	@Override
	public boolean readOnly() {
		return !insertable && !updatable;
	}

	@Override
	public int keyIndex() {
		return keyIndex;
	}

	@Override
	public boolean groupBy() {
		return groupBy;
	}

	@Override
	public boolean aggregate() {
		return aggregate;
	}

	@Override
	public boolean selected() {
		return selected;
	}

	@Override
	public boolean primaryKey() {
		return keyIndex >= 0;
	}

	@Override
	public boolean searchable() {
		return searchable;
	}

	@Override
	public @Nullable T get(ResultSet resultSet, int index) throws SQLException {
		Object value = getValue.get(resultSet, index);
		if (value != null || converter.handlesNull()) {
			return converter.fromColumn(value);
		}

		return null;
	}

	@Override
	public @Nullable T get(ResultSet resultSet) throws SQLException {
		return get(resultSet, resultSet.findColumn(name));
	}

	@Override
	public void set(PreparedStatement statement, int index, @Nullable T value) throws SQLException {
		setParameter.set(statement, index, columnValue(value, statement));
	}

	private @Nullable Object columnValue(@Nullable T value, PreparedStatement statement) throws SQLException {
		if (value != null || converter.handlesNull()) {
			return converter.toColumn(value, statement);
		}

		return null;
	}

	private static final class DefaultSetParameter implements SetParameter<Object> {

		private final int type;

		private DefaultSetParameter(int type) {
			this.type = type;
		}

		@Override
		public void set(PreparedStatement statement, int index, @Nullable Object value) throws SQLException {
			if (value == null) {
				statement.setNull(index, type);
			}
			else {
				statement.setObject(index, value, type);
			}
		}
	}

	private static final class DefaultConverter implements Converter<Object, Object> {
		@Override
		public @Nullable Object toColumn(@Nullable Object value, Statement statement) {
			return value;
		}

		@Override
		public @Nullable Object fromColumn(@Nullable Object value) {
			return value;
		}
	}

	private static Map<Class<?>, Integer> createTypeMap() {
		Map<Class<?>, Integer> typeMap = new HashMap<>();
		typeMap.put(Long.class, Types.BIGINT);
		typeMap.put(Integer.class, Types.INTEGER);
		typeMap.put(Short.class, Types.SMALLINT);
		typeMap.put(Double.class, Types.DOUBLE);
		typeMap.put(BigDecimal.class, Types.DECIMAL);
		typeMap.put(LocalDate.class, Types.DATE);
		typeMap.put(LocalTime.class, Types.TIME);
		typeMap.put(LocalDateTime.class, Types.TIMESTAMP);
		typeMap.put(OffsetTime.class, Types.TIME_WITH_TIMEZONE);
		typeMap.put(OffsetDateTime.class, Types.TIMESTAMP_WITH_TIMEZONE);
		typeMap.put(java.util.Date.class, Types.DATE);
		typeMap.put(java.sql.Date.class, Types.DATE);
		typeMap.put(Time.class, Types.TIME);
		typeMap.put(Timestamp.class, Types.TIMESTAMP);
		typeMap.put(String.class, Types.VARCHAR);
		typeMap.put(Character.class, Types.CHAR);
		typeMap.put(Boolean.class, Types.BOOLEAN);
		typeMap.put(byte[].class, Types.BLOB);

		return typeMap;
	}

	private static Map<Integer, GetValue<?>> createGetters() {
		Map<Integer, GetValue<?>> getters = new HashMap<>();
		getters.put(Types.SMALLINT, new GetShort());
		getters.put(Types.INTEGER, new GetInteger());
		getters.put(Types.BIGINT, new GetLong());
		getters.put(Types.DOUBLE, new GetDouble());
		getters.put(Types.DECIMAL, new GetBigDecimal());
		getters.put(Types.DATE, new GetLocalDate());
		getters.put(Types.TIMESTAMP, new GetLocalDateTime());
		getters.put(Types.TIME, new GetLocalTime());
		getters.put(Types.TIMESTAMP_WITH_TIMEZONE, new GetOffsetDateTime());
		getters.put(Types.TIME_WITH_TIMEZONE, new GetOffsetTime());
		getters.put(Types.VARCHAR, new GetString());
		getters.put(Types.BOOLEAN, new GetBoolean());
		getters.put(Types.CHAR, new GetCharacter());
		getters.put(Types.BLOB, new GetByteArray());

		return getters;
	}

	static sealed class DefaultColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
					extends AbstractValueAttributeDefinitionBuilder<T, B> implements ColumnDefinition.Builder<T, B>
					permits AbstractReadOnlyColumnDefinitionBuilder {

		private final int keyIndex;

		private int type;
		private boolean withDefault;
		private boolean insertable;
		private boolean updatable;
		private boolean searchable;
		private String name;
		private @Nullable String expression;
		private GetValue<Object> getValue;
		private SetParameter<Object> setParameter;
		private Converter<T, Object> converter;
		private boolean groupBy;
		private boolean aggregate;
		private boolean selected;
		private @Nullable Generator<T> generator;

		DefaultColumnDefinitionBuilder(Column<T> column) {
			this(column, -1);
		}

		DefaultColumnDefinitionBuilder(Column<T> column, int keyIndex) {
			super(column, keyIndex < 0);
			this.keyIndex = keyIndex;
			this.type = sqlType(column.type().valueClass());
			this.withDefault = false;
			this.insertable = true;
			this.updatable = keyIndex < 0;
			this.searchable = false;
			this.name = column.name();
			this.getValue = (GetValue<Object>) getter(this.type, column);
			this.setParameter = (SetParameter<Object>) setter(this.type);
			this.converter = (Converter<T, Object>) DEFAULT_CONVERTER;
			this.groupBy = false;
			this.aggregate = false;
			this.selected = true;
		}

		@Override
		public final ValueAttributeDefinition<T> build() {
			return new DefaultColumnDefinition<>(this);
		}

		@Override
		public final B generator(Generator<T> generator) {
			this.generator = requireNonNull(generator);
			return self();
		}

		@Override
		public final <C> B converter(Class<C> columnClass, Converter<T, C> converter) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getValue = getter(this.type, (Column<Object>) super.attribute());
			this.setParameter = new DefaultSetParameter(this.type);
			return self();
		}

		@Override
		public final <C> B converter(Class<C> columnClass, Converter<T, C> converter,
																 GetValue<C> getValue) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getValue = (GetValue<Object>) requireNonNull(getValue);
			this.setParameter = new DefaultSetParameter(this.type);
			return self();
		}

		@Override
		public final <C> B converter(Class<C> columnClass, Converter<T, C> converter,
																 SetParameter<C> setParameter) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getValue = getter(this.type, (Column<Object>) super.attribute());
			this.setParameter = (SetParameter<Object>) requireNonNull(setParameter);
			return self();
		}

		@Override
		public final <C> B converter(Class<C> columnClass, Converter<T, C> converter,
																 GetValue<C> getValue, SetParameter<C> setParameter) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getValue = (GetValue<Object>) requireNonNull(getValue);
			this.setParameter = (SetParameter<Object>) requireNonNull(setParameter);
			return self();
		}

		@Override
		public final B name(String name) {
			this.name = requireNonNull(name);
			return self();
		}

		@Override
		public B expression(String expression) {
			this.expression = requireNonNull(expression);
			return self();
		}

		@Override
		public final B withDefault(boolean withDefault) {
			this.withDefault = withDefault;
			return self();
		}

		@Override
		public B readOnly(boolean readOnly) {
			this.insertable = !readOnly;
			this.updatable = !readOnly;
			return self();
		}

		@Override
		public B insertable(boolean insertable) {
			this.insertable = insertable;
			return self();
		}

		@Override
		public B updatable(boolean updatable) {
			this.updatable = updatable;
			return self();
		}

		@Override
		public final B groupBy(boolean groupBy) {
			this.groupBy = groupBy;
			this.aggregate = !groupBy;
			return self();
		}

		@Override
		public final B aggregate(boolean aggregate) {
			this.aggregate = aggregate;
			this.groupBy = !aggregate;
			return self();
		}

		@Override
		public final B selected(boolean selected) {
			this.selected = selected;
			return self();
		}

		@Override
		public final B searchable(boolean searchable) {
			if (searchable && !super.attribute().type().isString()) {
				throw new IllegalStateException("Searchable columns must be String based: " + super.attribute());
			}
			this.searchable = searchable;
			return self();
		}

		/**
		 * Returns the default sql type for the given class.
		 * @param clazz the class
		 * @return the corresponding sql type
		 */
		private static int sqlType(Class<?> clazz) {
			return TYPE_MAP.getOrDefault(requireNonNull(clazz), Types.OTHER);
		}

		private static <T> GetValue<T> getter(int columnType, Column<T> column) {
			if (columnType == Types.OTHER) {
				return (GetValue<T>) new GetObject(column.type().valueClass());
			}
			if (!GETTERS.containsKey(columnType)) {
				throw new IllegalArgumentException("Unsupported SQL value type: " + columnType +
								", column: " + column + ", valueClass: " + column.type().valueClass());
			}

			return (GetValue<T>) GETTERS.get(columnType);
		}

		private static SetParameter<?> setter(int columnType) {
			return SETTERS.computeIfAbsent(columnType, DefaultSetParameter::new);
		}
	}

	abstract static sealed class AbstractReadOnlyColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
					extends DefaultColumnDefinitionBuilder<T, B>
					permits DefaultSubqueryColumnDefinitionBuilder {

		protected AbstractReadOnlyColumnDefinitionBuilder(Column<T> column) {
			super(column);
			super.readOnly(true);
		}

		@Override
		public final B readOnly(boolean readOnly) {
			throw new UnsupportedOperationException("Read only by default: " + super.attribute());
		}

		@Override
		public final B insertable(boolean insertable) {
			throw new UnsupportedOperationException("Column is not insertable: " + super.attribute());
		}

		@Override
		public final B updatable(boolean updatable) {
			throw new UnsupportedOperationException("Column is not updatable: " + super.attribute());
		}
	}

	static final class DefaultSubqueryColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
					extends AbstractReadOnlyColumnDefinitionBuilder<T, B> {

		DefaultSubqueryColumnDefinitionBuilder(Column<T> column, String subquery) {
			super(column);
			super.expression("(" + subquery + ")");
		}

		@Override
		public B expression(String expression) {
			throw new UnsupportedOperationException("Column expression can not be set on a subquery column: " + super.attribute());
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

			return value == null ? null : value.stripTrailingZeros();
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

	private static final class GetObject implements GetValue<Object> {

		private final Class<?> valueClass;

		private GetObject(Class<?> valueClass) {
			this.valueClass = valueClass;
		}

		@Override
		public @Nullable Object get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, valueClass);
		}
	}
}
