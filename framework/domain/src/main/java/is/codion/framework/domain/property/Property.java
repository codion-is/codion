/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Configuration;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import static is.codion.framework.domain.property.AuditProperty.AuditAction.INSERT;
import static is.codion.framework.domain.property.AuditProperty.AuditAction.UPDATE;

/**
 * Specifies a Property. Factory for {@link Property} instances.
 * @param <T> the underlying type
 */
public interface Property<T> {

  int DEFAULT_MAXIMUM_FRACTION_DIGITS = 10;
  int DEFAULT_FOREIGN_KEY_FETCH_DEPTH = 1;

  /**
   * Specifies the default maximum number of fraction digits for double property values<br>
   * Note that values are rounded when set.<br>
   * Value type: Integer<br>
   * Default value: 10
   */
  PropertyValue<Integer> MAXIMUM_FRACTION_DIGITS = Configuration.integerValue("codion.domain.maximumFractionDigits", DEFAULT_MAXIMUM_FRACTION_DIGITS);

  /**
   * Specifies the default rounding mode used for decimal property values<br>
   * Value type: {@link RoundingMode}<br>
   * Default value: {@link RoundingMode#HALF_EVEN}<br>
   * @see #MAXIMUM_FRACTION_DIGITS
   * @see Property.Builder#decimalRoundingMode(RoundingMode)
   */
  PropertyValue<RoundingMode> DECIMAL_ROUNDING_MODE = Configuration.enumValue("codion.domain.decimalRoundingMode", RoundingMode.class, RoundingMode.HALF_EVEN);

  /**
   * The default date format pattern to use when showing time values in tables and when creating default time input fields<br>
   * Value type: String<br>
   * Default value: HH:mm
   */
  PropertyValue<String> TIME_FORMAT = Configuration.stringValue("codion.domain.timeFormat", LocaleDateTimePattern.builder()
          .hoursMinutes()
          .build()
          .timePattern());

  /**
   * The default date/time format pattern to use when showing date/time values in tables and when creating default date/time input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy HH:mm [month/day order is locale specific]
   */
  PropertyValue<String> DATE_TIME_FORMAT = Configuration.stringValue("codion.domain.dateTimeFormat", LocaleDateTimePattern.builder()
          .delimiterDash()
          .yearFourDigits()
          .hoursMinutes()
          .build()
          .dateTimePattern());

  /**
   * The default date format pattern to use when showing date values in tables and when creating default date input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy [month/day order is locale specific]
   */
  PropertyValue<String> DATE_FORMAT = Configuration.stringValue("codion.domain.dateFormat", LocaleDateTimePattern.builder()
          .delimiterDash()
          .yearFourDigits()
          .build()
          .datePattern());

  /**
   * Specifies whether number format grouping is used by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> NUMBER_FORMAT_GROUPING = Configuration.booleanValue("codion.domain.numberFormatGrouping", false);

  /**
   * Specifies the default number grouping separator.<br>
   * Value type: Character<br>
   * Default value: The grouping separator for the default locale
   */
  PropertyValue<Character> GROUPING_SEPARATOR = Configuration.characterValue("codion.domain.groupingSeparator", DecimalFormatSymbols.getInstance().getGroupingSeparator());

  /**
   * Specifies the default number decimal separator.<br>
   * Value type: Character<br>
   * Default value: The decimal separator for the default locale
   */
  PropertyValue<Character> DECIMAL_SEPARATOR = Configuration.characterValue("codion.domain.decimalSeparator", DecimalFormatSymbols.getInstance().getDecimalSeparator());

  /**
   * Specifies the default foreign key fetch depth<br>
   * Value type: Integer<br>
   * Default value: 1
   */
  PropertyValue<Integer> FOREIGN_KEY_FETCH_DEPTH = Configuration.integerValue("codion.domain.foreignKeyFetchDepth", DEFAULT_FOREIGN_KEY_FETCH_DEPTH);

