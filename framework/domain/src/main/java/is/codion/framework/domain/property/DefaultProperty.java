/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Formats;
import is.codion.common.Util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.function.Supplier;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.requireNonNull;

/**
 * A default Property implementation
 */
abstract class DefaultProperty<T> implements Property<T> {

  private static final long serialVersionUID = 1;

  private static final Supplier<Object> DEFAULT_VALUE_SUPPLIER = new NullDefaultValueSupplier();

  /**
   * The attribute this property is based on, should be unique within an Entity.
   * By default the name of this attribute serves as column name for column properties.
   * @see #getAttribute()
   */
  private final Attribute<T> attribute;

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
  private Supplier<T> defaultValueSupplier = (Supplier<T>) DEFAULT_VALUE_SUPPLIER;

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
   * @param attribute the attribute
   * @param type the data type of this property
   * @param caption the caption of this property, if this is null then this property is defined as hidden
   * @param typeClass the type associated with this property
   */
  DefaultProperty(final Attribute<T> attribute, final String caption) {
    requireNonNull(attribute, "attribute");
    this.attribute = attribute;
    this.caption = caption;
    this.hidden = caption == null;
    this.format = initializeDefaultFormat();
    this.dateTimeFormatPattern = getDefaultDateTimeFormatPattern();
  }

  @Override
  public final String toString() {
    return getCaption();
  }

  @Override
  public final boolean is(final Attribute<?> attribute) {
    return this.attribute.equals(attribute);
  }

  @Override
  public Attribute<T> getAttribute() {
    return attribute;
  }

  @Override
  public final int getType() {
    return attribute.getType();
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
  public final T getDefaultValue() {
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
    return caption == null ? attribute.getName() : caption;
  }

  @Override
  public final boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DefaultProperty<?> that = (DefaultProperty<?>) obj;

    return attribute.equals(that.attribute);
  }

  @Override
  public final int hashCode() {
    return attribute.hashCode();
  }

  @Override
  public final T prepareValue(final T value) {
    if (value instanceof Double) {
      return (T) Util.roundDouble((Double) value, getMaximumFractionDigits());
    }
    if (value instanceof BigDecimal) {
      return (T) ((BigDecimal) value).setScale(getMaximumFractionDigits(), bigDecimalRoundingMode).stripTrailingZeros();
    }

    return value;
  }

