/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.db.ValueConverter;

final class DefaultColumnPropertyDefinition extends DefaultPropertyDefinition<DefaultColumnProperty>
        implements ColumnPropertyDefinition<DefaultColumnProperty> {

  DefaultColumnPropertyDefinition(final DefaultColumnProperty property) {
    super(property);
  }

  @Override
  public final DefaultColumnPropertyDefinition setColumnName(final String columnName) {
    property.setColumnName(columnName);
    return this;
  }

  @Override
  public final DefaultColumnPropertyDefinition setColumnHasDefaultValue(final boolean columnHasDefaultValue) {
    property.setColumnHasDefaultValue(columnHasDefaultValue);
    return this;
  }

  @Override
  public final DefaultColumnPropertyDefinition setUpdatable(final boolean updatable) {
    property.setUpdatable(updatable);
    return this;
  }

  @Override
  public final DefaultColumnPropertyDefinition setPrimaryKeyIndex(final int index) {
    property.setPrimaryKeyIndex(index);
    setNullable(false);
    setUpdatable(false);
    return this;
  }

  @Override
  public final DefaultColumnPropertyDefinition setGroupingColumn(final boolean groupingColumn) {
    property.setGroupingColumn(groupingColumn);
    return this;
  }

  @Override
  public final DefaultColumnPropertyDefinition setAggregateColumn(final boolean aggregateColumn) {
    property.setAggregateColumn(aggregateColumn);
    return this;
  }

  @Override
  public final DefaultColumnPropertyDefinition setSelectable(final boolean selectable) {
    property.setSelectable(selectable);
    return this;
  }

  @Override
  public DefaultColumnPropertyDefinition setReadOnly(final boolean readOnly) {
    property.setReadOnly(readOnly);
    if (property.isForeignKeyProperty()) {
      throw new IllegalStateException("Can not set the read only status of a property which is part of a foreign key property");
    }

    return (org.jminor.framework.domain.property.DefaultColumnPropertyDefinition) super.setReadOnly(readOnly);
  }

  @Override
  public final DefaultColumnPropertyDefinition setValueConverter(final ValueConverter<?, ?> valueConverter) {
    property.setValueConverter(valueConverter);
    return this;
  }

  @Override
  public final void setForeignKeyProperty(final ForeignKeyProperty foreignKeyProperty) {
    property.setForeignKeyProperty(foreignKeyProperty);
  }
}