  /**
   * Specifies whether String values should use a lexical comparator by default<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> USE_LEXICAL_STRING_COMPARATOR = Configuration.booleanValue("codion.domain.useLexicalStringComparator", true);

  /**
   * The {@link Attribute} this property is based on, should be unique within an Entity.
   * By default, the {@link Attribute#name()} serves as column name for database properties.
   * @return the attribute this property is based on
   */
  Attribute<T> attribute();

  /**
   * @return the type of the entity this Property is associated with
   */
  EntityType entityType();

  /**
   * @return the caption
   */
  String caption();

  /**
   * @return a String describing this attribute
   */
  String description();

  /**
   * @return the bean property name associated with this property
   */
  String beanProperty();

  /**
   * Prepares the value according to the property specification, such as rounding
   * to the correct number of fraction digits in case of doubles
   * @param value the value to prepare
   * @return the prepared value
   */
  T prepareValue(T value);

  /**
   * Returns a string representation of the given value formatted with this property's format.
   * If no format is available {@link Object#toString()} is used. By default, null values result in an empty string.
   * @param value the value to format.
   * @return the value formatted as a string
   * @see Property.Builder#format(Format)
   * @see Property.Builder#dateTimePattern(String)
   */
  String toString(T value);

  /**
   * @return true if a default value has been set for this property
   */
  boolean hasDefaultValue();

  /**
   * @return the default value for this property, if no default value has been set null is returned
   * @see #hasDefaultValue()
   */
  T defaultValue();

  /**
   * @return true if this property should be hidden in table views
   */
  boolean isHidden();

  /**
   * @return the maximum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  Number maximumValue();

  /**
   * @return the minimum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  Number minimumValue();

  /**
   * @return the maximum number of fraction digits to use for this property value,
   * -1 if this property is not based on Types.DOUBLE or Types.DECIMAL
   * @see #decimalRoundingMode()
   */
  int maximumFractionDigits();

  /**
   * @return the rounding mode to use when working with decimal values
   * @see #DECIMAL_ROUNDING_MODE
   * @see #maximumFractionDigits()
   */
  RoundingMode decimalRoundingMode();

  /**
   * @return true if null is a valid value for this property
   */
  boolean isNullable();

  /**
   * The value of a derived property can not be set, as it's value is derived from other values
   * @return true if the value of this property is derived from one or more values
   */
  boolean isDerived();

  /**
   * @return the maximum length of this property value, -1 is returned if the maximum length is undefined,
   * this only applies to String (varchar) based properties
   */
  int maximumLength();

  /**
   * Returns the mnemonic associated with this property.
   * @return the mnemonic to use when creating a label for this property
   */
  Character mnemonic();

  /**
   * Returns the Format used when presenting values for this property, null if none has been specified.
   * @return the Format object used to format the value of properties when being presented
   */
  Format format();

  /**
   * Returns the date time format pattern used when presenting and inputting values for this property.
   * @return the date/time format pattern
   */
  String dateTimePattern();

  /**
   * Returns the date time formatter used when presenting and inputting values for this property.
   * @return the DateTimeFormatter for this property or null if this is not a date/time based property
   */
  DateTimeFormatter dateTimeFormatter();

  /**
   * @return the Comparator to use when comparing values associated with this property
   */
  Comparator<T> comparator();

  /**
   * Supplies values, for example default ones.
   * @param <T> the value type
   */
  interface ValueSupplier<T> extends Supplier<T>, Serializable {}

  /**
   * Builds a Property instance
   * @param <T> the value type
   * @param <B> the builder type
   */
  interface Builder<T, B extends Builder<T, B>> {

    /**
     * @return the underying attribute
     */
    Attribute<T> attribute();

    /**
     * Specifies the key to use when retrieving the caption for this property from the entity resource bundle,
     * in case it differes from the attribute name ({@link Attribute#name()}), which is the default value.
     * Note that this configures the property to not be hidden.
     * @param captionResourceKey the caption resource bundle key
     * @return this builder instance
     * @throws IllegalStateException in case the caption has already been set
     * @throws IllegalStateException in case no resource bundle is specified for the entity
     * @throws IllegalStateException in case the caption resource is not found in the entity resource bundle
     * @see EntityType#resourceBundleName()
     */
    B captionResourceKey(String captionResourceKey);

