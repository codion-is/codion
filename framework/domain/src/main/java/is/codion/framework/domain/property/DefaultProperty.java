/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Text;
import is.codion.common.Util;
import is.codion.common.formats.Formats;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static is.codion.common.Util.nullOrEmpty;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.requireNonNull;

/**
 * A default Property implementation
 */
abstract class DefaultProperty<T> implements Property<T>, Serializable {

  private static final long serialVersionUID = 1;

  private static final ValueSupplier<Object> DEFAULT_VALUE_SUPPLIER = new NullDefaultValueSupplier();

  /**
   * The attribute this property is based on, should be unique within an Entity.
   * The name of this attribute serves as column name for column properties by default.
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
  private ValueSupplier<T> defaultValueSupplier;

  /**
   * The resource bundle key specifying the caption
   */
  private String captionResourceKey;

  /**
   * The caption from the resource bundle, if any
   */
  private transient String resourceCaption;

  /**
   * True if the value of this property is allowed to be null
   */
  private boolean nullable;

  /**
   * The preferred column width when this property is presented in a table
   */
  private int preferredColumnWidth;

  /**
   * True if this property should be hidden in table views
   */
  private boolean hidden;

  /**
   * The maximum length of the data.
   * Only applicable to string based properties.
   */
  private int maximumLength;

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
   * A locale sensitive numerical date/time pattern
   */
  private LocaleDateTimePattern localeDateTimePattern;

  /**
   * The rounding mode to use when working with decimal numbers
   */
  private RoundingMode decimalRoundingMode;

  /**
   * The date/time format pattern
   */
  private transient String dateTimePattern;

