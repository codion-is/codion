/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.text.Format;

public interface PropertyDefinition<T extends Property> {

  T get();

  /**
   * @param entityId the id of the entity this property is associated with
   * @throws IllegalStateException in case the entityId has already been set
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setEntityId(final String entityId);

  /**
   * Sets the bean name property to associate with this property
   * @param beanProperty.Definer the bean property name
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setBeanProperty(final String beanProperty);

  /**
   * Sets the default value for this property, overrides the underlying column default value, if any
   * @param defaultValue the value to use as default
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setDefaultValue(final Object defaultValue);

  /**
   * Sets the default value provider, use in case of dynamic default values.
   * @param provider the default value provider
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setDefaultValueProvider(final Property.ValueProvider provider);

  /**
   * @param hidden specifies whether this property should hidden in table views
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setHidden(final boolean hidden);

  /**
   * Sets the maximum allowed value for this property, only applicable to numerical properties
   * @param max the maximum allowed value
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setMax(final double max);

  /**
   * Only applicable to numerical properties
   * @param min the minimum allowed value for this property
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setMin(final double min);

  /**
   * Sets the maximum fraction digits to show for this property, only applicable to properties based on Types.DOUBLE.
   * This setting is overridden during subsequent calls to {@link #setFormat(java.text.Format)}.
   * Note that values associated with this property are automatically rounded to {@code maximumFractionDigits} digits.
   * @param maximumFractionDigits the maximum fraction digits
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setMaximumFractionDigits(final int maximumFractionDigits);

  /**
   * Specifies whether to use number grouping when presenting the value associated with this property.
   * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
   * By default grouping is not used.
   * Only applicable to numerical properties.
   * This setting is overridden during subsequent calls to {@code setFormat}
   * @param useGrouping if true then number grouping is used
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setUseNumberFormatGrouping(final boolean useGrouping);

  /**
   * @param preferredColumnWidth the preferred column width of this property in pixels when displayed in a table
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setPreferredColumnWidth(final int preferredColumnWidth);

  /**
   * @param readOnly specifies whether this property should be included during insert/update operations
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setReadOnly(final boolean readOnly);

  /**
   * Specifies whether or not this property is nullable, in case of
   * properties that are parts of a ForeignKeyProperty.Definer inherit the nullable state of that property.
   * @param nullable specifies whether or not this property accepts a null value
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setNullable(final boolean nullable);

  /**
   * Sets the maximum length of this property value, this applies to String (varchar) based properties
   * @param maxLength the maximum length
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setMaxLength(final int maxLength);

  /**
   * Sets the mnemonic to use when creating a label for this property
   * @param mnemonic the mnemonic character
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setMnemonic(final Character mnemonic);

  /**
   * @param description a String describing this property
   * @return this Property.Definer instance
   */
  PropertyDefinition<T> setDescription(final String description);

  /**
   * Sets the Format to use when presenting property values
   * @param format the format to use
   * @return this Property.Definer instance
   * @throws NullPointerException in case format is null
   * @throws IllegalArgumentException in case the format does not fit the property type,
   * f.ex. NumberFormat is expected for numerical properties
   */
  PropertyDefinition<T> setFormat(final Format format);

  /**
   * Sets the date/time format pattern used when presenting values
   * @param dateTimeFormatPattern the format pattern
   * @return this Property.Definer instance
   * @throws IllegalArgumentException in case the pattern is invalid or if this property is not a date/time based one
   */
  PropertyDefinition<T> setDateTimeFormatPattern(final String dateTimeFormatPattern);

}
