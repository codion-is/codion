/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.db.ValueConverter;

final class DefaultColumnPropertyDefinition extends DefaultPropertyDefinition<DefaultProperty.DefaultColumnProperty>
        implements ColumnPropertyDefinition<DefaultProperty.DefaultColumnProperty> {

  DefaultColumnPropertyDefinition(final DefaultProperty.DefaultColumnProperty property) {
    super(property);
  }

  @Override
  public final org.jminor.framework.domain.property.DefaultColumnPropertyDefinition setColumnName(final String columnName) {
    property.setColumnName(columnName);
    return this;
  }

  @Override
  public final org.jminor.framework.domain.property.DefaultColumnPropertyDefinition setColumnHasDefaultValue(final boolean columnHasDefaultValue) {
    property.setColumnHasDefaultValue(columnHasDefaultValue);
    return this;
  }

  @Override
  public final org.jminor.framework.domain.property.DefaultColumnPropertyDefinition setUpdatable(final boolean updatable) {
    property.setUpdatable(updatable);
    return this;
  }

  @Override
  public final org.jminor.framework.domain.property.DefaultColumnPropertyDefinition setPrimaryKeyIndex(final int index) {
    property.setPrimaryKeyIndex(index);
    setNullable(false);
    setUpdatable(false);
    return this;
  }

  @Override
  public final org.jminor.framework.domain.property.DefaultColumnPropertyDefinition setGroupingColumn(final boolean groupingColumn) {
    property.setGroupingColumn(groupingColumn);
    return this;
  }

  @Override
  public final org.jminor.framework.domain.property.DefaultColumnPropertyDefinition setAggregateColumn(final boolean aggregateColumn) {
    property.setAggregateColumn(aggregateColumn);
    return this;
  }

  @Override
  public final org.jminor.framework.domain.property.DefaultColumnPropertyDefinition setSelectable(final boolean selectable) {
    property.setSelectable(selectable);
    return this;
  }

  @Override
  public org.jminor.framework.domain.property.DefaultColumnPropertyDefinition setReadOnly(final boolean readOnly) {
    property.setReadOnly(readOnly);
    if (property.isForeignKeyProperty()) {
      throw new IllegalStateException("Can not set the read only status of a property which is part of a foreign key property");
    }

    return (org.jminor.framework.domain.property.DefaultColumnPropertyDefinition) super.setReadOnly(readOnly);
  }

  @Override
  public final org.jminor.framework.domain.property.DefaultColumnPropertyDefinition setValueConverter(final ValueConverter<?, ?> valueConverter) {
    property.setValueConverter(valueConverter);
    return this;
  }

  @Override
  public final void setForeignKeyProperty(final ForeignKeyProperty foreignKeyProperty) {
    property.setForeignKeyProperty(foreignKeyProperty);
  }
}
