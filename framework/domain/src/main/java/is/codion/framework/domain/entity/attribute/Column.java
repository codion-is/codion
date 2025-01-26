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
import is.codion.framework.domain.entity.condition.ColumnCondition;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static java.util.Objects.requireNonNull;

/**
 * An {@link Attribute} representing a table column.
 * @param <T> the column value type
 */
public interface Column<T> extends Attribute<T>, ColumnCondition.Factory<T> {

	/**
	 * @return a {@link ColumnDefiner} for this column
	 */
	ColumnDefiner<T> define();

	/**
	 * Creates a new {@link Column}, associated with the given entityType.
	 * @param entityType the entityType owning this column
	 * @param name the column name
	 * @param typeReference the {@link TypeReference} representing the column value type
	 * @param <T> the column type
	 * @return a new {@link Column}
	 */
	static <T> Column<T> column(EntityType entityType, String name, TypeReference<T> typeReference) {
		return new DefaultColumn<>(name, requireNonNull(typeReference).rawType(), entityType);
	}

	/**
	 * Creates a new {@link Column}, associated with the given entityType.
	 * @param entityType the entityType owning this column
	 * @param name the column name
	 * @param valueClass the class representing the column value type
	 * @param <T> the column type
	 * @return a new {@link Column}
	 */
	static <T> Column<T> column(EntityType entityType, String name, Class<T> valueClass) {
		return new DefaultColumn<>(name, valueClass, entityType);
	}

	/**
	 * Provides {@link ColumnDefinition.Builder} instances.
	 * @param <T> the column type
	 */
	interface ColumnDefiner<T> extends AttributeDefiner<T> {

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance.
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> column();

		/**
		 * Returns a new {@link ColumnDefinition.Builder} instance, with the primary key index 0.
		 * Note that this renders this column non-null and non-updatable by default, this can be
		 * reverted by setting it as updatable and/or nullable after defining a primary key column.
		 * <pre>
		 * {@code
		 *   ...
		 *   .primaryKey()
		 *   .nullable(true)
		 *   .updatable(true)
		 * }
		 * </pre>
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder} with primary key index 0
		 * @see ColumnDefinition.Builder#nullable(boolean)
		 * @see ColumnDefinition.Builder#updatable(boolean)
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey();

		/**
		 * Returns a new {@link ColumnDefinition.Builder} instance, with the given primary key index.
		 * Note that this renders this column non-null and non-updatable by default, this can be
		 * reverted by setting it as updatable and/or nullable after defining a primary key column.
		 * <pre>
		 * {@code
		 *   ...
		 *   .primaryKey()
		 *   .nullable(true)
		 *   .updatable(true)
		 * }
		 * </pre>
		 * @param index the zero-based index of this column in the primary key
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder} with the given primary key index
		 * @throws IllegalArgumentException in case index is a negative number
		 * @see ColumnDefinition.Builder#nullable(boolean)
		 * @see ColumnDefinition.Builder#updatable(boolean)
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey(int index);

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance, based on a subquery.
		 * @param subquery the sql query
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> subquery(String subquery);

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance representing a Boolean value.
		 * @param <C> the column type
		 * @param <B> the builder type
		 * @param columnClass the underlying column data type class
		 * @param trueValue the value representing 'true' in the underlying column
		 * @param falseValue the value representing 'false' in the underlying column
		 * @return a new {@link ColumnDefinition.Builder}
		 * @throws NullPointerException in case either the true or false value is null
		 * @throws IllegalStateException in case this column is not a boolean column
		 * @throws IllegalArgumentException in case the values representing true and false are equal
		 */
		<C, B extends ColumnDefinition.Builder<Boolean, B>> ColumnDefinition.Builder<Boolean, B> booleanColumn(Class<C> columnClass,
																																																					 C trueValue, C falseValue);

		/**
		 * @return a new {@link AuditColumnDefiner} instance
		 */
		AuditColumnDefiner<T> auditColumn();

		/**
		 * A convenience method for a {@link Converter} for boolean columns
		 * @param <C> the actual column type
		 * @param trueValue the true value
		 * @param falseValue the false value
		 * @return a boolean value converter
		 * @throws NullPointerException in case either the true or false value is null
		 * @throws IllegalArgumentException in case the values representing true and false are equal
		 */
		static <C> Converter<Boolean, C> booleanConverter(C trueValue, C falseValue) {
			return new DefaultColumn.BooleanConverter<>(trueValue, falseValue);
		}
	}

	/**
	 * Provides {@link ColumnDefinition.Builder} instances for audit columns.
	 * @param <T> the column type
	 */
	interface AuditColumnDefiner<T> {

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a row was inserted.
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> insertTime();

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a row was updated.
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> updateTime();

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who inserted a row.
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		<B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> insertUser();

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who updated a row.
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		<B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> updateUser();
	}

	/**
	 * Gets a single value from a {@link ResultSet}.
	 * @param <C> the column value type
	 */
	interface Getter<C> {

		/**
		 * Fetches a single value from a ResultSet
		 * @param resultSet the ResultSet
		 * @param index the index of the column to fetch
		 * @return a single value fetched from the given ResultSet
		 * @throws java.sql.SQLException in case of an exception
		 */
		C get(ResultSet resultSet, int index) throws SQLException;
	}

	/**
	 * Sets a parameter value in a {@link PreparedStatement}
	 * @param <C> the column value type
	 */
	interface Setter<C> {

		/**
		 * Sets a parameter value in a {@link PreparedStatement}
		 * @param statement the statement
		 * @param index the parameter index
		 * @param value the value to set, may be null
		 * @throws SQLException in case of an exception
		 */
		void set(PreparedStatement statement, int index, C value) throws SQLException;
	}

	/**
	 * Converts to and from SQL values, such as integers being used to represent booleans in a database.
	 * <p>
	 * By default a {@link Converter} is not expected to handle null values, with null values automatically converted to/from null column values.
	 * <p>
	 * If a {@link Converter} needs to handle null values as well as non-null values {@link #handlesNull()} must be overridden to return true.
	 * @param <T> the type of the value
	 * @param <C> the type of the underlying column
	 */
	interface Converter<T, C> {

		/**
		 * Unless a Converter handles null, null values are automatically converted to null column values.
		 * @return true if this Converter handles the null value, default false
		 */
		default boolean handlesNull() {
			return false;
		}

		/**
		 * Translates the given value into a sql value, usually this is not required
		 * but for certain types this may be necessary, such as boolean values where
		 * the values are represented by a non-boolean data type in the underlying database
		 * @param value the value to translate, not null unless {@link #handlesNull()} is overridden
		 * @param statement the statement using the value, may be null
		 * @return the sql value used to represent the given value
		 * @throws SQLException in case of an exception
		 */
		C toColumnValue(T value, Statement statement) throws SQLException;

		/**
		 * Translates the given sql column value into a column value.
		 * @param columnValue the sql value to translate from, not null unless {@link #handlesNull()} is overridden
		 * @return the value of sql {@code columnValue}
		 * @throws SQLException in case of an exception
		 */
		T fromColumnValue(C columnValue) throws SQLException;
	}
}
