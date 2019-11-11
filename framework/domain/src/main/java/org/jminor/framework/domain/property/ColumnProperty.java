/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.ValueConverter;

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
  Object toColumnValue(final Object value);

  /**
   * @param value the SQL value Object to translate from
   * @return the value of SQL {@code value}
   */
  Object fromColumnValue(final Object value);

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
   * Indicates whether or not this column is updatable
   * @return true if this column is updatable
   */
  boolean isUpdatable();

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
   * @return the ForeignKeyProperty this property is part of, if any
   */
  ForeignKeyProperty getForeignKeyProperty();

  /**
   * @return true if the underlying column has a default value
   */
  boolean columnHasDefaultValue();

  /**
   * Fetches a value for this property from a ResultSet
   * @param resultSet the ResultSet
   * @param index the index of the column to fetch
   * @return a single value fetched from the given ResultSet
   * @throws java.sql.SQLException in case of an exception
   */
  Object fetchValue(final ResultSet resultSet, final int index) throws SQLException;

  /**
   * @return a ResultPacker responsible for packing this property
   */
  ResultPacker<Object> getResultPacker();

  /**
   * Provides setters for ColumnProperty properties
   */
  interface Builder extends Property.Builder {

    /**
     * @return the property
     */
    ColumnProperty get();

    /**
     * Sets the actual string used as column when querying
     * @param columnName the column name
     * @return this instance
     */
    ColumnProperty.Builder setColumnName(final String columnName);

    /**
     * @param updatable specifies whether this property is updatable
     * @return this instance
     */
    ColumnProperty.Builder setUpdatable(final boolean updatable);

    /**
     * @param columnHasDefaultValue specifies whether or not the underlying column has a default value
     * @return this instance
     */
    ColumnProperty.Builder setColumnHasDefaultValue(final boolean columnHasDefaultValue);

    /**
     * Sets the zero based primary key index of this property.
     * Note that setting the primary key index renders this property non-null and non-updatable by default,
     * these can be reverted by setting it as updatable after setting the primary key index.
     * @param index the zero based index
     * @return this instance
     * @throws IllegalArgumentException in case index is a negative number
     * @see #setNullable(boolean)
     * @see #setUpdatable(boolean)
     */
    ColumnProperty.Builder setPrimaryKeyIndex(final int index);

    /**
     * @param groupingColumn true if this column should be used in a group by clause
     * @throws IllegalStateException in case the column has already been defined as an aggregate column
     * @return this instance
     */
    ColumnProperty.Builder setGroupingColumn(final boolean groupingColumn);

    /**
     * @param aggregateColumn true if this column is an aggregate function column
     * @throws IllegalStateException in case the column has already been defined as a grouping column
     * @return this instance
     */
    ColumnProperty.Builder setAggregateColumn(final boolean aggregateColumn);

    /**
     * @param selectable false if this property should not be included in select queries
     * @return this instance
     */
    ColumnProperty.Builder setSelectable(final boolean selectable);

    /**
     * Set a value converter, for converting to and from a sql representation of the value
     * @param valueConverter the converter
     * @return this instance
     */
    ColumnProperty.Builder setValueConverter(final ValueConverter<?, ?> valueConverter);

    /**
     * @param foreignKeyProperty the ForeignKeyProperty this property is part of
     */
    void setForeignKeyProperty(final ForeignKeyProperty foreignKeyProperty);
  }
}
