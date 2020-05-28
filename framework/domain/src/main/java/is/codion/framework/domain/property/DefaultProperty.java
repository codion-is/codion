/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Formats;
import is.codion.common.Util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.function.Supplier;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.requireNonNull;

/**
 * A default Property implementation
 */
abstract class DefaultProperty implements Property {

  private static final long serialVersionUID = 1;

  private static final Supplier<Object> DEFAULT_VALUE_SUPPLIER = new NullDefaultValueSupplier();

  /**
   * The id of the entity this property is associated with
   */
  private String entityId;

  /**
   * The property identifier, should be unique within an Entity.
   * By default this id serves as column name for database properties.
   * @see #getPropertyId()
   */
  private final Attribute<?> propertyId;

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
   * The name of a bean property linked to this property, if any
   */
  private String beanProperty;

  /**
   * The default value supplier for this property
   */
  private Supplier<Object> defaultValueSupplier = DEFAULT_VALUE_SUPPLIER;

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
   * The rounding mode to use when working with BigDecimal
   */
  private RoundingMode bigDecimalRoundingMode = BIG_DECIMAL_ROUNDING_MODE.get();

  /**
   * The DateTimeFormatter to use, based on dateTimeFormatPattern
   */
  private transient DateTimeFormatter dateTimeFormatter;

  /**
   * @param  propertyId the propertyId, this is used as the underlying column name
   * @param type the data type of this property
   * @param caption the caption of this property, if this is null then this property is defined as hidden
   * @param typeClass the type associated with this property
   */
  DefaultProperty(final Attribute<?> propertyId, final int type, final String caption,
                  final Class typeClass) {
    requireNonNull(propertyId, "propertyId");
    this.propertyId = propertyId;
    this.type = type;
    this.caption = caption;
    this.typeClass = typeClass;
    this.hidden = caption == null;
    this.format = initializeDefaultFormat();
    this.dateTimeFormatPattern = getDefaultDateTimeFormatPattern();
  }

  @Override
  public final String toString() {
    return getCaption();
  }

  @Override
  public final boolean is(final Attribute<?> propertyId) {
    return this.propertyId.equals(propertyId);
  }

  @Override
  public final boolean is(final Property property) {
    return is(property.getPropertyId());
  }

  @Override
  public final boolean isNumerical() {
    return isInteger() || isDecimal() || isLong();
  }

  @Override
  public final boolean isTemporal() {
    return isDate() || isTimestamp() || isTime();
  }

  @Override
  public final boolean isDate() {
    return isType(Types.DATE);
  }

  @Override
  public final boolean isTimestamp() {
    return isType(Types.TIMESTAMP);
  }

  @Override
  public final boolean isTime() {
    return isType(Types.TIME);
  }

  @Override
  public final boolean isCharacter() {
    return isType(Types.CHAR);
  }

  @Override
  public final boolean isString() {
    return isType(Types.VARCHAR);
  }

  @Override
  public final boolean isLong() {
    return isType(Types.BIGINT);
  }

  @Override
  public final boolean isInteger() {
    return isType(Types.INTEGER);
  }

  @Override
  public final boolean isDouble() {
    return isType(Types.DOUBLE);
  }

  @Override
  public final boolean isBigDecimal() {
    return isType(Types.DECIMAL);
  }

  @Override
  public final boolean isDecimal() {
    return isDouble() || isBigDecimal();
  }

  @Override
  public final boolean isBoolean() {
    return isType(Types.BOOLEAN);
  }

  @Override
  public final boolean isBlob() {
    return isType(Types.BLOB);
  }

  @Override
  public Attribute<?> getPropertyId() {
    return propertyId;
  }

  @Override
  public final String getEntityId() {
    return entityId;
  }

  @Override
  public final int getType() {
    return type;
  }

  @Override
  public final boolean isType(final int type) {
    return this.type == type;
  }

  @Override
  public final String getBeanProperty() {
    return beanProperty;
  }

  @Override
  public final boolean isHidden() {
    return hidden;
  }

  @Override
  public final boolean hasDefaultValue() {
    return !(this.defaultValueSupplier instanceof NullDefaultValueSupplier);
  }

  @Override
  public final Object getDefaultValue() {
    return this.defaultValueSupplier.get();
  }

  @Override
  public final boolean isNullable() {
    return nullable;
  }

  @Override
  public final int getMaximumLength() {
    return maximumLength;
  }

  @Override
  public final Double getMaximumValue() {
    return maximumValue;
  }