  /**
   * The DateTimeFormatter to use, based on dateTimePattern
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
  }

  @Override
  public final String toString() {
    return getCaption();
  }

  @Override
  public Attribute<T> getAttribute() {
    return attribute;
  }

  @Override
  public final EntityType getEntityType() {
    return attribute.getEntityType();
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
    return !(defaultValueSupplier instanceof NullDefaultValueSupplier);
  }

  @Override
  public final T getDefaultValue() {
    return defaultValueSupplier.get();
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
  public final String getDateTimePattern() {
    if (dateTimePattern == null) {
      if (localeDateTimePattern == null) {
        return getDefaultDateTimePattern();
      }
      dateTimePattern = localeDateTimePattern.getDateTimePattern();
    }

    return dateTimePattern;
  }

  @Override
  public final DateTimeFormatter getDateTimeFormatter() {
    if (dateTimeFormatter == null) {
      final String pattern = getDateTimePattern();
      dateTimeFormatter = pattern == null ? null : ofPattern(pattern);
    }

    return dateTimeFormatter;
  }

  @Override
  public final int getMaximumFractionDigits() {
    if (!(format instanceof NumberFormat)) {
      return -1;
    }

    return ((NumberFormat) format).getMaximumFractionDigits();
  }

  @Override
  public final RoundingMode getDecimalRoundingMode() {
    return decimalRoundingMode;
  }

  @Override
  public final String getCaption() {
    if (attribute.getEntityType().getResourceBundleName() != null) {
      if (resourceCaption == null) {
        final ResourceBundle bundle = ResourceBundle.getBundle(attribute.getEntityType().getResourceBundleName());
        resourceCaption = bundle.containsKey(captionResourceKey) ? bundle.getString(captionResourceKey) : "";
      }

      if (!resourceCaption.isEmpty()) {
        return resourceCaption;
      }
    }

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
      return (T) Util.roundDouble((Double) value, getMaximumFractionDigits(), decimalRoundingMode);
    }
    if (value instanceof BigDecimal) {
      return (T) ((BigDecimal) value).setScale(getMaximumFractionDigits(), decimalRoundingMode).stripTrailingZeros();
    }

    return value;
  }

  @Override
  public final String formatValue(final T value) {
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

  private String getDefaultDateTimePattern() {
    if (attribute.isLocalDate()) {
      return DATE_FORMAT.get();
    }
    else if (attribute.isLocalTime()) {
      return TIME_FORMAT.get();
    }
    else if (attribute.isLocalDateTime()) {
      return DATE_TIME_FORMAT.get();
    }
    else if (attribute.isOffsetDateTime()) {
      return DATE_TIME_FORMAT.get();
    }

    return null;
  }

  static class DefaultValueSupplier<T> implements ValueSupplier<T>, Serializable {

    private static final long serialVersionUID = 1;

    private final T defaultValue;

    DefaultValueSupplier(final T defaultValue) {
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

  abstract static class DefaultPropertyBuilder<T, B extends Property.Builder<T, B>> implements Property.Builder<T, B> {

    private final DefaultProperty<T> property;

    DefaultPropertyBuilder(final DefaultProperty<T> property) {
      this.property = property;
      property.format = initializeDefaultFormat(property.getAttribute());
      property.beanProperty = Text.underscoreToCamelCase(property.attribute.getName());
      property.captionResourceKey = property.attribute.getName();
      property.hidden = property.caption == null && resourceNotFound(property.attribute.getEntityType().getResourceBundleName(), property.captionResourceKey);
      property.nullable = true;
      property.preferredColumnWidth = -1;
      property.maximumLength = -1;
      property.defaultValueSupplier = (ValueSupplier<T>) DEFAULT_VALUE_SUPPLIER;
      property.decimalRoundingMode = DECIMAL_ROUNDING_MODE.get();
    }

    @Override
    public Property<T> get() {
      return property;
    }

    @Override
    public final B captionResourceKey(final String captionResourceKey) {
      if (property.caption != null) {
        throw new IllegalStateException("Caption has already been set for property: " + property.attribute);
      }
      final String resourceBundleName = property.attribute.getEntityType().getResourceBundleName();
      if (resourceBundleName == null) {
        throw new IllegalStateException("No resource bundle specified for entity: " + property.attribute.getEntityType());
      }
      if (resourceNotFound(resourceBundleName, requireNonNull(captionResourceKey, "captionResourceKey"))) {
        throw new IllegalArgumentException("Resource " + captionResourceKey + " not found in bundle: " + resourceBundleName);
      }
      property.captionResourceKey = captionResourceKey;
      property.hidden = false;
      return (B) this;
    }

    @Override
    public final B beanProperty(final String beanProperty) {
      if (nullOrEmpty(beanProperty)) {
        throw new IllegalArgumentException("beanProperty must be a non-empty string: " + property.attribute);
      }
      property.beanProperty = beanProperty;
      return (B) this;
    }

    @Override
    public final B hidden() {
      return hidden(true);
    }

    @Override
    public final B hidden(final boolean hidden) {
      property.hidden = hidden;
      return (B) this;
    }

    @Override
    public final B defaultValue(final T defaultValue) {
      return defaultValueSupplier(new DefaultValueSupplier<>(defaultValue));
    }

    @Override
    public final B defaultValueSupplier(final ValueSupplier<T> supplier) {
      if (supplier != null) {
        property.attribute.validateType(supplier.get());
      }
      property.defaultValueSupplier = supplier == null ? (ValueSupplier<T>) DEFAULT_VALUE_SUPPLIER : supplier;
      return (B) this;
    }

    @Override
    public final B nullable(final boolean nullable) {
      property.nullable = nullable;
      return (B) this;
    }

    @Override
    public final B maximumLength(final int maximumLength) {
      if (!property.attribute.isString()) {
        throw new IllegalStateException("maximumLength is only applicable to string properties: " + property.attribute);
      }
      if (maximumLength <= 0) {
        throw new IllegalArgumentException("maximumLength must be a positive integer: " + property.attribute);
      }
      property.maximumLength = maximumLength;
      return (B) this;
    }

    @Override
    public final B maximumValue(final double maximumValue) {
      if (!property.attribute.isNumerical()) {
        throw new IllegalStateException("maximumValue is only applicable to numerical properties: " + property.attribute);
      }
      if (property.minimumValue != null && property.minimumValue > maximumValue) {
        throw new IllegalArgumentException("maximumValue must be larger than minimum value: " + property.attribute);
      }
      property.maximumValue = maximumValue;
      return (B) this;
    }

    @Override
    public final B minimumValue(final double minimumValue) {
      if (!property.attribute.isNumerical()) {
        throw new IllegalStateException("minimumValue is only applicable to numerical properties");
      }
      if (property.maximumValue != null && property.maximumValue < minimumValue) {
        throw new IllegalArgumentException("minimumValue must be smaller than maximum value: " + property.attribute);
      }
      property.minimumValue = minimumValue;
      return (B) this;
    }

    @Override
    public final B numberFormatGrouping(final boolean numberFormatGrouping) {
      if (!property.attribute.isNumerical()) {
        throw new IllegalStateException("numberFormatGrouping is only applicable to numerical properties: " + property.attribute);
      }
      ((NumberFormat) property.format).setGroupingUsed(numberFormatGrouping);
      return (B) this;
    }

    @Override
    public final B preferredColumnWidth(final int preferredColumnWidth) {
      if (preferredColumnWidth <= 0) {
        throw new IllegalArgumentException("preferredColumnWidth must be larger than 0");
      }
      property.preferredColumnWidth = preferredColumnWidth;
      return (B) this;
    }

    @Override
    public final B description(final String description) {
      property.description = description;
      return (B) this;
    }

    @Override
    public final B mnemonic(final Character mnemonic) {
      property.mnemonic = mnemonic;
      return (B) this;
    }

    @Override
    public final B format(final Format format) {
      requireNonNull(format, "format");
      if (property.attribute.isNumerical() && !(format instanceof NumberFormat)) {
        throw new IllegalArgumentException("NumberFormat required for numerical property: " + property.attribute);
      }
      if (property.attribute.isTemporal()) {
        throw new IllegalStateException("Use dateTimePattern() or localeDateTimePattern() for temporal properties: " + property.attribute);
      }
      property.format = format;
      return (B) this;
    }

    @Override
    public final B dateTimePattern(final String dateTimePattern) {
      requireNonNull(dateTimePattern, "dateTimePattern");
      if (!property.attribute.isTemporal()) {
        throw new IllegalStateException("dateTimePattern is only applicable to temporal properties: " + property.attribute);
      }
      if (property.localeDateTimePattern != null) {
        throw new IllegalStateException("localeDateTimePattern has already been set for property: " + property.attribute);
      }
      property.dateTimePattern = dateTimePattern;
      property.dateTimeFormatter = ofPattern(property.dateTimePattern);
      return (B) this;
    }

    @Override
    public final B localeDateTimePattern(final LocaleDateTimePattern localeDateTimePattern) {
      requireNonNull(localeDateTimePattern, "localeDateTimePattern");
      if (!property.attribute.isTemporal()) {
        throw new IllegalStateException("localeDateTimePattern is only applicable to temporal properties: " + property.attribute);
      }
      if (property.dateTimePattern != null) {
        throw new IllegalStateException("dateTimePattern has already been set for property: " + property.attribute);
      }
      property.localeDateTimePattern = localeDateTimePattern;
      property.dateTimePattern = localeDateTimePattern.getDateTimePattern();
      property.dateTimeFormatter = localeDateTimePattern.getFormatter();

      return (B) this;
    }

    @Override
    public final B maximumFractionDigits(final int maximumFractionDigits) {
      if (!property.attribute.isDecimal()) {
        throw new IllegalStateException("maximumFractionDigits is only applicable to decimal properties: " + property.attribute);
      }
      ((NumberFormat) property.format).setMaximumFractionDigits(maximumFractionDigits);
      return (B) this;
    }

    @Override
    public final B decimalRoundingMode(final RoundingMode decimalRoundingMode) {
      if (!property.attribute.isDecimal()) {
        throw new IllegalStateException("decimalRoundingMode is only applicable to decimal properties: " + property.attribute);
      }
      property.decimalRoundingMode = requireNonNull(decimalRoundingMode, "decimalRoundingMode");
      return (B) this;
    }

    private static boolean resourceNotFound(final String resourceBundleName, final String captionResourceKey) {
      if (resourceBundleName == null) {
        return true;
      }
      try {
        ResourceBundle.getBundle(resourceBundleName).getString(captionResourceKey);

        return false;
      }
      catch (final MissingResourceException e) {
        return true;
      }
    }

    private static Format initializeDefaultFormat(final Attribute<?> attribute) {
      if (attribute.isNumerical()) {
        final NumberFormat numberFormat = getDefaultNumberFormat(attribute);
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
  }

  private static NumberFormat getDefaultNumberFormat(final Attribute<?> attribute) {
    final boolean grouping = Property.NUMBER_FORMAT_GROUPING.get();
    if (attribute.isInteger() || attribute.isLong()) {
      return grouping ? NumberFormat.getIntegerInstance() : Formats.getNonGroupingIntegerFormat();
    }

    return grouping ? NumberFormat.getNumberInstance() : Formats.getNonGroupingNumberFormat();
  }
}