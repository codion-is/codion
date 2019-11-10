/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.db.ResultPacker;

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
}
