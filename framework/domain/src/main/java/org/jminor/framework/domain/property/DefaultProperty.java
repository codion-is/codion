/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.Formats;
import org.jminor.common.Util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.requireNonNull;

/**
 * A default Property implementation
 */
abstract class DefaultProperty implements Property {

  private static final long serialVersionUID = 1;

  private static final Supplier<Object> DEFAULT_VALUE_PROVIDER = new NullDefaultValueProvider();

  /**
   * The ID of the entity this property is associated with
   */
  private String entityId;

  /**
   * The property identifier, should be unique within an Entity.
   * By default this ID serves as column name for database properties.
   * @see #getPropertyId()
   */
  private final String propertyId;

  /**
   * The property type, java.sql.Types
   */
  private final int type;

  /**
   * The class representing the values associated with this property
   */
  private final Class typeClass;

  /**
   * The caption to use when this property is presented
   */
  private final String caption;

  /**
   * This is based on an immutable field, so cache it
   */
  private final int hashCode;

  /**
   * The name of a bean property linked to this property, if any
   */
  private String beanProperty;

  /**
   * The default value provider for this property
   */
  private Supplier<Object> defaultValueProvider = DEFAULT_VALUE_PROVIDER;

  /**
   * True if the value of this property is allowed to be null
   */
  private boolean nullable = true;

  /**
   * The preferred column width when this property is presented in a table
   */
  private int preferredColumnWidth = -1;

  /**
   * True if this property should be hidden in table views
   */
  private boolean hidden;

  /**
   * The maximum length of the data.
   * Only applicable to string based properties.
   */
  private int maximumLength = -1;

  /**
   * The maximum value for this property.
   * Only applicable to numerical properties
   */
  private Double maximumValue;

  /**
   * The minimum value for this property.
   * Only applicable to numerical properties
   */
  private Double minimumValue;

  /**
   * A string describing this property
   */
  private String description;

  /**
   * A mnemonic to use when creating a label for this property
   */
  private Character mnemonic;

  /**
   * The Format used when presenting the value of this property
   */
  private Format format;

  /**
   * The date/time format pattern
   */
  private String dateTimeFormatPattern;

  /**
   * The DateTimeFormatter to use, based on dateTimeFormatPattern
   */
  private transient DateTimeFormatter dateTimeFormatter;

