/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.ValueConverter;

import java.text.Format;
import java.util.List;

class DefaultPropertyDefinition<T extends DefaultProperty> implements PropertyDefinition<T> {

  protected final T property;

  DefaultPropertyDefinition(final T property) {
    this.property = property;
  }

  @Override
  public T get() {
    return property;
  }

  @Override
  public PropertyDefinition<T> setEntityId(final String entityId) {
    property.setEntityId(entityId);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setBeanProperty(final String beanProperty) {
    property.setBeanProperty(beanProperty);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setHidden(final boolean hidden) {
    property.setHidden(hidden);
    return this;
  }

  @Override
  public PropertyDefinition<T> setReadOnly(final boolean readOnly) {
    property.setReadOnly(readOnly);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setDefaultValue(final Object defaultValue) {
    property.setDefaultValue(defaultValue);
    return this;
  }

  @Override
  public PropertyDefinition<T> setDefaultValueProvider(final Property.ValueProvider provider) {
    property.setDefaultValueProvider(provider);
    return this;
  }

  @Override
  public PropertyDefinition<T> setNullable(final boolean nullable) {
    property.setNullable(nullable);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setMaxLength(final int maxLength) {
    property.setMaxLength(maxLength);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setMax(final double max) {
    property.setMax(max);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setMin(final double min) {
    property.setMin(min);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setUseNumberFormatGrouping(final boolean useGrouping) {
    property.setUseNumberFormatGrouping(useGrouping);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setPreferredColumnWidth(final int preferredColumnWidth) {
    property.setPreferredColumnWidth(preferredColumnWidth);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setDescription(final String description) {
    property.setDescription(description);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setMnemonic(final Character mnemonic) {
    property.setMnemonic(mnemonic);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setFormat(final Format format) {
    property.setFormat(format);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setDateTimeFormatPattern(final String dateTimeFormatPattern) {
    property.setDateTimeFormatPattern(dateTimeFormatPattern);
    return this;
  }

  @Override
  public final PropertyDefinition<T> setMaximumFractionDigits(final int maximumFractionDigits) {
    property.setMaximumFractionDigits(maximumFractionDigits);
    return this;
  }

  static final class DefaultColumnPropertyDefinition extends DefaultPropertyDefinition<DefaultProperty.DefaultColumnProperty>
          implements ColumnPropertyDefinition<DefaultProperty.DefaultColumnProperty> {

    DefaultColumnPropertyDefinition(final DefaultProperty.DefaultColumnProperty property) {
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

      return (DefaultColumnPropertyDefinition) super.setReadOnly(readOnly);
    }

    @Override
    public final DefaultColumnPropertyDefinition setValueConverter(final ValueConverter<?, ?> valueConverter) {
      property.setValueConverter(valueConverter);
      return this;
    }

    @Override
    public final void setForeignKeyProperty(final Property.ForeignKeyProperty foreignKeyProperty) {
      property.setForeignKeyProperty(foreignKeyProperty);
    }
  }

  static final class DefaultForeignKeyPropertyDefinition extends DefaultPropertyDefinition<DefaultProperty.DefaultForeignKeyProperty>
          implements ForeignKeyPropertyDefinition<DefaultProperty.DefaultForeignKeyProperty> {

    DefaultForeignKeyPropertyDefinition(final DefaultProperty.DefaultForeignKeyProperty property) {
      super(property);
    }

    @Override
    public List<ColumnPropertyDefinition> getPropertyDefiners() {
      return property.columnPropertyDefinitions;
    }

    @Override
    public ForeignKeyPropertyDefinition setFetchDepth(final int fetchDepth) {
      property.setFetchDepth(fetchDepth);
      return this;
    }

    @Override
    public ForeignKeyPropertyDefinition setSoftReference(final boolean softReference) {
      property.setSoftReference(softReference);
      return this;
    }
  }

  static final class DefaultTransientPropertyDefinition extends DefaultPropertyDefinition<DefaultProperty.DefaultTransientProperty>
          implements TransientPropertyDefinition<DefaultProperty.DefaultTransientProperty> {

    DefaultTransientPropertyDefinition(final DefaultProperty.DefaultTransientProperty property) {
      super(property);
    }

    @Override
    public DefaultTransientPropertyDefinition setModifiesEntity(final boolean modifiesEntity) {
      property.setModifiesEntity(modifiesEntity);
      return this;
    }
  }
}
