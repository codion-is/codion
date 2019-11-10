package org.jminor.framework.domain;

import org.jminor.common.db.ValueConverter;

import java.text.Format;
import java.util.List;

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

  interface ColumnPropertyDefinition<T extends Property.ColumnProperty> extends PropertyDefinition<T> {

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
    void setForeignKeyProperty(final Property.ForeignKeyProperty foreignKeyProperty);
  }

  interface ForeignKeyPropertyDefinition<T extends Property.ForeignKeyProperty> extends PropertyDefinition<T> {

    @Override
    T get();

    List<ColumnPropertyDefinition> getPropertyDefiners();

    /**
     * @param fetchDepth the default query fetch depth for this foreign key
     * @return this ForeignKeyProperty instance
     */
    ForeignKeyPropertyDefinition setFetchDepth(final int fetchDepth);

    /**
     * @param softReference true if this foreign key is not based on a physical (table) foreign key
     * and should not prevent deletion
     * @return this ForeignKeyProperty instance
     */
    ForeignKeyPropertyDefinition setSoftReference(final boolean softReference);
  }

  interface TransientPropertyDefinition<T extends Property.TransientProperty> extends PropertyDefinition<T> {

    /**
     * @param modifiesEntity if true then modifications to the value result in the owning entity becoming modified
     * @return this property instance
     */
    TransientPropertyDefinition setModifiesEntity(final boolean modifiesEntity);
  }
}
