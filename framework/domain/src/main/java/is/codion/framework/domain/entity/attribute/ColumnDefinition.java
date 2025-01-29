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

import is.codion.framework.domain.entity.attribute.Column.Converter;
import is.codion.framework.domain.entity.attribute.Column.Getter;
import is.codion.framework.domain.entity.attribute.Column.Setter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Specifies a attribute definition based on a table column
 * @param <T> the underlying type
 */
public interface ColumnDefinition<T> extends AttributeDefinition<T> {

	@Override
	Column<T> attribute();

	/**
	 * Note: returns null when used in a remote connection context.
	 * @return the column name
	 */
	String name();

	/**
	 * Note: returns null when used in a remote connection context.
	 * @return the column expression to use when selecting or the column name if no expression has been set
	 */
	String expression();

	/**
	 * @return the sql data type of the underlying column ({@link java.sql.Types}.
	 */
	int type();

	/**
	 * Note: returns null when used in a remote connection context.
	 * @param <C> the colum value type
	 * @return the {@link Converter} for this column.
	 */
	<C> Converter<C, T> converter();

	/**
	 * @return this columns zero based index in the primary key, -1 if this column is not part of a primary key
	 */
	int primaryKeyIndex();

	/**
	 * @return true if this column is part of a primary key
	 */
	boolean primaryKey();

	/**
	 * @return true if this column should be grouped by
	 */
	boolean groupBy();

	/**
	 * @return true if this column is based on an aggregate function
	 */
	boolean aggregate();

	/**
	 * @return true if this column should be selected by default
	 */
	boolean selected();

	/**
	 * Specifies whether this column is insertable
	 * @return true if this column is insertable
	 */
	boolean insertable();

	/**
	 * Indicates whether this column is updatable
	 * @return true if this column is updatable
	 */
	boolean updatable();

	/**
	 * @return true if this column is neither insertable nor updatable.
	 */
	boolean readOnly();

	/**
	 * @return true if the underlying column has a default value
	 */
	boolean columnHasDefaultValue();

	/**
	 * @return true if this column should be included when searching by string
	 */
	boolean searchable();

	/**
	 * Fetches a value for this column from a {@link ResultSet}
	 * @param resultSet the {@link ResultSet}
	 * @param index this columns index in the result
	 * @return a single value fetched from the given {@link ResultSet}
	 * @throws SQLException in case of an exception
	 */
	T get(ResultSet resultSet, int index) throws SQLException;

	/**
	 * Sets a parameter for this column in a {@link PreparedStatement}
	 * @param statement the statement
	 * @param index the parameter index
	 * @param value the value to set, may be null
	 * @throws SQLException in case of an exception
	 */
	void set(PreparedStatement statement, int index, T value) throws SQLException;

	/**
	 * Builds a {@link ColumnDefinition}
	 * @param <T> the underlying type
	 * @param <B> the builder type
	 */
	interface Builder<T, B extends Builder<T, B>> extends AttributeDefinition.Builder<T, B> {

		/**
		 * Sets the actual column type, and the required {@link Converter}.
		 * @param <C> the column type
		 * @param columnClass the underlying column type class
		 * @param converter the converter to use when converting to and from column values
		 * @return this instance
		 */
		<C> B columnClass(Class<C> columnClass, Converter<T, C> converter);

		/**
		 * Sets the actual column type, and the required {@link Converter}.
		 * @param <C> the column type
		 * @param columnClass the underlying column type class
		 * @param converter the converter to use when converting to and from column values
		 * @param getter the getter to use to retrieve the value from a {@link ResultSet}
		 * @return this instance
		 */
		<C> B columnClass(Class<C> columnClass, Converter<T, C> converter, Getter<C> getter);

		/**
		 * Sets the actual column type, and the required {@link Converter}.
		 * @param <C> the column type
		 * @param columnClass the underlying column type class
		 * @param converter the converter to use when converting to and from column values
		 * @param setter the setter to use when setting parameters in a {@link PreparedStatement}
		 * @return this instance
		 */
		<C> B columnClass(Class<C> columnClass, Converter<T, C> converter, Setter<C> setter);

		/**
		 * Sets the actual column type, and the required {@link Converter}.
		 * @param <C> the column type
		 * @param columnClass the underlying column type class
		 * @param converter the converter to use when converting to and from column values
		 * @param getter the getter to use to retrieve the value from a {@link ResultSet}
		 * @param setter the setter to use when setting parameters in a {@link PreparedStatement}
		 * @return this instance
		 */
		<C> B columnClass(Class<C> columnClass, Converter<T, C> converter, Getter<C> getter, Setter<C> setter);

		/**
		 * Sets the actual string used as column name when inserting and updating.
		 * This column name is also used when selecting unless an {@link #expression(String)} has been specified.
		 * @param name the column name
		 * @return this instance
		 */
		B name(String name);

		/**
		 * The expression is used when the column is being selected or used in query conditions.
		 * @param expression the column expression to use when selecting
		 * @return this instance
		 */
		B expression(String expression);

		/**
		 * Specifies whether this column should be included during insert and update operations
		 * @param readOnly true if this column should be read-only
		 * @return this instance
		 */
		B readOnly(boolean readOnly);

		/**
		 * @param insertable specifies whether this column should be included during insert operations
		 * @return this instance
		 */
		B insertable(boolean insertable);

		/**
		 * @param updatable specifies whether this column is updatable
		 * @return this instance
		 */
		B updatable(boolean updatable);

		/**
		 * Specifies that the underlying table column has a default value
		 * @param columnHasDefaultValue true if the column has a default value
		 * @return this instance
		 */
		B columnHasDefaultValue(boolean columnHasDefaultValue);

		/**
		 * Specifies that this column should be grouped by.
		 * Also specifies that this column is not an aggregate function column.
		 * @param groupBy true if this is a grouping column
		 * @return this instance
		 */
		B groupBy(boolean groupBy);

		/**
		 * Specifies that this column is an aggregate function column.
		 * Also specifies that this column should not be grouped by.
		 * @param aggregate true if this is an aggregate function column
		 * @return this instance
		 */
		B aggregate(boolean aggregate);

		/**
		 * <p>Specifies whether this column should be inlucluded by default when selecting.
		 * <p>Default true.
		 * @param selected false if this column should not be selected by default
		 * @return this instance
		 */
		B selected(boolean selected);

		/**
		 * Specifies whether this column should be included when searching for an entity by a string value.
		 * Only applicable to attributes of type {@link java.sql.Types#VARCHAR}.
		 * @param searchable true if this column is a searchable column
		 * @return this instance
		 * @throws IllegalStateException in case this column type is not String
		 */
		B searchable(boolean searchable);
	}
}