  @Override
  public final Double getMinimumValue() {
    return minimumValue;
  }

  @Override
  public final int getPreferredColumnWidth() {
    return preferredColumnWidth;
  }

  @Override
  public final String getDescription() {
    return description;
  }

  @Override
  public final Character getMnemonic() {
    return mnemonic;
  }

  @Override
  public final Format getFormat() {
    return format;
  }

  @Override
  public final String getDateTimeFormatPattern() {
    return dateTimeFormatPattern;
  }

  @Override
  public final DateTimeFormatter getDateTimeFormatter() {
    if (dateTimeFormatter == null && dateTimeFormatPattern != null) {
      dateTimeFormatter = ofPattern(dateTimeFormatPattern);
    }

    return dateTimeFormatter;
  }

  @Override
  public final int getMaximumFractionDigits() {
    if (!(format instanceof NumberFormat)) {
      throw new IllegalStateException("Maximum fraction digits are only apply to numerical formats");
    }

    return ((NumberFormat) format).getMaximumFractionDigits();
  }

  @Override
  public final RoundingMode getBigDecimalRoundingMode() {
    return bigDecimalRoundingMode;
  }

  @Override
  public final String getCaption() {
    return caption == null ? propertyId.getId() : caption;
  }

  @Override
  public final boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DefaultProperty that = (DefaultProperty) obj;

    return Objects.equals(entityId, that.entityId) && propertyId.equals(that.propertyId);
  }

  @Override
  public final int hashCode() {
    return propertyId.hashCode() + 31 * (entityId == null ? 0 : entityId.hashCode());
  }

  @Override
  public final Class getTypeClass() {
    return typeClass;
  }

  @Override
  public Object validateType(final Object value) {
    if (value != null && typeClass != value.getClass() && !typeClass.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Value of type " + typeClass +
              " expected for property " + this + " in entity " + entityId + ", got: " + value.getClass());
    }

    return value;
  }

  @Override
  public final Object prepareValue(final Object value) {
    if (value instanceof Double) {
      return Util.roundDouble((Double) value, getMaximumFractionDigits());
    }
    if (value instanceof BigDecimal) {
      return ((BigDecimal) value).setScale(getMaximumFractionDigits(), bigDecimalRoundingMode).stripTrailingZeros();
    }

    return value;
  }

  @Override
  public String formatValue(final Object value) {
    if (value == null) {
      return "";
    }
    if (isTemporal()) {
      final DateTimeFormatter formatter = getDateTimeFormatter();
      if (formatter != null) {
        return formatter.format((TemporalAccessor) value);
      }
    }
    if (format != null) {
      return format.format(value);
    }

    return value.toString();
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

  private static class DefaultValueSupplier implements Supplier<Object>, Serializable {

    private static final long serialVersionUID = 1;

    private final Object defaultValue;

    private DefaultValueSupplier(final Object defaultValue) {
      this.defaultValue = defaultValue;
    }

    @Override
    public Object get() {
      return defaultValue;
    }
  }

  private static class NullDefaultValueSupplier extends DefaultValueSupplier {

    private static final long serialVersionUID = 1;

    private NullDefaultValueSupplier() {
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
      return defaultValueSupplier(new DefaultValueSupplier(defaultValue));
    }

    @Override
    public Property.Builder defaultValueSupplier(final Supplier<Object> supplier) {
      if (supplier != null) {
        property.validateType(supplier.get());
      }
      property.defaultValueSupplier = supplier == null ? DEFAULT_VALUE_SUPPLIER : supplier;
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
    public final Property.Builder numberFormatGrouping(final boolean numberFormatGrouping) {
      if (!property.isNumerical()) {
        throw new IllegalStateException("numberFormatGrouping is only applicable to numerical properties");
      }
      ((NumberFormat) property.format).setGroupingUsed(numberFormatGrouping);
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
        throw new IllegalArgumentException("Use dateTimeFormatPattern() for temporal properties: " + property.propertyId);
      }
      property.format = format;
      return this;
    }

    @Override
    public final Property.Builder dateTimeFormatPattern(final String dateTimeFormatPattern) {
      requireNonNull(dateTimeFormatPattern, "dateTimeFormatPattern");
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

    @Override
    public final Property.Builder bigDecimalRoundingMode(final RoundingMode roundingMode) {
      property.bigDecimalRoundingMode = requireNonNull(roundingMode, "roundingMode");
      return this;
    }
  }
}