/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Specifies a property based on a table column
 */
public interface ColumnProperty extends Property {

  /**
   * @return the column name
   */
  String getColumnName();

  /**
   * @return the data type of the underlying column, usually the same as {@link #getType()}
   * but can differ when the database system does not have native support for the given data type,
   * such as boolean
   */
  int getColumnType();

  /**
   * Translates the given value into a sql value, usually this is not required
   * but for certain types this may be necessary, such as boolean values
   * represented by a non-boolean data type in the underlying database
   * @param value the value to translate
   * @return the sql value used to represent the given value
   */
  Object toColumnValue(Object value);

  /**
   * @param value the SQL value Object to translate from
   * @return the value of SQL {@code value}
   */
  Object fromColumnValue(Object value);

  /**
   * @return this propertys zero based index in the primary key, -1 if this property is not part of a primary key
   */
  int getPrimaryKeyIndex();

  /**
   * @return true if this property is part of a primary key
   */
  boolean isPrimaryKeyProperty();

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
   * Specifies whether or not this property is insertable
   * @return true if this property is insertable
   */
  boolean isInsertable();

  /**
   * Indicates whether or not this column is updatable
   * @return true if this column is updatable
   */
  boolean isUpdatable();

  /**
   * @return true if this property is neither insertable nor updatable.
   */
  boolean isReadOnly();

  /**
   * @return true if this column is a denormalized column, one should which receives a value
   * from a column in a table referenced via a foreign key
   */
  boolean isDenormalized();

  /**
   * @return true if this property is part of a ForeignKeyProperty
   */
  boolean isForeignKeyProperty();

  /**
   * @return true if the underlying column has a default value
   */
  boolean columnHasDefaultValue();

  /**
   * Fetches a value for this property from a ResultSet
   * @param resultSet the ResultSet
   * @param index the index of the column to fetch
   * @param <T> the value type
   * @return a single value fetched from the given ResultSet
   * @throws java.sql.SQLException in case of an exception
   */
  <T> T fetchValue(ResultSet resultSet, int index) throws SQLException;

  /**
   * @param <T> the result type
   * @return a ResultPacker responsible for packing this property
   */
  <T> ResultPacker<T> getResultPacker();

  /**
   * Fetches a single value from a result set.
   * @param <T> the type of the value being fetched
   */
  interface ValueFetcher<T> {

    /**
     * Fetches a single value from a ResultSet
     * @param resultSet the ResultSet
     * @param index the index of the column to fetch
     * @return a single value fetched from the given ResultSet
     * @throws java.sql.SQLException in case of an exception
     */
    T fetchValue(ResultSet resultSet, int index) throws SQLException;
  }

  /**
   * Converts to and from SQL values, such as integers being used to represent booleans in a database.
   * @param <T> the type of the value
   * @param <C> the type of the underlying column
   * @see Builder#columnType(int)
   */
  interface ValueConverter<T, C> {

    /**
     * Translates the given value into a sql value, usually this is not required
     * but for certain types this may be necessary, such as boolean values where
     * the values are represented by a non-boolean data type in the underlying database
     * @param value the value to translate
     * @return the sql value used to represent the given value
     */
    C toColumnValue(T value);

    /**
     * Translates the given sql column value into a property value.
     * @param columnValue the sql value to translate from
     * @return the value of sql {@code columnValue}
     */
    T fromColumnValue(C columnValue);
  }

  /**
   * Provides setters for ColumnProperty properties
   */
  interface Builder extends Property.Builder {

    /**
     * @return the property
     */
    ColumnProperty get();

    /**
     * Sets the actual column type, use in conjunction with a {@link ValueConverter} if necessary.
     * @param columnType the underlying column type
     * @return this instance
     */
    ColumnProperty.Builder columnType(int columnType);

    /**
     * Sets the actual string used as column when querying
     * @param columnName the column name
     * @return this instance
     */
    ColumnProperty.Builder columnName(String columnName);

    /**
     * @param readOnly specifies whether this property should be included during insert and update operations
     * @return this instance
     */
    ColumnProperty.Builder readOnly(boolean readOnly);

    /**
     * @param insertable specifies whether this property should be included during insert operations
     * @return this instance
     */
    ColumnProperty.Builder insertable(boolean insertable);

    /**
     * @param updatable specifies whether this property is updatable
     * @return this instance
     */
    ColumnProperty.Builder updatable(boolean updatable);

    /**
     * @param columnHasDefaultValue specifies whether or not the underlying column has a default value
     * @return this instance
     */
    ColumnProperty.Builder columnHasDefaultValue(boolean columnHasDefaultValue);

    /**
     * Sets the zero based primary key index of this property.
     * Note that setting the primary key index renders this property non-null and non-updatable by default,
     * these can be reverted by setting it as updatable after setting the primary key index.
     * @param index the zero based index
     * @return this instance
     * @throws IllegalArgumentException in case index is a negative number
     * @see #nullable(boolean)
     * @see #updatable(boolean)
     */
    ColumnProperty.Builder primaryKeyIndex(int index);

    /**
     * @param groupingColumn true if this column should be used in a group by clause
     * @throws IllegalStateException in case the column has already been defined as an aggregate column
     * @return this instance
     */
    ColumnProperty.Builder groupingColumn(boolean groupingColumn);

    /**
     * @param aggregateColumn true if this column is an aggregate function column
     * @throws IllegalStateException in case the column has already been defined as a grouping column
     * @return this instance
     */
    ColumnProperty.Builder aggregateColumn(boolean aggregateColumn);

    /**
     * @param selectable false if this property should not be included in select queries
     * @return this instance
     */
    ColumnProperty.Builder selectable(boolean selectable);

    /**
     * Set a value converter, for converting to and from a sql representation of the value
     * @param valueConverter the converter
     * @return this instance
     */
    ColumnProperty.Builder valueConverter(ValueConverter<?, ?> valueConverter);

    /**
     * @param foreignKeyProperty true if this property is part of a foreign key
     */
    void setForeignKeyProperty(boolean foreignKeyProperty);
  }
}
