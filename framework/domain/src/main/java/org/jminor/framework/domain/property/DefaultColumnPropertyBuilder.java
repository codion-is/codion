/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.db.ValueConverter;

final class DefaultColumnPropertyBuilder extends DefaultPropertyBuilder<DefaultColumnProperty>
        implements ColumnProperty.Builder<DefaultColumnProperty> {

  DefaultColumnPropertyBuilder(final DefaultColumnProperty property) {
    super(property);
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnProperty.Builder setColumnName(final String columnName) {
    property.setColumnName(columnName);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnProperty.Builder setColumnHasDefaultValue(final boolean columnHasDefaultValue) {
    property.setColumnHasDefaultValue(columnHasDefaultValue);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnProperty.Builder setUpdatable(final boolean updatable) {
    property.setUpdatable(updatable);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnProperty.Builder setPrimaryKeyIndex(final int index) {
    property.setPrimaryKeyIndex(index);
    setNullable(false);
    setUpdatable(false);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnProperty.Builder setGroupingColumn(final boolean groupingColumn) {
    property.setGroupingColumn(groupingColumn);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnProperty.Builder setAggregateColumn(final boolean aggregateColumn) {
    property.setAggregateColumn(aggregateColumn);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnProperty.Builder setSelectable(final boolean selectable) {
    property.setSelectable(selectable);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public ColumnProperty.Builder setReadOnly(final boolean readOnly) {
    property.setReadOnly(readOnly);
    if (property.isForeignKeyProperty()) {
      throw new IllegalStateException("Can not set the read only status of a property which is part of a foreign key property");
    }

    return (ColumnProperty.Builder) super.setReadOnly(readOnly);
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnProperty.Builder setValueConverter(final ValueConverter<?, ?> valueConverter) {
    property.setValueConverter(valueConverter);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final void setForeignKeyProperty(final ForeignKeyProperty foreignKeyProperty) {
    property.setForeignKeyProperty(foreignKeyProperty);
  }
}