  @Override
  public String formatValue(final T value) {
    if (value == null) {
      return "";
    }
    if (attribute.isTemporal()) {
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
    if (attribute.isNumerical()) {
      final NumberFormat numberFormat = attribute.isInteger() || attribute.isLong() ?
              Formats.getNonGroupingIntegerFormat() : Formats.getNonGroupingNumberFormat();
      if (attribute.isBigDecimal()) {
        ((DecimalFormat) numberFormat).setParseBigDecimal(true);
      }
      if (attribute.isDecimal()) {
        numberFormat.setMaximumFractionDigits(Property.MAXIMUM_FRACTION_DIGITS.get());
      }

      return numberFormat;
    }

    return Formats.NULL_FORMAT;
  }

  private String getDefaultDateTimeFormatPattern() {
    if (attribute.isDate()) {
      return DATE_FORMAT.get();
    }
    else if (attribute.isTime()) {
      return TIME_FORMAT.get();
    }
    else if (attribute.isTimestamp()) {
      return TIMESTAMP_FORMAT.get();
    }

    return null;
  }

  private static class DefaultValueSupplier<T> implements Supplier<T>, Serializable {

    private static final long serialVersionUID = 1;

    private final T defaultValue;

    private DefaultValueSupplier(final T defaultValue) {
      this.defaultValue = defaultValue;
    }

    @Override
    public T get() {
      return defaultValue;
    }
  }

  private static class NullDefaultValueSupplier extends DefaultValueSupplier<Object> {

    private static final long serialVersionUID = 1;

    private NullDefaultValueSupplier() {
      super(null);
    }
  }

  abstract static class DefaultPropertyBuilder<T> implements Property.Builder<T> {

    protected final DefaultProperty<T> property;

    DefaultPropertyBuilder(final DefaultProperty<T> property) {
      this.property = property;
    }

    @Override
    public Property<T> get() {
      return property;
    }

    @Override
    public Property.Builder<T> entityId(final String entityId) {
      ((DefaultAttribute<T>) property.attribute).setEntityId(entityId);
      return this;
    }

    @Override
    public final Property.Builder<T> beanProperty(final String beanProperty) {
      property.beanProperty = requireNonNull(beanProperty, "beanProperty");
      return this;
    }

    @Override
    public final Property.Builder<T> hidden(final boolean hidden) {
      property.hidden = hidden;
      return this;
    }

    @Override
    public final Property.Builder<T> defaultValue(final T defaultValue) {
      return defaultValueSupplier(new DefaultValueSupplier<>(defaultValue));
    }

    @Override
    public Property.Builder<T> defaultValueSupplier(final Supplier<T> supplier) {
      if (supplier != null) {
        property.attribute.validateType(supplier.get());
      }
      property.defaultValueSupplier = supplier == null ? (Supplier<T>) DEFAULT_VALUE_SUPPLIER : supplier;
      return this;
    }

    @Override
    public Property.Builder<T> nullable(final boolean nullable) {
      property.nullable = nullable;
      return this;
    }

    @Override
    public final Property.Builder<T> maximumLength(final int maxLength) {
      if (!property.attribute.isString()) {
        throw new IllegalStateException("maximumLength is only applicable to string properties");
      }
      if (maxLength <= 0) {
        throw new IllegalArgumentException("Maximum length must be a positive integer");
      }
      property.maximumLength = maxLength;
      return this;
    }

    @Override
    public final Property.Builder<T> maximumValue(final double maximumValue) {
      if (!property.attribute.isNumerical()) {
        throw new IllegalStateException("maximumValue is only applicable to numerical properties");
      }
      if (property.minimumValue != null && property.minimumValue > maximumValue) {
        throw new IllegalArgumentException("Maximum value must be larger than minimum value");
      }
      property.maximumValue = maximumValue;
      return this;
    }

    @Override
    public final Property.Builder<T> minimumValue(final double minimumValue) {
      if (!property.attribute.isNumerical()) {
        throw new IllegalStateException("minimumValue is only applicable to numerical properties");
      }
      if (property.maximumValue != null && property.maximumValue < minimumValue) {
        throw new IllegalArgumentException("Minimum value must be smaller than maximum value");
      }
      property.minimumValue = minimumValue;
      return this;
    }

    @Override
    public final Property.Builder<T> numberFormatGrouping(final boolean numberFormatGrouping) {
      if (!property.attribute.isNumerical()) {
        throw new IllegalStateException("numberFormatGrouping is only applicable to numerical properties");
      }
      ((NumberFormat) property.format).setGroupingUsed(numberFormatGrouping);
      return this;
    }

    @Override
    public final Property.Builder<T> preferredColumnWidth(final int preferredColumnWidth) {
      property.preferredColumnWidth = preferredColumnWidth;
      return this;
    }

    @Override
    public final Property.Builder<T> description(final String description) {
      property.description = description;
      return this;
    }

    @Override
    public final Property.Builder<T> mnemonic(final Character mnemonic) {
      property.mnemonic = mnemonic;
      return this;
    }

    @Override
    public final Property.Builder<T> format(final Format format) {
      requireNonNull(format, "format");
      if (property.attribute.isNumerical() && !(format instanceof NumberFormat)) {
        throw new IllegalArgumentException("NumberFormat required for numerical property: " + property.attribute);
      }
      if (property.attribute.isTemporal()) {
        throw new IllegalArgumentException("Use dateTimeFormatPattern() for temporal properties: " + property.attribute);
      }
      property.format = format;
      return this;
    }

    @Override
    public final Property.Builder<T> dateTimeFormatPattern(final String dateTimeFormatPattern) {
      requireNonNull(dateTimeFormatPattern, "dateTimeFormatPattern");
      if (!property.attribute.isTemporal()) {
        throw new IllegalArgumentException("dateTimeFormatPattern is only applicable to temporal properties: " + property.attribute);
      }
      property.dateTimeFormatter = ofPattern(dateTimeFormatPattern);
      property.dateTimeFormatPattern = dateTimeFormatPattern;
      return this;
    }

    @Override
    public final Property.Builder<T> maximumFractionDigits(final int maximumFractionDigits) {
     if (!property.attribute.isDecimal()) {
        throw new IllegalStateException("maximumFractionDigits is only applicable to decimal properties");
      }
      ((NumberFormat) property.format).setMaximumFractionDigits(maximumFractionDigits);
      return this;
    }

    @Override
    public final Property.Builder<T> bigDecimalRoundingMode(final RoundingMode roundingMode) {
      property.bigDecimalRoundingMode = requireNonNull(roundingMode, "roundingMode");
      return this;
    }
  }
}