    /**
     * Sets the bean name property to associate with this property
     * @param beanProperty the bean property name
     * @return this builder instance
     */
    B beanProperty(String beanProperty);

    /**
     * Sets the default value for this property, overrides the underlying column default value, if any
     * @param defaultValue the value to use as default
     * @return this builder instance
     */
    B defaultValue(T defaultValue);

    /**
     * Sets the default value supplier, use in case of dynamic default values.
     * @param supplier the default value supplier
     * @return this builder instance
     */
    B defaultValueSupplier(ValueSupplier<T> supplier);

    /**
     * Specifies whether this property should be hidden in table views
     * @param hidden true if this property should be hidden
     * @return this builder instance
     */
    B hidden(boolean hidden);

    /**
     * Only applicable to numerical properties
     * @param minimumValue the minimum allowed value for this property
     * @return this builder instance
     * @throws IllegalStateException in case this is not a numerical property
     */
    B minimumValue(Number minimumValue);

    /**
     * Only applicable to numerical properties
     * @param maximumValue the maximum allowed value for this property
     * @return this builder instance
     * @throws IllegalStateException in case this is not a numerical property
     */
    B maximumValue(Number maximumValue);

    /**
     * Only applicable to numerical properties
     * @param minimumValue the minimum allowed value for this property
     * @param maximumValue the maximum allowed value for this property
     * @return this builder instance
     * @throws IllegalStateException in case this is not a numerical property
     */
    B valueRange(Number minimumValue, Number maximumValue);

    /**
     * Sets the maximum fraction digits to show for this property, only applicable to properties based on decimal types.
     * This setting is overridden during subsequent calls to {@link #format(Format)}.
     * Note that values associated with this property are automatically rounded to {@code maximumFractionDigits} digits.
     * @param maximumFractionDigits the maximum fraction digits
     * @return this builder instance
     * @throws IllegalStateException in case this is not a decimal property
     * @see #decimalRoundingMode(RoundingMode)
     */
    B maximumFractionDigits(int maximumFractionDigits);

    /**
     * Sets the rounding mode to use when working with decimals
     * @param decimalRoundingMode the rounding mode
     * @return this builder instance
     * @throws IllegalStateException in case this is not a decimal property
     * @see #maximumFractionDigits(int)
     */
    B decimalRoundingMode(RoundingMode decimalRoundingMode);

    /**
     * Specifies whether to use number grouping when presenting the value associated with this property.
     * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
     * By default, grouping is not used.
     * Only applicable to numerical properties.
     * This setting is overridden during subsequent calls to {@link #format(Format)}
     * @param numberFormatGrouping if true then number grouping is used
     * @return this builder instance
     * @throws IllegalStateException in case this is not a numerical property
     */
    B numberFormatGrouping(boolean numberFormatGrouping);

    /**
     * Specifies whether this property is nullable. Note that this will not prevent
     * the value from being set to null, only prevent successful validation of the entity.
     * @param nullable specifies whether null is a valid value for this property
     * @return this builder instance
     */
    B nullable(boolean nullable);

    /**
     * Sets the maximum length of this property value, this applies to String (varchar) based properties
     * @param maximumLength the maximum length
     * @return this builder instance
     * @throws IllegalStateException in case this is not a String property
     */
    B maximumLength(int maximumLength);

    /**
     * Sets the mnemonic to use when creating a label for this property
     * @param mnemonic the mnemonic character
     * @return this builder instance
     */
    B mnemonic(Character mnemonic);

    /**
     * Sets the description for this property, used for tooltips f.ex.
     * @param description a String describing this property
     * @return this builder instance
     */
    B description(String description);

    /**
     * @param comparator the Comparator to use when comparing values for this attribute
     * @return this builder instance
     */
    B comparator(Comparator<T> comparator);

    /**
     * Sets the Format to use when presenting property values
     * @param format the format to use
     * @return this builder instance
     * @throws NullPointerException in case format is null
     * @throws IllegalArgumentException in case this is a numerical property and the given format is not a NumberFormat.
     * @throws IllegalStateException if the underlying attribute is temporal, in which case
     * {@link #dateTimePattern(String)} or {@link #localeDateTimePattern(LocaleDateTimePattern)} should be used.
     */
    B format(Format format);