  /**
   * @param propertyId the property ID, this is used as the underlying column name
   * @param type the data type of this property
   * @param caption the caption of this property, if this is null then this property is defined as hidden
   * @param typeClass the type associated with this property
   */
  DefaultProperty(final String propertyId, final int type, final String caption,
                  final Class typeClass) {
    requireNonNull(propertyId, "propertyId");
    this.propertyId = propertyId;
    this.hashCode = propertyId.hashCode();
    this.type = type;
    this.caption = caption;
    this.typeClass = typeClass;
    this.hidden = caption == null;
    this.format = initializeDefaultFormat();
    this.dateTimeFormatPattern = getDefaultDateTimeFormatPattern();
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getCaption();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean is(final String propertyId) {
    return this.propertyId.equals(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean is(final Property property) {
    return is(property.getPropertyId());
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNumerical() {
    return isInteger() || isDecimal() || isLong();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isTemporal() {
    return isDate() || isTimestamp() || isTime();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDate() {
    return isType(Types.DATE);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isTimestamp() {
    return isType(Types.TIMESTAMP);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isTime() {
    return isType(Types.TIME);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCharacter() {
    return isType(Types.CHAR);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isString() {
    return isType(Types.VARCHAR);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isLong() {
    return isType(Types.BIGINT);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isInteger() {
    return isType(Types.INTEGER);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDouble() {
    return isType(Types.DOUBLE);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isBigDecimal() {
    return isType(Types.DECIMAL);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDecimal() {
    return isDouble() || isBigDecimal();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isBoolean() {
    return isType(Types.BOOLEAN);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isBlob() {
    return isType(Types.BLOB);
  }

  /** {@inheritDoc} */
  @Override
  public final String getPropertyId() {
    return propertyId;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final int getType() {
    return type;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isType(final int type) {
    return this.type == type;
  }

  /** {@inheritDoc} */
  @Override
  public final String getBeanProperty() {
    return beanProperty;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isHidden() {
    return hidden;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean hasDefaultValue() {
    return !(this.defaultValueProvider instanceof NullDefaultValueProvider);
  }

  /** {@inheritDoc} */
  @Override
  public final Object getDefaultValue() {
    return this.defaultValueProvider.get();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNullable() {
    return nullable;
  }

  /** {@inheritDoc} */
  @Override
  public final int getMaximumLength() {
    return maximumLength;
  }

  /** {@inheritDoc} */
  @Override
  public final Double getMaximumValue() {
    return maximumValue;
  }

  /** {@inheritDoc} */
  @Override
  public final Double getMinimumValue() {
    return minimumValue;
  }

  /** {@inheritDoc} */
  @Override
  public final int getPreferredColumnWidth() {
    return preferredColumnWidth;
  }

  /** {@inheritDoc} */
  @Override
  public final String getDescription() {
    return description;
  }

  /** {@inheritDoc} */
  @Override
  public final Character getMnemonic() {
    return mnemonic;
  }

  /** {@inheritDoc} */
  @Override
  public final Format getFormat() {
    return format;
  }

  /** {@inheritDoc} */
  @Override
  public final String getDateTimeFormatPattern() {
    return dateTimeFormatPattern;
  }

  /** {@inheritDoc} */
  @Override
  public final DateTimeFormatter getDateTimeFormatter() {
    if (dateTimeFormatter == null && dateTimeFormatPattern != null) {
      dateTimeFormatter = ofPattern(dateTimeFormatPattern);
    }

    return dateTimeFormatter;
  }

  /** {@inheritDoc} */
  @Override
  public final int getMaximumFractionDigits() {
    if (!(format instanceof NumberFormat)) {
      throw new IllegalStateException("Maximum fraction digits are only apply to numerical formats");
    }

    return ((NumberFormat) format).getMaximumFractionDigits();
  }

  /** {@inheritDoc} */
  @Override
  public final String getCaption() {
    return caption == null ? propertyId : caption;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean equals(final Object obj) {
    return this == obj || obj instanceof Property && this.propertyId.equals(((Property) obj).getPropertyId());
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return hashCode;
  }

  /** {@inheritDoc} */
  @Override
  public final Class getTypeClass() {
    return typeClass;
  }

  /** {@inheritDoc} */
  @Override
  public Object validateType(final Object value) {
    if (value != null && typeClass != value.getClass() && !typeClass.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Value of type " + typeClass +
              " expected for property " + this + " in entity " + entityId + ", got: " + value.getClass());
    }

    return value;
  }

  /** {@inheritDoc} */
  @Override
  public final Object prepareValue(final Object value) {
    if (value instanceof Double) {
      return Util.roundDouble((Double) value, getMaximumFractionDigits());
    }
    if (value instanceof BigDecimal) {
      return ((BigDecimal) value).setScale(getMaximumFractionDigits(),
              Property.BIG_DECIMAL_ROUNDING_MODE.get()).stripTrailingZeros();
    }

    return value;
  }

  private Format initializeDefaultFormat() {
    if (isNumerical()) {
      final NumberFormat numberFormat = isInteger() || isLong() ?
              Formats.getNonGroupingIntegerFormat() : Formats.getNonGroupingNumberFormat();
      if (isBigDecimal()) {
        ((DecimalFormat) numberFormat).setParseBigDecimal(true);
      }
      if (isDecimal()) {
        numberFormat.setMaximumFractionDigits(Property.MAXIMUM_FRACTION_DIGITS.get());
      }

      return numberFormat;
    }

    return Formats.NULL_FORMAT;
  }

  private String getDefaultDateTimeFormatPattern() {
    if (isDate()) {
      return DATE_FORMAT.get();
    }
    else if (isTime()) {
      return TIME_FORMAT.get();
    }
    else if (isTimestamp()) {
      return TIMESTAMP_FORMAT.get();
    }

    return null;
  }

  /**
   * @param sqlType the type
   * @return the Class representing the given type
   */
  protected static Class getTypeClass(final int sqlType) {
    switch (sqlType) {
      case Types.BIGINT:
        return Long.class;
      case Types.INTEGER:
        return Integer.class;
      case Types.DOUBLE:
        return Double.class;
      case Types.DECIMAL:
        return BigDecimal.class;
      case Types.DATE:
        return LocalDate.class;
      case Types.TIME:
        return LocalTime.class;
      case Types.TIMESTAMP:
        return LocalDateTime.class;
      case Types.VARCHAR:
        return String.class;
      case Types.BOOLEAN:
        return Boolean.class;
      case Types.CHAR:
        return Character.class;
      case Types.BLOB:
        return byte[].class;
      default:
        return Object.class;
    }
  }

  private static class DefaultValueProvider implements Supplier<Object>, Serializable {

    private static final long serialVersionUID = 1;

    private final Object defaultValue;

    public DefaultValueProvider(final Object defaultValue) {
      this.defaultValue = defaultValue;
    }

    @Override
    public Object get() {
      return defaultValue;
    }
  }

  private static class NullDefaultValueProvider extends DefaultValueProvider {

    private static final long serialVersionUID = 1;

    public NullDefaultValueProvider() {
      super(null);
    }
  }

  abstract static class DefaultPropertyBuilder implements Property.Builder {

    protected final DefaultProperty property;

    DefaultPropertyBuilder(final DefaultProperty property) {
      this.property = property;
    }

    @Override
    public Property get() {
      return property;
    }

    @Override
    public Property.Builder entityId(final String entityId) {
      if (property.entityId != null) {
        throw new IllegalStateException("entityId (" + property.entityId +
                ") has already been set for property: " + property.propertyId);
      }
      property.entityId = entityId;
      return this;
    }

    @Override
    public final Property.Builder beanProperty(final String beanProperty) {
      property.beanProperty = requireNonNull(beanProperty, "beanProperty");
      return this;
    }

    @Override
    public final Property.Builder hidden(final boolean hidden) {
      property.hidden = hidden;
      return this;
    }

    @Override
    public final Property.Builder defaultValue(final Object defaultValue) {
      return defaultValueProvider(new DefaultValueProvider(defaultValue));
    }

    @Override
    public Property.Builder defaultValueProvider(final Supplier<Object> provider) {
      if (provider != null) {
        property.validateType(provider.get());
      }
      property.defaultValueProvider = provider == null ? DEFAULT_VALUE_PROVIDER : provider;
      return this;
    }

    @Override
    public Property.Builder nullable(final boolean nullable) {
      property.nullable = nullable;
      return this;
    }

    @Override
    public final Property.Builder maximumLength(final int maxLength) {
      if (!property.isString()) {
        throw new IllegalStateException("maximumLength is only applicable to string properties");
      }
      if (maxLength <= 0) {
        throw new IllegalArgumentException("Maximum length must be a positive integer");
      }
      property.maximumLength = maxLength;
      return this;
    }

    @Override
    public final Property.Builder maximumValue(final double maximumValue) {
      if (!property.isNumerical()) {
        throw new IllegalStateException("maximumValue is only applicable to numerical properties");
      }
      if (property.minimumValue != null && property.minimumValue > maximumValue) {
        throw new IllegalArgumentException("Maximum value must be larger than minimum value");
      }
      property.maximumValue = maximumValue;
      return this;
    }

    @Override
    public final Property.Builder minimumValue(final double minimumValue) {
      if (!property.isNumerical()) {
        throw new IllegalStateException("minimumValue is only applicable to numerical properties");
      }
      if (property.maximumValue != null && property.maximumValue < minimumValue) {
        throw new IllegalArgumentException("Minimum value must be smaller than maximum value");
      }
      property.minimumValue = minimumValue;
      return this;
    }

    @Override
    public final Property.Builder useNumberFormatGrouping(final boolean useGrouping) {
      if (!property.isNumerical()) {
        throw new IllegalStateException("useNumberFormatGrouping is only applicable to numerical properties");
      }
      ((NumberFormat) property.format).setGroupingUsed(useGrouping);
      return this;
    }

    @Override
    public final Property.Builder preferredColumnWidth(final int preferredColumnWidth) {
      property.preferredColumnWidth = preferredColumnWidth;
      return this;
    }

    @Override
    public final Property.Builder description(final String description) {
      property.description = description;
      return this;
    }

    @Override
    public final Property.Builder mnemonic(final Character mnemonic) {
      property.mnemonic = mnemonic;
      return this;
    }

    @Override
    public final Property.Builder format(final Format format) {
      requireNonNull(format, "format");
      if (property.isNumerical() && !(format instanceof NumberFormat)) {
        throw new IllegalArgumentException("NumberFormat required for numerical property: " + property.propertyId);
      }
      if (property.isTemporal()) {
        throw new IllegalArgumentException("Use setDateTimeFormatPattern() for temporal properties: " + property.propertyId);
      }
      property.format = format;
      return this;
    }

    @Override
    public final Property.Builder dateTimeFormatPattern(final String dateTimeFormatPattern) {
      if (!property.isTemporal()) {
        throw new IllegalArgumentException("dateTimeFormatPattern is only applicable to temporal properties: " + property.propertyId);
      }
      property.dateTimeFormatter = ofPattern(dateTimeFormatPattern);
      property.dateTimeFormatPattern = dateTimeFormatPattern;
      return this;
    }

    @Override
    public final Property.Builder maximumFractionDigits(final int maximumFractionDigits) {
     if (!property.isDecimal()) {
        throw new IllegalStateException("maximumFractionDigits is only applicable to decimal properties");
      }
      ((NumberFormat) property.format).setMaximumFractionDigits(maximumFractionDigits);
      return this;
    }
  }
}