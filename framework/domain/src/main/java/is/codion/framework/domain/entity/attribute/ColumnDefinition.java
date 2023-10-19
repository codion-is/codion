/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.attribute.Column.ValueConverter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
   * @return the {@link ValueConverter} for this column.
   * @param <C> the colum value type
   */
  <C> ValueConverter<C, T> valueConverter();

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
   * @return true if this column should be included in select queries
   */
  boolean selectable();

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
  boolean searchColumn();

  /**
   * Fetches a value for this column from a ResultSet
   * @param resultSet the ResultSet
   * @param index this columns index in the result
   * @return a single value fetched from the given ResultSet
   * @throws SQLException in case of an exception
   */
  T get(ResultSet resultSet, int index) throws SQLException;

  /**
   * Builds a {@link ColumnDefinition}
   * @param <T> the underlying type
   * @param <B> the builder type
   */
  interface Builder<T, B extends Builder<T, B>> extends AttributeDefinition.Builder<T, B> {

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
    <C> B columnClass(Class<C> columnClass, ValueConverter<T, C> valueConverter, Column.ValueFetcher<C> valueFetcher);

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

    /**
     * @param items the Items representing all the valid values for this attribute
     * @return this instance
     * @throws IllegalArgumentException in case the valid item list contains duplicate values
     */
    B items(List<Item<T>> items);
  }
}
