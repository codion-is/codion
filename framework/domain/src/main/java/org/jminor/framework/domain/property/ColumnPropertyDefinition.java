/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.db.ValueConverter;

public interface ColumnPropertyDefinition<T extends ColumnProperty> extends PropertyDefinition<T> {

  @Override
  T get();

  /**
   * Sets the actual string used as column when querying
   * @param columnName the column name
   * @return this Property.Definer instance
   */
  ColumnPropertyDefinition<T> setColumnName(final String columnName);

  /**
   * @param updatable specifies whether this property is updatable
   * @return this Property.Definer instance
   */
  ColumnPropertyDefinition<T> setUpdatable(final boolean updatable);

  /**
   * @param columnHasDefaultValue specifies whether or not the underlying column has a default value
   * @return this Property.Definer instance
   */
  ColumnPropertyDefinition<T> setColumnHasDefaultValue(final boolean columnHasDefaultValue);

  /**
   * Sets the zero based primary key index of this property.
   * Note that setting the primary key index renders this property non-null and non-updatable by default,
   * these can be reverted by setting it as updatable after setting the primary key index.
   * @param index the zero based index
   * @return this ColumnProperty.Definer instance
   * @throws IllegalArgumentException in case index is a negative number
   * @see #setNullable(boolean)
   * @see #setUpdatable(boolean)
   */
  ColumnPropertyDefinition<T> setPrimaryKeyIndex(final int index);

  /**
   * @param groupingColumn true if this column should be used in a group by clause
   * @throws IllegalStateException in case the column has already been defined as an aggregate column
   * @return this Property.Definer instance
   */
  ColumnPropertyDefinition<T> setGroupingColumn(final boolean groupingColumn);

  /**
   * @param aggregateColumn true if this column is an aggregate function column
   * @throws IllegalStateException in case the column has already been defined as a grouping column
   * @return this Property.Definer instance
   */
  ColumnPropertyDefinition<T> setAggregateColumn(final boolean aggregateColumn);

  /**
   * @param selectable false if this property should not be included in select queries
   * @return this Property.Definer instance
   */
  ColumnPropertyDefinition<T> setSelectable(final boolean selectable);

  /**
   * Set a value converter, for converting to and from a sql representation of the value
   * @param valueConverter the converter
   * @return this Property.Definer instance
   */
  ColumnPropertyDefinition<T> setValueConverter(final ValueConverter<?, ?> valueConverter);

  /**
   * @param foreignKeyProperty the ForeignKeyProperty this property is part of
   */
  void setForeignKeyProperty(final ForeignKeyProperty foreignKeyProperty);
}
