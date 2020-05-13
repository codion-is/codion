/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.domain.property;

import dev.codion.common.Configuration;
import dev.codion.common.value.PropertyValue;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Specifies a Property.
 */
public interface Property extends Serializable {

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
   * Specifies the default rounding mode used for BigDecimal property values<br>
   * Value type: Integer<br>
   * Default value: BigDecimal.ROUND_HALF_EVEN<br>
   * @see #MAXIMUM_FRACTION_DIGITS
   * @see Property.Builder#bigDecimalRoundingMode(int)
   */
  PropertyValue<Integer> BIG_DECIMAL_ROUNDING_MODE = Configuration.integerValue("codion.domain.bigDecimalRoundingMode", BigDecimal.ROUND_HALF_EVEN);

  /**
   * The date format pattern to use when showing time values in tables and when creating default time input fields<br>
   * Value type: String<br>
   * Default value: HH:mm
   */
  PropertyValue<String> TIME_FORMAT = Configuration.stringValue("codion.domain.timeFormat", "HH:mm");

  /**
   * The date format pattern to use when showing timestamp values in tables and when creating default timestamp input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy HH:mm
   */
  PropertyValue<String> TIMESTAMP_FORMAT = Configuration.stringValue("codion.domain.timestampFormat", "dd-MM-yyyy HH:mm");

  /**
   * The date format pattern to use when showing date values in tables and when creating default date input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy
   */
  PropertyValue<String> DATE_FORMAT = Configuration.stringValue("codion.domain.dateFormat", "dd-MM-yyyy");

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
   * @return the id of the entity this property is associated with
   */
  String getEntityId();

  /**
   * The property identifier, should be unique within an Entity.
   * By default this id serves as column name for database properties.
   * @return the id of this property
   */
  String getPropertyId();

  /**
   * @return the caption
   */
  String getCaption();

  /**
   * @return a String describing this attribute
   */
  String getDescription();

  /**
   * @return the Class representing the values of this attribute
   */
  Class getTypeClass();

  /**
   * @return the bean property name associated with this property
   */
  String getBeanProperty();

  /**
   * @param value the value to validate
   * @return the value
   * @throws IllegalArgumentException in case {@code value} is of a type incompatible with this property
   */
  Object validateType(Object value);

  /**
   * Prepares the value according to the property configuration, such as rounding
   * to the correct number of fraction digits in case of doubles
   * @param value the value to prepare
   * @return the prepared value
   */
  Object prepareValue(Object value);

  /**
   * Returns a value formatted with this property's format. If no format is specified {@link Object#toString()} is used.
   * Null values result in an empty string.
   * @param value the value to format.
   * @return the value formatted as a string
   * @see Property.Builder#format(Format)
   * @see Property.Builder#dateTimeFormatPattern(String)
   */
  String formatValue(Object value);

  /**
   * @param  propertyId the propertyId
   * @return true if this property is of the given type
   */
  boolean is(String propertyId);

  /**
   * @param property the property
   * @return true if this property is of the given type
   */
  boolean is(Property property);

  /**
   * @return the data type ({@link java.sql.Types}) of the value of this property
   */
  int getType();

  /**
   * @param type the type to check ({@link java.sql.Types})
   * @return true if the type of this property is the one given
   */
  boolean isType(int type);

  /**
   * @return true if this is a numerical Property, that is, Integer or Double
   */
  boolean isNumerical();

  /**
   * @return true if this is a time based property, Date (LocalDate), Timestamp (LocalDatetime) or Time (LocalTime)
   */
  boolean isTemporal();

  /**
   * @return true if this is a date property
   */
  boolean isDate();

  /**
   * @return true if this is a timestamp property
   */
  boolean isTimestamp();

  /**
   * @return true if this is a time property
   */
  boolean isTime();

  /**
   * @return true if this is a character property
   */
  boolean isCharacter();

  /**
   * @return true if this is a string property
   */
  boolean isString();

  /**
   * @return true if this is a long property
   */
  boolean isLong();

  /**
   * @return true if this is a integer property
   */
  boolean isInteger();

  /**
   * @return true if this is a double property
   */
  boolean isDouble();

  /**
   * @return true if this is a BigDecimal property
   */
  boolean isBigDecimal();

  /**
   * @return true if this is a decimal property
   */
  boolean isDecimal();

  /**
   * @return true if this is a boolean property
   */
  boolean isBoolean();

  /**
   * @return true if this is a blob property
   */
  boolean isBlob();

  /**
   * @return true if a default value has been set for this property
   */
  boolean hasDefaultValue();

  /**
   * @return the default value for this property, if no default value has been set null is returned
   * @see #hasDefaultValue()
   */
  Object getDefaultValue();

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
   * only applicable to properties based on Types.DOUBLE and Types.DECIMAL
   */
  int getMaximumFractionDigits();

