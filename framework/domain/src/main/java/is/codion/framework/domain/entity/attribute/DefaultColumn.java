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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.DefaultAttribute.DefaultAttributeDefiner;
import is.codion.framework.domain.entity.condition.ColumnCondition;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Statement;
import java.util.Collection;
import java.util.Objects;

import static is.codion.framework.domain.entity.attribute.AuditAction.INSERT;
import static is.codion.framework.domain.entity.attribute.AuditAction.UPDATE;
import static is.codion.framework.domain.entity.condition.ColumnCondition.factory;
import static java.util.Objects.requireNonNull;

final class DefaultColumn<T> implements Column<T>, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final Attribute<T> attribute;

	DefaultColumn(String name, Class<T> valueClass, EntityType entityType) {
		this.attribute = new DefaultAttribute<>(name, valueClass, entityType);
	}

	@Override
	public Type<T> type() {
		return attribute.type();
	}

	@Override
	public String name() {
		return attribute.name();
	}

	@Override
	public EntityType entityType() {
		return attribute.entityType();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DefaultColumn)) {
			return false;
		}
		DefaultColumn<?> that = (DefaultColumn<?>) object;

		return attribute.equals(that.attribute);
	}

	@Override
	public int hashCode() {
		return attribute.hashCode();
	}

	@Override
	public String toString() {
		return attribute.toString();
	}

	@Override
	public Column.ColumnDefiner<T> define() {
		return new DefaultColumnDefiner<>(this);
	}

	@Override
	public ColumnCondition<T> equalTo(@Nullable T value) {
		return factory(this).equalTo(value);
	}

	@Override
	public ColumnCondition<T> notEqualTo(@Nullable T value) {
		return factory(this).notEqualTo(value);
	}

	@Override
	public ColumnCondition<String> equalToIgnoreCase(@Nullable String value) {
		return factory(this).equalToIgnoreCase(value);
	}

	@Override
	public ColumnCondition<Character> equalToIgnoreCase(@Nullable Character value) {
		return factory(this).equalToIgnoreCase(value);
	}

	@Override
	public ColumnCondition<String> notEqualToIgnoreCase(@Nullable String value) {
		return factory(this).notEqualToIgnoreCase(value);
	}

	@Override
	public ColumnCondition<Character> notEqualToIgnoreCase(@Nullable Character value) {
		return factory(this).notEqualToIgnoreCase(value);
	}

	@Override
	public ColumnCondition<String> like(@Nullable String value) {
		return factory(this).like(value);
	}

	@Override
	public ColumnCondition<String> notLike(@Nullable String value) {
		return factory(this).notLike(value);
	}

	@Override
	public ColumnCondition<String> likeIgnoreCase(@Nullable String value) {
		return factory(this).likeIgnoreCase(value);
	}

	@Override
	public ColumnCondition<String> notLikeIgnoreCase(@Nullable String value) {
		return factory(this).notLikeIgnoreCase(value);
	}

	@Override
	public ColumnCondition<T> in(T... values) {
		return factory(this).in(values);
	}

	@Override
	public ColumnCondition<T> notIn(T... values) {
		return factory(this).notIn(values);
	}

	@Override
	public ColumnCondition<T> in(Collection<? extends T> values) {
		return factory(this).in(values);
	}

	@Override
	public ColumnCondition<T> notIn(Collection<? extends T> values) {
		return factory(this).notIn(values);
	}

	@Override
	public ColumnCondition<String> inIgnoreCase(String... values) {
		return factory(this).inIgnoreCase(values);
	}

	@Override
	public ColumnCondition<String> notInIgnoreCase(String... values) {
		return factory(this).notInIgnoreCase(values);
	}

	@Override
	public ColumnCondition<String> inIgnoreCase(Collection<String> values) {
		return factory(this).inIgnoreCase(values);
	}

	@Override
	public ColumnCondition<String> notInIgnoreCase(Collection<String> values) {
		return factory(this).notInIgnoreCase(values);
	}

	@Override
	public ColumnCondition<T> lessThan(T upper) {
		return factory(this).lessThan(upper);
	}

	@Override
	public ColumnCondition<T> lessThanOrEqualTo(T upper) {
		return factory(this).lessThanOrEqualTo(upper);
	}

	@Override
	public ColumnCondition<T> greaterThan(T lower) {
		return factory(this).greaterThan(lower);
	}

	@Override
	public ColumnCondition<T> greaterThanOrEqualTo(T lower) {
		return factory(this).greaterThanOrEqualTo(lower);
	}

	@Override
	public ColumnCondition<T> betweenExclusive(T lower, T upper) {
		return factory(this).betweenExclusive(lower, upper);
	}

	@Override
	public ColumnCondition<T> between(T lower, T upper) {
		return factory(this).between(lower, upper);
	}

	@Override
	public ColumnCondition<T> notBetweenExclusive(T lower, T upper) {
		return factory(this).notBetweenExclusive(lower, upper);
	}

	@Override
	public ColumnCondition<T> notBetween(T lower, T upper) {
		return factory(this).notBetween(lower, upper);
	}

	@Override
	public ColumnCondition<T> isNull() {
		return factory(this).isNull();
	}

	@Override
	public ColumnCondition<T> isNotNull() {
		return factory(this).isNotNull();
	}

	static final class BooleanConverter<T> implements Converter<Boolean, T> {

		private final T trueValue;
		private final T falseValue;

		BooleanConverter(T trueValue, T falseValue) {
			if (Objects.equals(trueValue, falseValue)) {
				throw new IllegalArgumentException("The values representing true and false are equal: " + trueValue);
			}
			this.trueValue = requireNonNull(trueValue);
			this.falseValue = requireNonNull(falseValue);
		}

		@Override
		public Boolean fromColumnValue(T columnValue) {
			if (Objects.equals(trueValue, columnValue)) {
				return true;
			}
			if (Objects.equals(falseValue, columnValue)) {
				return false;
			}

			throw new IllegalArgumentException("Unrecognized boolean value: " + columnValue);
		}

		@Override
		public T toColumnValue(Boolean value, Statement statement) {
			if (value) {
				return trueValue;
			}

			return falseValue;
		}
	}

	private final class DefaultColumnDefiner<T> extends DefaultAttributeDefiner<T> implements Column.ColumnDefiner<T> {

		private final Column<T> column;

		private DefaultColumnDefiner(Column<T> column) {
			super(column);
			this.column = column;
		}

		@Override
		public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> column() {
			return new DefaultColumnDefinition.DefaultColumnDefinitionBuilder<>(column);
		}

		@Override
		public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey() {
			return new DefaultColumnDefinition.DefaultColumnDefinitionBuilder<>(column, 0);
		}

		@Override
		public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey(int index) {
			if (index < 0) {
				throw new IllegalArgumentException("Primary key index must be at least 0: " + attribute);
			}

			return new DefaultColumnDefinition.DefaultColumnDefinitionBuilder<>(column, index);
		}

		@Override
		public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> subquery(String subquery) {
			return new DefaultColumnDefinition.DefaultSubqueryColumnDefinitionBuilder<>(column, subquery);
		}

		@Override
		public <C, B extends ColumnDefinition.Builder<Boolean, B>> ColumnDefinition.Builder<Boolean, B> booleanColumn(Class<C> columnClass,
																																																									C trueValue, C falseValue) {
			if (!type().isBoolean()) {
				throw new IllegalStateException(column + " is not a boolean column");
			}

			return (ColumnDefinition.Builder<Boolean, B>) new DefaultColumnDefinition.DefaultColumnDefinitionBuilder<>(column)
							.converter(columnClass, (Converter<T, C>) new BooleanConverter<>(trueValue, falseValue));
		}

		@Override
		public AuditColumnDefiner<T> auditColumn() {
			return new DefaultAuditColumnDefiner<>(column);
		}
	}

	private final class DefaultAuditColumnDefiner<T> implements AuditColumnDefiner<T> {

		private static final String COLUMN_CAPTION = "Column";

		private final Column<T> column;

		private DefaultAuditColumnDefiner(Column<T> column) {
			this.column = column;
		}

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a row was inserted.
		 * @param column the column
		 * @param <T> the Temporal type to base the column on
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> insertTime() {
			if (!type().isTemporal()) {
				throw new IllegalArgumentException(COLUMN_CAPTION + " " + column + " is not a temporal column");
			}

			return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>(column, INSERT);
		}

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a row was updated.
		 * @param column the column
		 * @param <T> the Temporal type to base the column on
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> updateTime() {
			if (!type().isTemporal()) {
				throw new IllegalArgumentException(COLUMN_CAPTION + " " + column + " is not a temporal column");
			}

			return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>(column, UPDATE);
		}

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who inserted a row.
		 * @param column the column
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 * @throws IllegalArgumentException in case column is not a string attribute
		 */
		public <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> insertUser() {
			if (!type().isString()) {
				throw new IllegalArgumentException(COLUMN_CAPTION + " " + column + " is not a string column");
			}

			return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>((Column<String>) column, INSERT);
		}

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who updated a row.
		 * @param column the column
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 * @throws IllegalArgumentException in case column is not a string attribute
		 */
		public <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> updateUser() {
			if (!type().isString()) {
				throw new IllegalArgumentException(COLUMN_CAPTION + " " + column + " is not a string column");
			}

			return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>((Column<String>) column, UPDATE);
		}
	}
}