    /**
     * Sets the date/time format pattern used when presenting and inputtind values
     * @param dateTimePattern the format pattern
     * @return this builder instance
     * @throws IllegalArgumentException in case the pattern is invalid
     * @throws IllegalStateException in case this is not a temporal property
     * @throws IllegalStateException in case {@link #localeDateTimePattern(LocaleDateTimePattern)} has been set
     */
    B dateTimePattern(String dateTimePattern);

    /**
     * Sets the locale aware date/time format pattern used when presenting and inputting values
     * @param localeDateTimePattern the format pattern
     * @return this builder instance
     * @throws IllegalStateException in case this is not a temporal property
     * @throws IllegalStateException in case {@link #dateTimePattern(String)} has been set
     */
    B localeDateTimePattern(LocaleDateTimePattern localeDateTimePattern);

    /**
     * Builds a new Property instance
     * @return a new property instance based on this builder
     */
    Property<T> build();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance.
   * @param attribute the attribute
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> columnProperty(Column<T> attribute) {
    return columnProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> columnProperty(Column<T> attribute, String caption) {
    return new DefaultColumnProperty.DefaultColumnPropertyBuilder<>(attribute, caption);
  }

  /**
   * A convenience method for creating a new {@link ColumnProperty.Builder} instance,
   * with the primary key index set to 0.
   * @param attribute the attribute
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder} with primary key index 0
   */
  static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> primaryKeyProperty(Column<T> attribute) {
    return primaryKeyProperty(attribute, null);
  }

  /**
   * A convenience method for creating a new {@link ColumnProperty.Builder} instance,
   * with the primary key index set to 0.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder} with primary key index 0
   */
  static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> primaryKeyProperty(Column<T> attribute, String caption) {
    return (ColumnProperty.Builder<T, B>) columnProperty(attribute, caption).primaryKeyIndex(0);
  }

  /**
   * Instantiates a {@link ForeignKeyProperty.Builder} instance.
   * @param foreignKey the foreign key
   * @return a new {@link ForeignKeyProperty.Builder}
   */
  static ForeignKeyProperty.Builder foreignKeyProperty(ForeignKey foreignKey) {
    return foreignKeyProperty(foreignKey, null);
  }

  /**
   * Instantiates a {@link ForeignKeyProperty.Builder} instance.
   * @param foreignKey the foreign key
   * @param caption the caption
   * @return a new {@link ForeignKeyProperty.Builder}
   */
  static ForeignKeyProperty.Builder foreignKeyProperty(ForeignKey foreignKey, String caption) {
    return new DefaultForeignKeyProperty.DefaultForeignKeyPropertyBuilder(foreignKey, caption);
  }

  /**
   * Instantiates a {@link Property.Builder} instance, for displaying a value from a referenced entity attribute.
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @param attribute the attribute
   * @param entityAttribute the entity attribute from which this property gets its value
   * @param denormalizedAttribute the attribute from the referenced entity, from which this property gets its value
   * @return a new {@link Property.Builder}
   */
  static <T, B extends Property.Builder<T, B>> Property.Builder<T, B> denormalizedProperty(Attribute<T> attribute,
                                                                                           Attribute<Entity> entityAttribute,
                                                                                           Attribute<T> denormalizedAttribute) {
    return denormalizedProperty(attribute, null, entityAttribute, denormalizedAttribute);
  }

  /**
   * Instantiates a {@link Property.Builder} instance, for displaying a value from a referenced entity attribute.
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @param attribute the attribute
   * @param caption the caption of this property
   * @param entityAttribute the entity attribute from which this property gets its value
   * @param denormalizedAttribute the attribute from the referenced entity, from which this property gets its value
   * @return a new {@link Property.Builder}
   */
  static <T, B extends Property.Builder<T, B>> Property.Builder<T, B> denormalizedProperty(Attribute<T> attribute, String caption,
                                                                                           Attribute<Entity> entityAttribute,
                                                                                           Attribute<T> denormalizedAttribute) {
    return new DefaultDerivedProperty.DefaultDerivedPropertyBuilder<>(attribute, caption,
            new DenormalizedValueProvider<>(entityAttribute, denormalizedAttribute), entityAttribute);
  }

  /**
   * Instantiates a {@link Property.Builder} instance, which value is derived from one or more source attributes.
   * @param attribute the attribute
   * @param valueProvider a {@link DerivedProperty.Provider} instance responsible for deriving the value
   * @param sourceAttributes the attributes from which this property derives its value
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link Property.Builder}
   * @throws IllegalArgumentException in case no source properties are specified
   */
  static <T, B extends Property.Builder<T, B>> Property.Builder<T, B> derivedProperty(Attribute<T> attribute,
                                                                                      DerivedProperty.Provider<T> valueProvider,
                                                                                      Attribute<?>... sourceAttributes) {
    return derivedProperty(attribute, null, valueProvider, sourceAttributes);
  }

  /**
   * Instantiates a {@link Property.Builder} instance, which value is derived from one or more source attributes.
   * @param attribute the attribute
   * @param caption the caption
   * @param valueProvider a {@link DerivedProperty.Provider} instance responsible for deriving the value
   * @param sourceAttributes the ids of the properties from which this property derives its value
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link Property.Builder}
   * @throws IllegalArgumentException in case no source properties are specified
   */
  static <T, B extends Property.Builder<T, B>> Property.Builder<T, B> derivedProperty(Attribute<T> attribute, String caption,
                                                                                      DerivedProperty.Provider<T> valueProvider,
                                                                                      Attribute<?>... sourceAttributes) {
    return new DefaultDerivedProperty.DefaultDerivedPropertyBuilder<>(attribute, caption, valueProvider, sourceAttributes);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on a subquery.
   * @param attribute the attribute
   * @param subquery the sql query
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> subqueryProperty(Column<T> attribute, String subquery) {
    return subqueryProperty(attribute, null, subquery);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on a subquery.
   * @param attribute the attribute
   * @param caption the property caption
   * @param subquery the sql query
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> subqueryProperty(Column<T> attribute, String caption,
                                                                                                   String subquery) {
    return new DefaultColumnProperty.DefaultSubqueryPropertyBuilder<>(attribute, caption, subquery);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on the given items.
   * @param attribute the attribute
   * @param validItems the Items representing all the valid values for this property
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   * @throws IllegalArgumentException in case the valid item list contains duplicate values
   */
  static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> itemProperty(Column<T> attribute, List<Item<T>> validItems) {
    return itemProperty(attribute, null, validItems);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on the given items.
   * @param attribute the attribute
   * @param caption the property caption
   * @param validItems the Items representing all the valid values for this property
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   * @throws IllegalArgumentException in case the valid item list contains duplicate values
   */
  static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> itemProperty(Column<T> attribute, String caption,
                                                                                               List<Item<T>> validItems) {
    return new DefaultItemProperty.DefaultItemPropertyBuilder<>(attribute, caption, validItems);
  }

  /**
   * Creates a new {@link TransientProperty.Builder} instance, which does not map to an underlying table column.
   * @param attribute the attribute
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link TransientProperty.Builder}
   */
  static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> transientProperty(Attribute<T> attribute) {
    return transientProperty(attribute, null);
  }

  /**
   * Creates a new {@link TransientProperty.Builder} instance, which does not map to an underlying table column.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link TransientProperty.Builder}
   */
  static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> transientProperty(Attribute<T> attribute, String caption) {
    return new DefaultTransientProperty.DefaultTransientPropertyBuilder<>(attribute, caption);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance representing a Boolean value.
   * @param <C> the column type
   * @param <B> the builder type
   * @param attribute the attribute
   * @param columnClass the underlying column data type class
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new {@link ColumnProperty.Builder}
   */
  static <C, B extends ColumnProperty.Builder<Boolean, B>> ColumnProperty.Builder<Boolean, B> booleanProperty(Column<Boolean> attribute, Class<C> columnClass,
                                                                                                              C trueValue, C falseValue) {
    return booleanProperty(attribute, null, columnClass, trueValue, falseValue);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance representing a Boolean value.
   * @param <C> the column type
   * @param <B> the builder type
   * @param columnClass the underlying column data type class
   * @param attribute the attribute
   * @param caption the property caption
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new {@link ColumnProperty.Builder}
   */
  static <C, B extends ColumnProperty.Builder<Boolean, B>> ColumnProperty.Builder<Boolean, B> booleanProperty(Column<Boolean> attribute, String caption,
                                                                                                              Class<C> columnClass, C trueValue, C falseValue) {
    return (ColumnProperty.Builder<Boolean, B>) new DefaultColumnProperty.DefaultColumnPropertyBuilder<>(attribute, caption)
            .columnClass(columnClass, booleanValueConverter(trueValue, falseValue));
  }

  /**
   * Creates a new {@link BlobProperty.Builder} instance.
   * @param attribute the attribute
   * @return a new {@link BlobProperty.Builder}
   */
  static BlobProperty.Builder blobProperty(Column<byte[]> attribute) {
    return blobProperty(attribute, null);
  }

  /**
   * Creates a new {@link BlobProperty.Builder} instance.
   * @param attribute the attribute
   * @param caption the property caption
   * @return a new {@link BlobProperty.Builder}
   */
  static BlobProperty.Builder blobProperty(Column<byte[]> attribute, String caption) {
    return new DefaultBlobProperty.DefaultBlobPropertyBuilder(attribute, caption);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was inserted.
   * @param attribute the attribute
   * @param <T> the Temporal type to base this property on
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditInsertTimeProperty(Column<T> attribute) {
    return auditInsertTimeProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was inserted.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the Temporal type to base this property on
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditInsertTimeProperty(Column<T> attribute, String caption) {
    return new DefaultAuditProperty.DefaultAuditPropertyBuilder<>(attribute, caption, INSERT);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was updated.
   * @param attribute the attribute
   * @param <T> the Temporal type to base this property on
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditUpdateTimeProperty(Column<T> attribute) {
    return auditUpdateTimeProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was updated.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the Temporal type to base this property on
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditUpdateTimeProperty(Column<T> attribute, String caption) {
    return new DefaultAuditProperty.DefaultAuditPropertyBuilder<>(attribute, caption, UPDATE);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who inserted a record.
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditInsertUserProperty(Column<String> attribute) {
    return auditInsertUserProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who inserted a record.
   * @param attribute the attribute
   * @param <B> the builder type
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditInsertUserProperty(Column<String> attribute, String caption) {
    return new DefaultAuditProperty.DefaultAuditPropertyBuilder<>(attribute, caption, INSERT);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who updated a record.
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditUpdateUserProperty(Column<String> attribute) {
    return auditUpdateUserProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who updated a record.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditUpdateUserProperty(Column<String> attribute, String caption) {
    return new DefaultAuditProperty.DefaultAuditPropertyBuilder<>(attribute, caption, UPDATE);
  }

  /**
   * Creates a new {@link ColumnProperty.ValueConverter} instance for converting a column value
   * representing a boolean value to and from an actual Boolean.
   * @param trueValue the value used to represent 'true' in the underlying database, may not be null
   * @param falseValue the value used to represent 'false' in the underlying database, may not be null
   * @param <T> the type of the value used to represent a boolean
   * @return a value converter for converting an underlying database representation
   * of a boolean value into an actual Boolean
   */
  static <T> ColumnProperty.ValueConverter<Boolean, T> booleanValueConverter(T trueValue, T falseValue) {
    return new DefaultColumnProperty.BooleanValueConverter<>(trueValue, falseValue);
  }

  /**
   * Returns a new Comparator instance for sorting Property instances by caption,
   * or if that is not available, attribute name, ignoring case
   * @return a new Comparator instance
   */
  static Comparator<Property<?>> propertyComparator() {
    return new AbstractProperty.PropertyComparator();
  }
}
