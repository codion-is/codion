/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Specifies a attribute definition based on a table column
 * @param <T> the underlying type
 */
public interface ColumnDefinition<T> extends AttributeDefinition<T> {

  @Override
  Column<T> attribute();

  /**
   * @return the column name
   */
  String columnName();

  /**
   * @return the column expression to use when selecting or the column name if no expression has been set
   */
  String columnExpression();

  /**
   * @return the sql data type of the underlying column ({@link java.sql.Types}.
   */
  int columnType();

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
   * @return this columns zero based index in the primary key, -1 if this column is not part of a primary key
   */
  int primaryKeyIndex();

  /**
   * @return true if this column is part of a primary key
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
   * @return true if this column should be included in select queries
   */
  boolean isSelectable();

  /**
   * Specifies whether this column is insertable
   * @return true if this column is insertable
   */
  boolean isInsertable();

  /**
   * Indicates whether this column is updatable
   * @return true if this column is updatable
   */
  boolean isUpdatable();

  /**
   * @return true if this column is neither insertable nor updatable.
   */
  boolean isReadOnly();

  /**
   * @return true if the underlying column has a default value
   */
  boolean columnHasDefaultValue();

  /**
   * @return true if this column should be included when searching by string
   */
  boolean isSearchColumn();

  /**
   * Fetches a value for this column from a ResultSet
   * @param resultSet the ResultSet
   * @param index the index of the column to fetch
   * @return a single value fetched from the given ResultSet
   * @throws SQLException in case of an exception
   */
  T get(ResultSet resultSet, int index) throws SQLException;

  /**
   * @return a ResultPacker responsible for packing this column
   */
  ResultPacker<T> resultPacker();

  /**
   * Builds a {@link ColumnDefinition}
   * @param <T> the underlying type
   * @param <B> the builder type
   */
  interface Builder<T, B extends Builder<T, B>> extends AttributeDefinition.Builder<T, B> {

    /**
     * Sets the actual column type, and the required {@link Column.ValueConverter}.
     * @param <C> the column type
     * @param columnClass the underlying column type class
     * @param valueConverter the converter to use when converting to and from column values
     * @return this instance
     */
    <C> B columnClass(Class<C> columnClass, Column.ValueConverter<T, C> valueConverter);

    /**
     * Sets the actual column type, and the required {@link Column.ValueConverter}.
     * @param <C> the column type
     * @param columnClass the underlying column type class
     * @param valueConverter the converter to use when converting to and from column values
     * @param valueFetcher the value fetcher used to retrieve the value from a ResultSet
     * @return this instance
     */
    <C> B columnClass(Class<C> columnClass, Column.ValueConverter<T, C> valueConverter, Column.ValueFetcher<C> valueFetcher);

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
     * Sets the zero based primary key index of this column.
     * Note that setting the primary key index renders this column non-null and non-updatable by default,
     * these can be reverted by setting it as updatable and/or nullable after setting the primary key index.
     * @param index the zero based index
     * @return this instance
     * @throws IllegalArgumentException in case index is a negative number
     * @see #nullable(boolean)
     * @see #updatable(boolean)
     */
    B primaryKeyIndex(int index);

    /**
     * Specifies that this column should be used in a group by clause.
     * Also specifies that this column should not be an aggregate column.
     * @param groupingColumn true if this is a grouping column
     * @return this instance
     */
    B groupingColumn(boolean groupingColumn);

    /**
     * Specifies that this column is an aggregate function column
     * Also specifies that this column should not be a grouping column.
     * @param aggregateColumn true if this is an aggregate column
     * @return this instance
     */
    B aggregateColumn(boolean aggregateColumn);

    /**
     * Specifies whether this column should be included in select queries
     * @param selectable true if this column should be included in select queries
     * @return this instance
     */
    B selectable(boolean selectable);

    /**
     * Specifies whether this column should be included when searching for an entity by a string value.
     * Only applicable to attributes of type {@link java.sql.Types#VARCHAR}.
     * @param searchColumn true if this column is a search column
     * @return this instance
     * @throws IllegalStateException in case this column type is not String
     */
    B searchColumn(boolean searchColumn);
  }
}
