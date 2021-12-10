/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Specifies a property based on a table column
 * @param <T> the underlying type
 */
public interface ColumnProperty<T> extends Property<T> {

  /**
   * @return the column name
   */
  String getColumnName();

  /**
   * @return the column expression to use when selecting or the column name if no expression has been set
   */
  String getColumnExpression();

  /**
   * @return the sql data type of the underlying column ({@link java.sql.Types}.
   */
  int getColumnType();

  /**
   * Translates the given value into a sql value, usually this is not required
   * but for certain types this may be necessary, such as boolean values
   * represented by a non-boolean data type in the underlying database
   * @param value the value to translate
   * @param statement the statement using the column value
   * @param <C> the column value type
   * @return the sql value used to represent the given value
   * @throws java.sql.SQLException in case of an exception
   */
  <C> C toColumnValue(T value, Statement statement) throws SQLException;

  /**
   * @return this propertys zero based index in the primary key, -1 if this property is not part of a primary key
   */
  int getPrimaryKeyIndex();

  /**
   * @return true if this property is part of a primary key
   */
  boolean isPrimaryKeyColumn();

  /**
   * @return true if this column is a group by column
   */
  boolean isGroupingColumn();

  /**
   * @return true if this is an aggregate column
   */
  boolean isAggregateColumn();

  /**
   * @return true if this property should be included in select queries
   */
  boolean isSelectable();

  /**
   * Specifies whether this property is insertable
   * @return true if this property is insertable
   */
  boolean isInsertable();

  /**
   * Indicates whether this column is updatable
   * @return true if this column is updatable
   */
  boolean isUpdatable();

  /**
   * @return true if this property is neither insertable nor updatable.
   */
  boolean isReadOnly();

  /**
   * @return true if this column is a denormalized column, one which should receives a value
   * from a column in a table referenced via a foreign key
   */
  boolean isDenormalized();

  /**
   * @return true if the underlying column has a default value
   */
  boolean columnHasDefaultValue();

  /**
   * @return true if this property should be included when searching by string
   */
  boolean isSearchProperty();

  /**
   * Fetches a value for this property from a ResultSet
   * @param resultSet the ResultSet
   * @param index the index of the column to fetch
   * @return a single value fetched from the given ResultSet
   * @throws java.sql.SQLException in case of an exception
   */
  T fetchValue(ResultSet resultSet, int index) throws SQLException;

  /**
   * @param <T> the result type
   * @return a ResultPacker responsible for packing this property
   */
  <T> ResultPacker<T> getResultPacker();

  /**
   * Fetches a single value from a result set.
   * @param <C> the type of the column value being fetched
   */
  interface ValueFetcher<C> {

    /**
     * Fetches a single value from a ResultSet
     * @param resultSet the ResultSet
     * @param index the index of the column to fetch
     * @return a single value fetched from the given ResultSet
     * @throws java.sql.SQLException in case of an exception
     */
    C fetchValue(ResultSet resultSet, int index) throws SQLException;
  }

  /**
   * Converts to and from SQL values, such as integers being used to represent booleans in a database.
   * @param <T> the type of the value
   * @param <C> the type of the underlying column
   */
  interface ValueConverter<T, C> {

    /**
     * Translates the given value into a sql value, usually this is not required
     * but for certain types this may be necessary, such as boolean values where
     * the values are represented by a non-boolean data type in the underlying database
     * @param value the value to translate
     * @param statement the statement using the value
     * @return the sql value used to represent the given value
     * @throws java.sql.SQLException in case of an exception
     */
    C toColumnValue(T value, Statement statement) throws SQLException;

    /**
     * Translates the given sql column value into a property value.
     * @param columnValue the sql value to translate from
     * @return the value of sql {@code columnValue}
     * @throws java.sql.SQLException in case of an exception
     */
    T fromColumnValue(C columnValue) throws SQLException;
  }

  /**
   * Provides setters for ColumnProperty properties
   * @param <T> the underlying type
   * @param <B> the builder type
   */
  interface Builder<T, B extends Builder<T, B>> extends Property.Builder<T, B> {

    @Override
    ColumnProperty<T> get();

    /**
     * Sets the actual column type, and the required {@link ValueConverter}.
     * @param <C> the column type
     * @param columnClass the underlying column type class
     * @param valueConverter the converter to use when converting to and from column values
     * @return this instance
     */
    <C> B columnClass(Class<C> columnClass, ValueConverter<T, C> valueConverter);

    /**
     * Sets the actual column type, and the required {@link ValueConverter}.
     * @param <C> the column type
     * @param columnClass the underlying column type class
     * @param valueConverter the converter to use when converting to and from column values
     * @param valueFetcher the value fetcher used to retrieve the value from a ResultSet
     * @return this instance
     */
    <C> B columnClass(Class<C> columnClass, ValueConverter<T, C> valueConverter, ValueFetcher<C> valueFetcher);

    /**
     * Sets the actual string used as column when querying
     * @param columnName the column name
     * @return this instance
     */
    B columnName(String columnName);

    /**
     * @param columnExpression the column expression to use when selecting
     * @return this instance
     */
    B columnExpression(String columnExpression);

    /**
     * Specifies that this property should not be included during insert and update operations
     * @return this instance
     */
    B readOnly();

    /**
     * Specifies whether this property should be included during insert and update operations
     * @param readOnly true if this property should be read-only
     * @return this instance
     */
    B readOnly(boolean readOnly);

    /**
     * @param insertable specifies whether this property should be included during insert operations
     * @return this instance
     */
    B insertable(boolean insertable);

    /**
     * @param updatable specifies whether this property is updatable
     * @return this instance
     */
    B updatable(boolean updatable);

    /**
     * Specifies that the underlying table column has a default value
     * @return this instance
     */
    B columnHasDefaultValue();

    /**
     * Sets the zero based primary key index of this property.
     * Note that setting the primary key index renders this property non-null and non-updatable by default,
     * these can be reverted by setting it as updatable and/or nullable after setting the primary key index.
     * @param index the zero based index
     * @return this instance
     * @throws IllegalArgumentException in case index is a negative number
     * @see #nullable(boolean)
     * @see #updatable(boolean)
     */
    B primaryKeyIndex(int index);

    /**
     * Specifies that this column should be used in a group by clause
     * @throws IllegalStateException in case the column has already been defined as an aggregate column
     * @return this instance
     */
    B groupingColumn();

    /**
     * Specifies that this column is an aggregate function column
     * @throws IllegalStateException in case the column has already been defined as a grouping column
     * @return this instance
     */
    B aggregateColumn();

    /**
     * Specifies that this property should not be included in select queries
     * @return this instance
     */
    B nonSelectable();

    /**
     * Specifies that this property is included when searching for an entity by a string value.
     * Only applicable to properties of type {@link java.sql.Types#VARCHAR}.
     * @throws IllegalStateException in case this property type is not String
     * @return this instance
     */
    B searchProperty();
  }
}
