/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.text.Format;

class DefaultPropertyBuilder<T extends DefaultProperty> implements Property.Builder<T> {

  protected final T property;

  DefaultPropertyBuilder(final T property) {
    this.property = property;
  }

  /** {@inheritDoc} */
  @Override
  public T get() {
    return property;
  }

  /** {@inheritDoc} */
  @Override
  public Property.Builder<T> setEntityId(final String entityId) {
    property.setEntityId(entityId);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setBeanProperty(final String beanProperty) {
    property.setBeanProperty(beanProperty);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setHidden(final boolean hidden) {
    property.setHidden(hidden);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Property.Builder<T> setReadOnly(final boolean readOnly) {
    property.setReadOnly(readOnly);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setDefaultValue(final Object defaultValue) {
    property.setDefaultValue(defaultValue);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Property.Builder<T> setDefaultValueProvider(final Property.ValueProvider provider) {
    property.setDefaultValueProvider(provider);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Property.Builder<T> setNullable(final boolean nullable) {
    property.setNullable(nullable);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setMaxLength(final int maxLength) {
    property.setMaxLength(maxLength);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setMax(final double max) {
    property.setMax(max);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setMin(final double min) {
    property.setMin(min);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setUseNumberFormatGrouping(final boolean useGrouping) {
    property.setUseNumberFormatGrouping(useGrouping);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setPreferredColumnWidth(final int preferredColumnWidth) {
    property.setPreferredColumnWidth(preferredColumnWidth);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setDescription(final String description) {
    property.setDescription(description);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setMnemonic(final Character mnemonic) {
    property.setMnemonic(mnemonic);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setFormat(final Format format) {
    property.setFormat(format);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setDateTimeFormatPattern(final String dateTimeFormatPattern) {
    property.setDateTimeFormatPattern(dateTimeFormatPattern);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property.Builder<T> setMaximumFractionDigits(final int maximumFractionDigits) {
    property.setMaximumFractionDigits(maximumFractionDigits);
    return this;
  }
}