  /**
   * @return the rounding mode to use when working with BigDecimal
   * @see #BIG_DECIMAL_ROUNDING_MODE
   */
  int getBigDecimalRoundingMode();

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
   * @return the mnemonic to use when creating a label for this property
   */
  Character getMnemonic();

  /**
   * @return the Format object used to format the value of properties when being presented
   */
  Format getFormat();

  /**
   * @return the date/time format pattern
   */
  String getDateTimeFormatPattern();

  /**
   * @return the DateTimeFormatter for this property or null if this is not a date/time based property
   */
  DateTimeFormatter getDateTimeFormatter();

  /**
   * Builds a Property instance
   */
  interface Builder {

    /**
     * @return the property
     */
    Property get();

    /**
     * @param entityId the id of the entity this property is associated with
     * @throws IllegalStateException in case the entityId has already been set
     * @return this instance
     */
    Property.Builder entityId(String entityId);

    /**
     * Sets the bean name property to associate with this property
     * @param beanProperty the bean property name
     * @return this instance
     */
    Property.Builder beanProperty(String beanProperty);

    /**
     * Sets the default value for this property, overrides the underlying column default value, if any
     * @param defaultValue the value to use as default
     * @return this instance
     */
    Property.Builder defaultValue(Object defaultValue);

    /**
     * Sets the default value supplier, use in case of dynamic default values.
     * @param supplier the default value supplier
     * @return this instance
     */
    Property.Builder defaultValueSupplier(Supplier<Object> supplier);

    /**
     * @param hidden specifies whether this property should hidden in table views
     * @return this instance
     */
    Property.Builder hidden(boolean hidden);

    /**
     * Sets the maximum allowed value for this property, only applicable to numerical properties
     * @param maximumValue the maximum allowed value
     * @return this instance
     */
    Property.Builder maximumValue(double maximumValue);

    /**
     * Only applicable to numerical properties
     * @param minimumValue the minimum allowed value for this property
     * @return this instance
     */
    Property.Builder minimumValue(double minimumValue);

    /**
     * Sets the maximum fraction digits to show for this property, only applicable to properties based on Types.DOUBLE.
     * This setting is overridden during subsequent calls to {@link #format(Format)}.
     * Note that values associated with this property are automatically rounded to {@code maximumFractionDigits} digits.
     * @param maximumFractionDigits the maximum fraction digits
     * @return this instance
     */
    Property.Builder maximumFractionDigits(int maximumFractionDigits);

    /**
     * Sets the rounding mode to use when working with BigDecimal
     * @param bigDecimalRoundingMode the rounding mode
     * @return this instance
     */
    Property.Builder bigDecimalRoundingMode(int bigDecimalRoundingMode);

    /**
     * Specifies whether to use number grouping when presenting the value associated with this property.
     * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
     * By default grouping is not used.
     * Only applicable to numerical properties.
     * This setting is overridden during subsequent calls to {@link #format(Format)}
     * @param numberFormatGrouping if true then number grouping is used
     * @return this instance
     */
    Property.Builder numberFormatGrouping(boolean numberFormatGrouping);

    /**
     * @param preferredColumnWidth the preferred column width of this property in pixels when displayed in a table
     * @return this instance
     */
    Property.Builder preferredColumnWidth(int preferredColumnWidth);

    /**
     * Specifies whether or not this property is nullable, in case of
     * properties that are parts of a ForeignKeyProperty inherit the nullable state of that property.
     * @param nullable specifies whether or not this property accepts a null value
     * @return this instance
     */
    Property.Builder nullable(boolean nullable);

    /**
     * Sets the maximum length of this property value, this applies to String (varchar) based properties
     * @param maxLength the maximum length
     * @return this instance
     */
    Property.Builder maximumLength(int maxLength);

    /**
     * Sets the mnemonic to use when creating a label for this property
     * @param mnemonic the mnemonic character
     * @return this instance
     */
    Property.Builder mnemonic(Character mnemonic);

    /**
     * @param description a String describing this property
     * @return this instance
     */
    Property.Builder description(String description);

    /**
     * Sets the Format to use when presenting property values
     * @param format the format to use
     * @return this instance
     * @throws NullPointerException in case format is null
     * @throws IllegalArgumentException in case the format does not fit the property type,
     * NumberFormat for example is expected for numerical properties
     */
    Property.Builder format(Format format);

    /**
     * Sets the date/time format pattern used when presenting values
     * @param dateTimeFormatPattern the format pattern
     * @return this instance
     * @throws IllegalArgumentException in case the pattern is invalid or if this property is not a date/time based one
     */
    Property.Builder dateTimeFormatPattern(String dateTimeFormatPattern);
  }
}
