/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Configuration;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.value.PropertyValue;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Specifies a Property.
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
          .hoursMinutes().build().getTimePattern());

  /**
   * The default date/time format pattern to use when showing date/time values in tables and when creating default date/time input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy HH:mm [month/day order is locale specific]
   */
  PropertyValue<String> DATE_TIME_FORMAT = Configuration.stringValue("codion.domain.dateTimeFormat", LocaleDateTimePattern.builder()
          .delimiterDash().yearFourDigits().hoursMinutes().build().getDateTimePattern());

  /**
   * The default date format pattern to use when showing date values in tables and when creating default date input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy [month/day order is locale specific]
   */
  PropertyValue<String> DATE_FORMAT = Configuration.stringValue("codion.domain.dateFormat", LocaleDateTimePattern.builder()
          .delimiterDash().yearFourDigits().build().getDatePattern());

  /**
   * Specifies the default foreign key fetch depth<br>
   * Value type: Integer<br>
   * Default value: 1
   */
  PropertyValue<Integer> FOREIGN_KEY_FETCH_DEPTH = Configuration.integerValue("codion.domain.foreignKeyFetchDepth", DEFAULT_FOREIGN_KEY_FETCH_DEPTH);

  /**
   * Specifies the wildcard character used by the framework<br>
   * Value type: String<br>
   * Default value: %
   */
  PropertyValue<String> WILDCARD_CHARACTER = Configuration.stringValue("codion.wildcardCharacter", "%");

  /**
   * The {@link Attribute} this property is based on, should be unique within an Entity.
   * By default the {@link Attribute#getName()} serves as column name for database properties.
   * @return the attribute this property is based on
   */
  Attribute<T> getAttribute();

  /**
   * @return the type of the entity this Property is associated with
   */
  EntityType<?> getEntityType();

  /**
   * @return the caption
   */
  String getCaption();

  /**
   * @return a String describing this attribute
   */
  String getDescription();

  /**
   * @return the bean property name associated with this property
   */
  String getBeanProperty();

  /**
   * Prepares the value according to the property configuration, such as rounding
   * to the correct number of fraction digits in case of doubles
   * @param value the value to prepare
   * @return the prepared value
   */
  T prepareValue(T value);

  /**
   * Returns a value formatted with this property's format. If no format is specified {@link Object#toString()} is used.
   * Null values result in an empty string.
   * @param value the value to format.
   * @return the value formatted as a string
   * @see Property.Builder#format(Format)
   * @see Property.Builder#dateTimePattern(String)
   */
  String formatValue(T value);

  /**
   * @return true if a default value has been set for this property
   */
  boolean hasDefaultValue();

  /**
   * @return the default value for this property, if no default value has been set null is returned
   * @see #hasDefaultValue()
   */
  T getDefaultValue();

  /**
   * @return true if this property should be hidden in table views
   */
  boolean isHidden();

  /**
   * @return the maximum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  Double getMaximumValue();

  /**
   * @return the minimum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  Double getMinimumValue();

  /**
   * @return the maximum number of fraction digits to use for this property value,
   * -1 if this property is not based on Types.DOUBLE or Types.DECIMAL
   * @see #getDecimalRoundingMode()
   */
  int getMaximumFractionDigits();

  /**
   * @return the rounding mode to use when working with decimal values
   * @see #DECIMAL_ROUNDING_MODE
   * @see #getMaximumFractionDigits()
   */
  RoundingMode getDecimalRoundingMode();

  /**
   * @return the preferred column width of this property in pixels when presented in a table, 0 if none has been specified
   */
  int getPreferredColumnWidth();

  /**
   * @return true if values associated with this property can be set null
   */
  boolean isNullable();

  /**
   * @return the maximum length of this property value, -1 is returned if the max length is undefined,
   * this applies to String (varchar) based properties
   */
  int getMaximumLength();

  /**
   * Returns the mnemonic associated with this property.
   * @return the mnemonic to use when creating a label for this property
   */
  Character getMnemonic();

  /**
   * Returns the Format used when presenting values for this property.
   * @return the Format object used to format the value of properties when being presented
   */
  Format getFormat();

  /**
   * Returns the date time format pattern used when presenting and inputting values for this property.
   * @return the date/time format pattern
   */
  String getDateTimePattern();

  /**
   * Returns the date time formatter used when presenting and inputting values for this property.
   * @return the DateTimeFormatter for this property or null if this is not a date/time based property
   */
  DateTimeFormatter getDateTimeFormatter();

  /**
   * Supplies values, for example default ones.
   * @param <T> the value type
   */
  interface ValueSupplier<T> extends Supplier<T>, Serializable {}

  /**
   * Builds a Property instance
   * @param <T> the property value type
   */
  interface Builder<T> {

    /**
     * Returns the underlying property.
     * @return the property
     */
    Property<T> get();

    /**
     * Specifies the resource bundle from which to retrieve the caption
     * for this property, assuming the resource key is the attribute name ({@link Attribute#getName()}).
     * Note that this sets the property to be not hidden.
     * @param captionResourceKey the caption resource bundle key
     * @return this instance
     * @throws IllegalStateException in case the caption has already been set
     * @see EntityType#getResourceBundleName()
     */
    Property.Builder<T> captionResourceKey(String captionResourceKey);

    /**
     * Sets the bean name property to associate with this property
     * @param beanProperty the bean property name
     * @return this instance
     */
    Property.Builder<T> beanProperty(String beanProperty);

    /**
     * Sets the default value for this property, overrides the underlying column default value, if any
     * @param defaultValue the value to use as default
     * @return this instance
     */
    Property.Builder<T> defaultValue(T defaultValue);

    /**
     * Sets the default value supplier, use in case of dynamic default values.
     * @param supplier the default value supplier
     * @return this instance
     */
    Property.Builder<T> defaultValueSupplier(ValueSupplier<T> supplier);

    /**
     * Specifies that this property should be hidden in table views
     * @return this instance
     */
    Property.Builder<T> hidden();

    /**
     * Specifies whether this property should be hidden in table views
     * @param hidden true if this property should be hidden
     * @return this instance
     */
    Property.Builder<T> hidden(boolean hidden);

    /**
     * Sets the maximum allowed value for this property, only applicable to numerical properties
     * @param maximumValue the maximum allowed value
     * @return this instance
     */
    Property.Builder<T> maximumValue(double maximumValue);

    /**
     * Only applicable to numerical properties
     * @param minimumValue the minimum allowed value for this property
     * @return this instance
     */
    Property.Builder<T> minimumValue(double minimumValue);

    /**
     * Sets the maximum fraction digits to show for this property, only applicable to properties based on decimal types.
     * This setting is overridden during subsequent calls to {@link #format(Format)}.
     * Note that values associated with this property are automatically rounded to {@code maximumFractionDigits} digits.
     * @param maximumFractionDigits the maximum fraction digits
     * @return this instance
     * @throws IllegalStateException in case the underlying attribute is not a decimal type
     * @see #decimalRoundingMode(RoundingMode)
     */
    Property.Builder<T> maximumFractionDigits(int maximumFractionDigits);

    /**
     * Sets the rounding mode to use when working with decimals
     * @param decimalRoundingMode the rounding mode
     * @return this instance
     * @throws IllegalStateException in case the underlying attribute is not a decimal
     * @see #maximumFractionDigits(int)
     */
    Property.Builder<T> decimalRoundingMode(RoundingMode decimalRoundingMode);

    /**
     * Specifies whether to use number grouping when presenting the value associated with this property.
     * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
     * By default grouping is not used.
     * Only applicable to numerical properties.
     * This setting is overridden during subsequent calls to {@link #format(Format)}
     * @param numberFormatGrouping if true then number grouping is used
     * @return this instance
     */
    Property.Builder<T> numberFormatGrouping(boolean numberFormatGrouping);

    /**
     * Specifies the preferred column width when displaying this property in a table.
     * @param preferredColumnWidth the preferred column width of this property in pixels when displayed in a table
     * @return this instance
     */
    Property.Builder<T> preferredColumnWidth(int preferredColumnWidth);

    /**
     * Specifies whether or not this property is nullable. Note that this will not prevent
     * the value from being set to null, only prevent successful validation of the entity.
     * @param nullable specifies whether or not this property accepts a null value
     * @return this instance
     */
    Property.Builder<T> nullable(boolean nullable);

    /**
     * Sets the maximum length of this property value, this applies to String (varchar) based properties
     * @param maximumLength the maximum length
     * @return this instance
     */
    Property.Builder<T> maximumLength(int maximumLength);

    /**
     * Sets the mnemonic to use when creating a label for this property
     * @param mnemonic the mnemonic character
     * @return this instance
     */
    Property.Builder<T> mnemonic(Character mnemonic);

    /**
     * Sets the description for this property, used for tooltips f.ex.
     * @param description a String describing this property
     * @return this instance
     */
    Property.Builder<T> description(String description);

    /**
     * Sets the Format to use when presenting property values
     * @param format the format to use
     * @return this instance
     * @throws NullPointerException in case format is null
     * @throws IllegalArgumentException in case the underlying attribute is numerical
     * and the given format is not a NumberFormat.
     * @throws IllegalStateException if the underlying attribute is temporal, in which case
     * {@link #dateTimePattern(String)} or {@link #localeDateTimePattern(LocaleDateTimePattern)} should be used.
     */
    Property.Builder<T> format(Format format);

    /**
     * Sets the date/time format pattern used when presenting and inputtind values
     * @param dateTimePattern the format pattern
     * @return this instance
     * @throws IllegalArgumentException in case the pattern is invalid
     * @throws IllegalStateException in case the underlying attribute is not a date/time based one
     * @throws IllegalStateException in case {@link #localeDateTimePattern(LocaleDateTimePattern)} has been set
     */
    Property.Builder<T> dateTimePattern(String dateTimePattern);

    /**
     * Sets the locale aware date/time format pattern used when presenting and inputting values
     * @param localeDateTimePattern the format pattern
     * @return this instance
     * @throws IllegalStateException in case the underlying attribute is not a date/time based one
     * @throws IllegalStateException in case {@link #dateTimePattern(String)} has been set
     */
    Property.Builder<T> localeDateTimePattern(LocaleDateTimePattern localeDateTimePattern);
  }
}
