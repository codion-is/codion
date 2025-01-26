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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.attribute.Column.Converter;
import is.codion.framework.domain.entity.attribute.Column.Getter;
import is.codion.framework.domain.entity.attribute.Column.Setter;

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

import static is.codion.common.Text.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultColumnDefinition<T> extends AbstractAttributeDefinition<T> implements ColumnDefinition<T> {

	@Serial
	private static final long serialVersionUID = 1;

	private static final Converter<Object, Object> DEFAULT_CONVERTER = new DefaultConverter();
	private static final Map<Class<?>, Integer> TYPE_MAP = createTypeMap();
	private static final Map<Integer, Getter<?>> GETTERS = createGetters();

	private final int type;
	private final int primaryKeyIndex;
	private final boolean columnHasDefaultValue;
	private final boolean insertable;
	private final boolean updatable;
	private final boolean searchable;
	private final boolean groupBy;
	private final boolean aggregate;
	private final boolean selectable;
	private final boolean lazy;

	private final transient String name;
	private final transient String expression;
	private final transient Getter<Object> getter;
	private final transient Setter<Object> setter;
	private final transient Converter<T, Object> converter;

	protected DefaultColumnDefinition(DefaultColumnDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.type = builder.type;
		this.primaryKeyIndex = builder.primaryKeyIndex;
		this.columnHasDefaultValue = builder.columnHasDefaultValue;
		this.insertable = builder.insertable;
		this.updatable = builder.updatable;
		this.searchable = builder.searchable;
		this.name = builder.name;
		this.expression = builder.expression == null ? builder.name : builder.expression;
		this.getter = builder.getter;
		this.setter = builder.setter;
		this.converter = builder.converter;
		this.groupBy = builder.groupBy;
		this.aggregate = builder.aggregate;
		this.selectable = builder.selectable;
		this.lazy = builder.lazy;
	}

	@Override
	public final Column<T> attribute() {
		return (Column<T>) super.attribute();
	}

	@Override
	public final String name() {
		return name;
	}

	@Override
	public final String expression() {
		return expression;
	}

	@Override
	public final int type() {
		return type;
	}

	@Override
	public final <C> Converter<C, T> converter() {
		return (Converter<C, T>) converter;
	}

	@Override
	public final boolean columnHasDefaultValue() {
		return columnHasDefaultValue;
	}

	@Override
	public final boolean insertable() {
		return insertable;
	}

	@Override
	public final boolean updatable() {
		return updatable;
	}

	@Override
	public final boolean readOnly() {
		return !insertable && !updatable;
	}

	@Override
	public final int primaryKeyIndex() {
		return primaryKeyIndex;
	}

	@Override
	public final boolean groupBy() {
		return groupBy;
	}

	@Override
	public final boolean aggregate() {
		return aggregate;
	}

	@Override
	public final boolean selectable() {
		return selectable;
	}

	@Override
	public final boolean lazy() {
		return lazy;
	}

	@Override
	public final boolean primaryKey() {
		return primaryKeyIndex >= 0;
	}

	@Override
	public final boolean searchable() {
		return searchable;
	}

	@Override
	public final T get(ResultSet resultSet, int index) throws SQLException {
		Object columnValue = getter.get(resultSet, index);
		if (columnValue != null || converter.handlesNull()) {
			return converter.fromColumnValue(columnValue);
		}

		return null;
	}

	@Override
	public final void set(PreparedStatement statement, int index, T value) throws SQLException {
		setter.set(statement, index, columnValue(statement, value));
	}

	private Object columnValue(PreparedStatement statement, T value) throws SQLException {
		if (value != null || converter.handlesNull()) {
			return converter.toColumnValue(value, statement);
		}

		return null;
	}

	private static final class DefaultSetter implements Setter<Object> {

		private final int type;

		private DefaultSetter(int type) {
			this.type = type;
		}

		@Override
		public void set(PreparedStatement statement, int index, Object value) throws SQLException {
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
		public Object toColumnValue(Object value, Statement statement) {
			return value;
		}

		@Override
		public Object fromColumnValue(Object columnValue) {
			return columnValue;
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

	private static Map<Integer, Getter<?>> createGetters() {
		Map<Integer, Getter<?>> getters = new HashMap<>();
		getters.put(Types.SMALLINT, new ShortGetter());
		getters.put(Types.INTEGER, new IntegerGetter());
		getters.put(Types.BIGINT, new LongGetter());
		getters.put(Types.DOUBLE, new DoubleGetter());
		getters.put(Types.DECIMAL, new BigDecimalGetter());
		getters.put(Types.DATE, new LocalDateGetter());
		getters.put(Types.TIMESTAMP, new LocalDateTimeGetter());
		getters.put(Types.TIME, new LocalTimeGetter());
		getters.put(Types.TIMESTAMP_WITH_TIMEZONE, new OffsetDateTimeGetter());
		getters.put(Types.TIME_WITH_TIMEZONE, new OffsetTimeGetter());
		getters.put(Types.VARCHAR, new StringGetter());
		getters.put(Types.BOOLEAN, new BooleanGetter());
		getters.put(Types.CHAR, new CharacterGetter());
		getters.put(Types.BLOB, new ByteArrayGetter());

		return getters;
	}

	static class DefaultColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
					extends AbstractAttributeDefinitionBuilder<T, B> implements ColumnDefinition.Builder<T, B> {

		private final int primaryKeyIndex;

		private int type;
		private boolean columnHasDefaultValue;
		private boolean insertable;
		private boolean updatable;
		private boolean searchable;
		private String name;
		private String expression;
		private Getter<Object> getter;
		private Setter<Object> setter;
		private Converter<T, Object> converter;
		private boolean groupBy;
		private boolean aggregate;
		private boolean selectable;
		private boolean lazy;

		DefaultColumnDefinitionBuilder(Column<T> column) {
			this(column, -1);
		}

		DefaultColumnDefinitionBuilder(Column<T> column, int primaryKeyIndex) {
			super(column);
			this.primaryKeyIndex = primaryKeyIndex;
			this.type = sqlType(column.type().valueClass());
			this.columnHasDefaultValue = false;
			this.insertable = true;
			nullable(primaryKeyIndex < 0);
			this.updatable = primaryKeyIndex < 0;
			this.searchable = false;
			this.name = column.name();
			this.getter = (Getter<Object>) getter(this.type, column);
			this.setter = new DefaultSetter(this.type);
			this.converter = (Converter<T, Object>) DEFAULT_CONVERTER;
			this.groupBy = false;
			this.aggregate = false;
			this.selectable = true;
			this.lazy = false;
		}

		@Override
		public AttributeDefinition<T> build() {
			return new DefaultColumnDefinition<>(this);
		}

		@Override
		public final B nullable(boolean nullable) {
			return super.nullable(nullable);
		}

		@Override
		public final <C> B columnClass(Class<C> columnClass, Converter<T, C> converter) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getter = getter(this.type, (Column<Object>) super.attribute());
			return self();
		}

		@Override
		public final <C> B columnClass(Class<C> columnClass, Converter<T, C> converter,
																	 Getter<C> getter) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getter = (Getter<Object>) requireNonNull(getter);
			return self();
		}

		@Override
		public <C> B columnClass(Class<C> columnClass, Converter<T, C> converter,
														 Setter<C> setter) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getter = getter(this.type, (Column<Object>) super.attribute());
			this.setter = (Setter<Object>) requireNonNull(setter);
			return self();
		}

		@Override
		public <C> B columnClass(Class<C> columnClass, Converter<T, C> converter,
														 Getter<C> getter, Setter<C> setter) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getter = (Getter<Object>) requireNonNull(getter);
			this.setter = (Setter<Object>) requireNonNull(setter);
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
		public final B columnHasDefaultValue(boolean columnHasDefaultValue) {
			this.columnHasDefaultValue = columnHasDefaultValue;
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
		public final B selectable(boolean selectable) {
			this.selectable = selectable;
			return self();
		}

		@Override
		public final B lazy(boolean lazy) {
			this.lazy = lazy;
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

		private static <T> Getter<T> getter(int columnType, Column<T> column) {
			if (columnType == Types.OTHER) {
				return (Getter<T>) new ObjectGetter(column.type().valueClass());
			}
			if (!GETTERS.containsKey(columnType)) {
				throw new IllegalArgumentException("Unsupported SQL value type: " + columnType +
								", column: " + column + ", valueClass: " + column.type().valueClass());
			}

			return (Getter<T>) GETTERS.get(columnType);
		}
	}

	abstract static class AbstractReadOnlyColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
					extends DefaultColumnDefinitionBuilder<T, B> implements AttributeDefinition.Builder<T, B> {

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
					extends AbstractReadOnlyColumnDefinitionBuilder<T, B> implements AttributeDefinition.Builder<T, B> {

		DefaultSubqueryColumnDefinitionBuilder(Column<T> column, String subquery) {
			super(column);
			super.expression("(" + subquery + ")");
		}

		@Override
		public B expression(String expression) {
			throw new UnsupportedOperationException("Column expression can not be set on a subquery column: " + super.attribute());
		}
	}

	private static final class ShortGetter implements Getter<Short> {

		@Override
		public Short get(ResultSet resultSet, int index) throws SQLException {
			short value = resultSet.getShort(index);

			return value == 0 && resultSet.wasNull() ? null : value;
		}
	}

	private static final class IntegerGetter implements Getter<Integer> {

		@Override
		public Integer get(ResultSet resultSet, int index) throws SQLException {
			int value = resultSet.getInt(index);

			return value == 0 && resultSet.wasNull() ? null : value;
		}
	}

	private static final class LongGetter implements Getter<Long> {

		@Override
		public Long get(ResultSet resultSet, int index) throws SQLException {
			long value = resultSet.getLong(index);

			return value == 0L && resultSet.wasNull() ? null : value;
		}
	}

	private static final class DoubleGetter implements Getter<Double> {

		@Override
		public Double get(ResultSet resultSet, int index) throws SQLException {
			double value = resultSet.getDouble(index);

			return Double.compare(value, 0d) == 0 && resultSet.wasNull() ? null : value;
		}
	}

	private static final class BigDecimalGetter implements Getter<BigDecimal> {

		@Override
		public BigDecimal get(ResultSet resultSet, int index) throws SQLException {
			BigDecimal value = resultSet.getBigDecimal(index);

			return value == null ? null : value.stripTrailingZeros();
		}
	}

	private static final class LocalDateGetter implements Getter<LocalDate> {

		@Override
		public LocalDate get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, LocalDate.class);
		}
	}

	private static final class LocalDateTimeGetter implements Getter<LocalDateTime> {

		@Override
		public LocalDateTime get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, LocalDateTime.class);
		}
	}

	private static final class OffsetDateTimeGetter implements Getter<OffsetDateTime> {

		@Override
		public OffsetDateTime get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, OffsetDateTime.class);
		}
	}

	private static final class LocalTimeGetter implements Getter<LocalTime> {

		@Override
		public LocalTime get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, LocalTime.class);
		}
	}

	private static final class OffsetTimeGetter implements Getter<OffsetTime> {

		@Override
		public OffsetTime get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, OffsetTime.class);
		}
	}

	private static final class StringGetter implements Getter<String> {

		@Override
		public String get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getString(index);
		}
	}

	private static final class BooleanGetter implements Getter<Boolean> {

		@Override
		public Boolean get(ResultSet resultSet, int index) throws SQLException {
			boolean value = resultSet.getBoolean(index);

			return !value && resultSet.wasNull() ? null : value;
		}
	}

	private static final class CharacterGetter implements Getter<Character> {

		@Override
		public Character get(ResultSet resultSet, int index) throws SQLException {
			String string = resultSet.getString(index);
			if (nullOrEmpty(string)) {
				return null;
			}

			return Character.valueOf(string.charAt(0));
		}
	}

	private static final class ByteArrayGetter implements Getter<byte[]> {

		@Override
		public byte[] get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getBytes(index);
		}
	}

	private static final class ObjectGetter implements Getter<Object> {

		private final Class<?> valueClass;

		private ObjectGetter(Class<?> valueClass) {
			this.valueClass = valueClass;
		}

		@Override
		public Object get(ResultSet resultSet, int index) throws SQLException {
			return resultSet.getObject(index, valueClass);
		}
	}
}
