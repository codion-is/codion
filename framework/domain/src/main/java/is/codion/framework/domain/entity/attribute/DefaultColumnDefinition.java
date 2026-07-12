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

import is.codion.common.db.database.Database;
import is.codion.common.db.database.GetValue;
import is.codion.common.db.database.SetValue;
import is.codion.framework.domain.entity.attribute.Column.Converter;
import is.codion.framework.domain.entity.attribute.Column.Generator;

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

import static java.util.Objects.requireNonNull;

final class DefaultColumnDefinition<T> extends AbstractValueAttributeDefinition<T> implements ColumnDefinition<T> {

	@Serial
	private static final long serialVersionUID = 1;

	private static final Converter<Object, Object> DEFAULT_CONVERTER = new DefaultConverter();
	private static final Map<Class<?>, Integer> TYPE_MAP = createTypeMap();

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
	private final transient Converter<T, Object> converter;
	private final transient @Nullable Generator<T> generator;

	private transient @Nullable GetValue<Object> getValue;
	private transient @Nullable SetValue<Object> setValue;

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
		this.setValue = builder.setValue;
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
	public @Nullable T get(ResultSet resultSet, int index, Database database) throws SQLException {
		if (getValue == null) {
			getValue = (GetValue<Object>) database.getter(type);
		}
		Object value = getValue.get(resultSet, index);
		if (value != null || converter.handlesNull()) {
			return converter.fromColumn(value);
		}

		return null;
	}

	@Override
	public @Nullable T get(ResultSet resultSet, Database database) throws SQLException {
		return get(resultSet, resultSet.findColumn(name), database);
	}

	@Override
	public void set(PreparedStatement statement, int index, @Nullable T value, Database database) throws SQLException {
		if (setValue == null) {
			setValue = (SetValue<Object>) database.setter(type);
		}
		setValue.set(statement, index, columnValue(value, statement));
	}

	private @Nullable Object columnValue(@Nullable T value, PreparedStatement statement) throws SQLException {
		if (value != null || converter.handlesNull()) {
			return converter.toColumn(value, statement);
		}

		return null;
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
		private @Nullable GetValue<Object> getValue;
		private @Nullable SetValue<Object> setValue;
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
			this.setValue = null;
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
			this.setValue = null;
			return self();
		}

		@Override
		public final <C> B converter(Class<C> columnClass, Converter<T, C> converter,
																 GetValue<C> getValue) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getValue = (GetValue<Object>) requireNonNull(getValue);
			this.setValue = null;
			return self();
		}

		@Override
		public final <C> B converter(Class<C> columnClass, Converter<T, C> converter,
																 SetValue<C> setValue) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getValue = getter(this.type, (Column<Object>) super.attribute());
			this.setValue = (SetValue<Object>) requireNonNull(setValue);
			return self();
		}

		@Override
		public final <C> B converter(Class<C> columnClass, Converter<T, C> converter,
																 GetValue<C> getValue, SetValue<C> setValue) {
			this.type = sqlType(columnClass);
			this.converter = (Converter<T, Object>) requireNonNull(converter);
			this.getValue = (GetValue<Object>) requireNonNull(getValue);
			this.setValue = (SetValue<Object>) requireNonNull(setValue);
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

		// A column-specific getter for the raw column value, or null to resolve the default getter for the SQL type
		// from the Database at read time. Types.OTHER reads via getObject(index, valueClass), which needs the value class.
		private static <T> @Nullable GetValue<T> getter(int columnType, Column<T> column) {
			if (columnType == Types.OTHER) {
				return (GetValue<T>) new GetObject(column.type().valueClass());
			}

			return null;
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
