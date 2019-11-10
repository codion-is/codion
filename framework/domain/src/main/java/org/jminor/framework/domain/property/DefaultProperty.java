/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.Formats;
import org.jminor.common.db.ValueConverter;

import java.math.BigDecimal;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.requireNonNull;

/**
 * A default Property implementation
 */
class DefaultProperty implements Property {

  private static final long serialVersionUID = 1;

  private static final ValueProvider DEFAULT_VALUE_PROVIDER = new DefaultValueProvider();

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
  private ValueProvider defaultValueProvider = DEFAULT_VALUE_PROVIDER;

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
  private boolean hidden = false;

  /**
   * True if this property is for selecting only, implicitly not updatable
   * and not used in insert statements
   */
  private boolean readOnly = false;

  /**
   * The maximum length of the data
   */
  private int maxLength = -1;

  /**
   * The maximum value for this property.
   * Only applicable to numerical properties
   */
  private Double max;

  /**
   * The minimum value for this property.
   * Only applicable to numerical properties
   */
  private Double min;

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
    setHidden(caption == null);
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
  public final String getPropertyId() {
    return propertyId;
  }

  /** {@inheritDoc} */
  @Override
  public String getEntityId() {
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
  public boolean isReadOnly() {
    return this.readOnly;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasDefaultValue() {
    return !(this.defaultValueProvider instanceof DefaultValueProvider);
  }

  /** {@inheritDoc} */
  @Override
  public final Object getDefaultValue() {
    return this.defaultValueProvider.getValue();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isNullable() {
    return nullable;
  }

  /** {@inheritDoc} */
  @Override
  public final int getMaxLength() {
    return maxLength;
  }

  /** {@inheritDoc} */
  @Override
  public final Double getMax() {
    return max;
  }

  /** {@inheritDoc} */
  @Override
  public final Double getMin() {
    return min;
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
      throw new IllegalStateException("Maximum fraction digits is only applicable for numerical formats");
    }

    return ((NumberFormat) format).getMaximumFractionDigits();
  }

  /** {@inheritDoc} */
  @Override
  public String getCaption() {
    if (caption == null) {
      return propertyId;
    }

    return caption;
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
  public void validateType(final Object value) {
    if (value != null && typeClass != value.getClass() && !typeClass.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Value of type " + typeClass +
              " expected for property " + this + " in entity " + entityId + ", got: " + value.getClass());
    }
  }

  void setEntityId(final String entityId) {
    if (this.entityId != null) {
      throw new IllegalStateException("entityId (" + this.entityId + ") has already been set for property: " + propertyId);
    }
    this.entityId = entityId;
  }

  void setBeanProperty(final String beanProperty) {
    this.beanProperty = requireNonNull(beanProperty, "beanProperty");
  }

  void setHidden(final boolean hidden) {
    this.hidden = hidden;
  }

  void setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
  }

  void setDefaultValue(final Object defaultValue) {
    setDefaultValueProvider(() -> defaultValue);
  }

  void setDefaultValueProvider(final ValueProvider provider) {
    if (provider != null) {
      validateType(provider.getValue());
    }
    this.defaultValueProvider = provider == null ? DEFAULT_VALUE_PROVIDER : provider;
  }

  void setNullable(final boolean nullable) {
    this.nullable = nullable;
  }

  void setMaxLength(final int maxLength) {
    if (maxLength <= 0) {
      throw new IllegalArgumentException("Max length must be a positive integer");
    }
    this.maxLength = maxLength;
  }

  void setMax(final double max) {
    this.max = max;
  }

  void setMin(final double min) {
    this.min = min;
  }

  void setUseNumberFormatGrouping(final boolean useGrouping) {
    if (!(format instanceof NumberFormat)) {
      throw new IllegalStateException("Grouping can only be set for number formats");
    }

    ((NumberFormat) format).setGroupingUsed(useGrouping);
  }

  void setPreferredColumnWidth(final int preferredColumnWidth) {
    this.preferredColumnWidth = preferredColumnWidth;
  }

  void setDescription(final String description) {
    this.description = description;
  }

  void setMnemonic(final Character mnemonic) {
    this.mnemonic = mnemonic;
  }

  void setFormat(final Format format) {
    requireNonNull(format, "format");
    if (isNumerical() && !(format instanceof NumberFormat)) {
      throw new IllegalArgumentException("NumberFormat required for numerical property: " + propertyId);
    }
    if (isTemporal()) {
      throw new IllegalArgumentException("Use setDateTimeFormatPattern() for date/time based property: " + propertyId);
    }
    this.format = format;
  }

  void setDateTimeFormatPattern(final String dateTimeFormatPattern) {
    if (!isTemporal()) {
      throw new IllegalArgumentException("dateTimeFormatPattern is only applicable to date/time based property: " + propertyId);
    }
    this.dateTimeFormatter = ofPattern(dateTimeFormatPattern);
    this.dateTimeFormatPattern = dateTimeFormatPattern;
  }

  final void setMaximumFractionDigits(final int maximumFractionDigits) {
    if (!(format instanceof NumberFormat)) {
      throw new IllegalStateException("Maximum fraction digits is only applicable for numerical formats");
    }

    ((NumberFormat) format).setMaximumFractionDigits(maximumFractionDigits);
  }

  private Format initializeDefaultFormat() {
    if (isNumerical()) {
      final NumberFormat numberFormat = Formats.getNonGroupingNumberFormat(isInteger());
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

  static final class BooleanValueConverter<T> implements ValueConverter<Boolean, T> {

    private final T trueValue;
    private final T falseValue;

    BooleanValueConverter(final T trueValue, final T falseValue) {
      this.trueValue = requireNonNull(trueValue);
      this.falseValue = requireNonNull(falseValue);
    }

    @Override
    public Boolean fromColumnValue(final T columnValue) {
      if (Objects.equals(trueValue, columnValue)) {
        return true;
      }
      else if (Objects.equals(falseValue, columnValue)) {
        return false;
      }

      return null;
    }

    @Override
    public T toColumnValue(final Boolean value) {
      if (value == null) {
        return null;
      }

      if ((Boolean) value) {
        return trueValue;
      }

      return falseValue;
    }
  }

  private static final class DefaultValueProvider implements ValueProvider {

    private static final long serialVersionUID = 1;

    @Override
    public Object getValue() {
      return null;
    }
  }

  static class Builder<T extends DefaultProperty> implements Property.Builder<T> {

    protected final T property;

    Builder(final T property) {
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
    public Property.Builder<T> setDefaultValueProvider(final ValueProvider provider) {
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
}