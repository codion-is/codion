/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.Rounder;
import is.codion.common.Text;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Comparator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static is.codion.common.NullOrEmpty.notNull;
import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.requireNonNull;

abstract class AbstractAttributeDefinition<T> implements AttributeDefinition<T>, Serializable {

  private static final long serialVersionUID = 1;

  private static final Comparator<?> LEXICAL_COMPARATOR = Text.spaceAwareCollator();
  private static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = new DefaultComparator();
  private static final Comparator<Object> TO_STRING_COMPARATOR = new ToStringComparator();
  private static final ValueSupplier<Object> DEFAULT_VALUE_SUPPLIER = new NullDefaultValueSupplier();

  /**
   * The attribute this definition is based on, should be unique within an Entity.
   * The name of this attribute serves as column name for column attributes by default.
   */
  private final Attribute<T> attribute;

  /**
   * The caption to use when this attribute is presented
   */
  private final String caption;

  /**
   * The resource bundle key specifying the caption
   */
  private final String captionResourceKey;

  /**
   * The name of a bean property linked to this property, if any
   */
  private final String beanProperty;

  /**
   * The default value supplier for this property
   */
  private final ValueSupplier<T> defaultValueSupplier;

  /**
   * True if the value of this attribute is allowed to be null
   */
  private final boolean nullable;

  /**
   * True if this attribute should be hidden in table views
   */
  private final boolean hidden;

  /**
   * The maximum length of the data.
   * Only applicable to string based attributes.
   */
  private final int maximumLength;

  /**
   * The maximum value for this attribute.
   * Only applicable to numerical attributes
   */
  private final Number maximumValue;

  /**
   * The minimum value for this attribute.
   * Only applicable to numerical attributes
   */
  private final Number minimumValue;

  /**
   * A string describing this attribute
   */
  private final String description;

  /**
   * A mnemonic to use when creating a label for this attribute
   */
  private final Character mnemonic;

  /**
   * The Format used when presenting the value of this propertattribute
   */
  private final Format format;

  /**
   * A locale sensitive numerical date/time pattern
   */
  private final LocaleDateTimePattern localeDateTimePattern;

  /**
   * The rounding mode to use when working with decimal numbers
   */
  private final RoundingMode decimalRoundingMode;

  /**
   * The comparator for this attribute
   */
  private final Comparator<T> comparator;

  /**
   * The caption from the resource bundle, if any
   */
  private transient String resourceCaption;

  /**
   * The date/time format pattern
   */
  private transient String dateTimePattern;

  /**
   * The DateTimeFormatter to use, based on dateTimePattern
   */
  private transient DateTimeFormatter dateTimeFormatter;

  protected AbstractAttributeDefinition(AbstractAttributeDefinitionBuilder<T, ?> builder) {
    requireNonNull(builder, "builder");
    this.attribute = builder.attribute;
    this.caption = builder.caption;
    this.captionResourceKey = builder.captionResourceKey;
    this.beanProperty = builder.beanProperty;
    this.defaultValueSupplier = builder.defaultValueSupplier;
    this.nullable = builder.nullable;
    this.hidden = builder.hidden;
    this.maximumLength = builder.maximumLength;
    this.maximumValue = builder.maximumValue;
    this.minimumValue = builder.minimumValue;
    this.description = builder.description;
    this.mnemonic = builder.mnemonic;
    this.format = builder.format;
    this.localeDateTimePattern = builder.localeDateTimePattern;
    this.decimalRoundingMode = builder.decimalRoundingMode;
    this.comparator = builder.comparator;
    this.dateTimePattern = builder.dateTimePattern;
    this.dateTimeFormatter = builder.dateTimeFormatter;
  }

  @Override
  public final String toString() {
    return caption();
  }

  @Override
  public Attribute<T> attribute() {
    return attribute;
  }

  @Override
  public boolean derived() {
    return false;
  }

  @Override
  public final EntityType entityType() {
    return attribute.entityType();
  }

  @Override
  public final String beanProperty() {
    return beanProperty;
  }

  @Override
  public final boolean hidden() {
    return hidden;
  }

  @Override
  public final boolean hasDefaultValue() {
    return !(defaultValueSupplier instanceof AbstractAttributeDefinition.NullDefaultValueSupplier);
  }

  @Override
  public final T defaultValue() {
    return defaultValueSupplier.get();
  }

  @Override
  public final boolean nullable() {
    return nullable;
  }

  @Override
  public final int maximumLength() {
    return maximumLength;
  }

  @Override
  public final Number maximumValue() {
    return maximumValue;
  }

  @Override
  public final Number minimumValue() {
    return minimumValue;
  }

  @Override
  public final String description() {
    return description;
  }

  @Override
  public final Character mnemonic() {
    return mnemonic;
  }

  @Override
  public final Format format() {
    return format;
  }

  @Override
  public final String dateTimePattern() {
    if (dateTimePattern == null) {
      dateTimePattern = localeDateTimePattern == null ? defaultDateTimePattern() : localeDateTimePattern.dateTimePattern();
    }

    return dateTimePattern;
  }

  @Override
  public final DateTimeFormatter dateTimeFormatter() {
    if (dateTimeFormatter == null) {
      String pattern = dateTimePattern();
      dateTimeFormatter = pattern == null ? null : ofPattern(pattern);
    }

    return dateTimeFormatter;
  }

  @Override
  public final Comparator<T> comparator() {
    return comparator;
  }

  @Override
  public final int maximumFractionDigits() {
    if (!(format instanceof NumberFormat)) {
      return -1;
    }

    return ((NumberFormat) format).getMaximumFractionDigits();
  }

  @Override
  public final RoundingMode decimalRoundingMode() {
    return decimalRoundingMode;
  }

  @Override
  public final String caption() {
    if (attribute.entityType().resourceBundleName() != null) {
      if (resourceCaption == null) {
        ResourceBundle bundle = ResourceBundle.getBundle(attribute.entityType().resourceBundleName());
        resourceCaption = bundle.containsKey(captionResourceKey) ? bundle.getString(captionResourceKey) : "";
      }

      if (!resourceCaption.isEmpty()) {
        return resourceCaption;
      }
    }

    return caption == null ? attribute.name() : caption;
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    AbstractAttributeDefinition<?> that = (AbstractAttributeDefinition<?>) obj;

    return attribute.equals(that.attribute);
  }

  @Override
  public final int hashCode() {
    return attribute.hashCode();
  }

  @Override
  public final T prepareValue(T value) {
    if (value instanceof Double) {
      return (T) Rounder.roundDouble((Double) value, maximumFractionDigits(), decimalRoundingMode);
    }
    if (value instanceof BigDecimal) {
      return (T) ((BigDecimal) value).setScale(maximumFractionDigits(), decimalRoundingMode).stripTrailingZeros();
    }

    return value;
  }

  @Override
  public String toString(T value) {
    if (value == null) {
      return "";
    }
    if (attribute.type().isTemporal()) {
      DateTimeFormatter formatter = dateTimeFormatter();
      if (formatter != null) {
        return formatter.format((TemporalAccessor) value);
      }
    }
    if (format != null) {
      return format.format(value);
    }

    return value.toString();
  }

  private String defaultDateTimePattern() {
    if (attribute.type().isLocalDate()) {
      return DATE_FORMAT.get();
    }
    else if (attribute.type().isLocalTime()) {
      return TIME_FORMAT.get();
    }
    else if (attribute.type().isLocalDateTime()) {
      return DATE_TIME_FORMAT.get();
    }
    else if (attribute.type().isOffsetDateTime()) {
      return DATE_TIME_FORMAT.get();
    }

    return null;
  }

  private static boolean resourceNotFound(String resourceBundleName, String captionResourceKey) {
    if (resourceBundleName == null) {
      return true;
    }
    try {
      return !ResourceBundle.getBundle(resourceBundleName).containsKey(captionResourceKey);
    }
    catch (MissingResourceException e) {
      return true;
    }
  }

  static class DefaultValueSupplier<T> implements ValueSupplier<T>, Serializable {

    private static final long serialVersionUID = 1;

    private final T defaultValue;

    DefaultValueSupplier(T defaultValue) {
      this.defaultValue = defaultValue;
    }

    @Override
    public T get() {
      return defaultValue;
    }
  }

  private static final class NullDefaultValueSupplier extends DefaultValueSupplier<Object> {

    private static final long serialVersionUID = 1;

    private NullDefaultValueSupplier() {
      super(null);
    }
  }

  private static final class DefaultComparator implements Comparator<Comparable<Object>>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public int compare(Comparable<Object> o1, Comparable<Object> o2) {
      return o1.compareTo(o2);
    }
  }

  private static final class ToStringComparator implements Comparator<Object>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public int compare(Object o1, Object o2) {
      return o1.toString().compareTo(o2.toString());
    }
  }

  static final class DefinitionComparator implements Comparator<AttributeDefinition<?>> {

    private final Collator collator = Collator.getInstance();

    @Override
    public int compare(AttributeDefinition<?> definition1, AttributeDefinition<?> definition2) {
      return collator.compare(definition1.toString().toLowerCase(), definition2.toString().toLowerCase());
    }
  }

  abstract static class AbstractAttributeDefinitionBuilder<T, B extends AttributeDefinition.Builder<T, B>> implements AttributeDefinition.Builder<T, B> {

    protected final Attribute<T> attribute;
    private String caption;
    private String beanProperty;
    private ValueSupplier<T> defaultValueSupplier;
    private String captionResourceKey;
    private boolean nullable;
    private boolean hidden;
    private int maximumLength;
    private Number maximumValue;
    private Number minimumValue;
    private String description;
    private Character mnemonic;
    private Format format;
    private LocaleDateTimePattern localeDateTimePattern;
    private RoundingMode decimalRoundingMode;
    private Comparator<T> comparator;
    private String dateTimePattern;
    private DateTimeFormatter dateTimeFormatter;

    AbstractAttributeDefinitionBuilder(Attribute<T> attribute) {
      this.attribute = requireNonNull(attribute);
      format = defaultFormat(attribute);
      comparator = defaultComparator(attribute);
      beanProperty = Text.underscoreToCamelCase(attribute.name());
      captionResourceKey = attribute.name();
      hidden = resourceNotFound(attribute.entityType().resourceBundleName(), captionResourceKey);
      nullable = true;
      maximumLength = -1;
      defaultValueSupplier = (ValueSupplier<T>) DEFAULT_VALUE_SUPPLIER;
      decimalRoundingMode = DECIMAL_ROUNDING_MODE.get();
      minimumValue = defaultMinimumValue();
      maximumValue = defaultMaximumValue();
    }

    @Override
    public final Attribute<T> attribute() {
      return attribute;
    }

    @Override
    public final B caption(String caption) {
      this.caption = caption;
      this.hidden = caption == null;
      return (B) this;
    }

    @Override
    public final B captionResourceKey(String captionResourceKey) {
      if (caption != null) {
        throw new IllegalStateException("Caption has already been set for attribute: " + attribute);
      }
      String resourceBundleName = attribute.entityType().resourceBundleName();
      if (resourceBundleName == null) {
        throw new IllegalStateException("No resource bundle specified for entity: " + attribute.entityType());
      }
      if (resourceNotFound(resourceBundleName, requireNonNull(captionResourceKey, "captionResourceKey"))) {
        throw new IllegalArgumentException("Resource " + captionResourceKey + " not found in bundle: " + resourceBundleName);
      }
      this.captionResourceKey = captionResourceKey;
      this.hidden = false;
      return (B) this;
    }

    @Override
    public final B beanProperty(String beanProperty) {
      if (nullOrEmpty(beanProperty)) {
        throw new IllegalArgumentException("beanProperty must be a non-empty string: " + attribute);
      }
      this.beanProperty = beanProperty;
      return (B) this;
    }

    @Override
    public final B hidden(boolean hidden) {
      this.hidden = hidden;
      return (B) this;
    }

    @Override
    public final B defaultValue(T defaultValue) {
      return defaultValue(new DefaultValueSupplier<>(defaultValue));
    }

    @Override
    public B defaultValue(ValueSupplier<T> supplier) {
      if (supplier != null) {
        attribute.type().validateType(supplier.get());
      }
      this.defaultValueSupplier = supplier == null ? (ValueSupplier<T>) DEFAULT_VALUE_SUPPLIER : supplier;
      return (B) this;
    }

    @Override
    public B nullable(boolean nullable) {
      this.nullable = nullable;
      return (B) this;
    }

    @Override
    public B maximumLength(int maximumLength) {
      if (!attribute.type().isString()) {
        throw new IllegalStateException("maximumLength is only applicable to string attributes: " + attribute);
      }
      if (maximumLength <= 0) {
        throw new IllegalArgumentException("maximumLength must be a positive integer: " + attribute);
      }
      this.maximumLength = maximumLength;
      return (B) this;
    }

    @Override
    public final B minimumValue(Number minimumValue) {
      return valueRange(minimumValue, null);
    }

    @Override
    public final B maximumValue(Number maximumValue) {
      return valueRange(null, maximumValue);
    }

    @Override
    public B valueRange(Number minimumValue, Number maximumValue) {
      if (!attribute.type().isNumerical()) {
        throw new IllegalStateException("valueRange is only applicable to numerical attributes");
      }
      if (maximumValue != null && minimumValue != null && maximumValue.doubleValue() < minimumValue.doubleValue()) {
        throw new IllegalArgumentException("minimum value must be smaller than maximum value: " + attribute);
      }
      this.minimumValue = minimumValue;
      this.maximumValue = maximumValue;
      return (B) this;
    }

    @Override
    public final B numberFormatGrouping(boolean numberFormatGrouping) {
      if (!attribute.type().isNumerical()) {
        throw new IllegalStateException("numberFormatGrouping is only applicable to numerical attributes: " + attribute);
      }
      ((NumberFormat) format).setGroupingUsed(numberFormatGrouping);
      return (B) this;
    }

    @Override
    public final B description(String description) {
      this.description = description;
      return (B) this;
    }

    @Override
    public final B mnemonic(Character mnemonic) {
      this.mnemonic = mnemonic;
      return (B) this;
    }

    @Override
    public final B format(Format format) {
      requireNonNull(format, "format");
      if (attribute.type().isNumerical() && !(format instanceof NumberFormat)) {
        throw new IllegalArgumentException("NumberFormat required for numerical attribute: " + attribute);
      }
      if (attribute.type().isTemporal()) {
        throw new IllegalStateException("Use dateTimePattern() or localeDateTimePattern() for temporal attributes: " + attribute);
      }
      this.format = format;
      return (B) this;
    }

    @Override
    public final B dateTimePattern(String dateTimePattern) {
      requireNonNull(dateTimePattern, "dateTimePattern");
      if (!attribute.type().isTemporal()) {
        throw new IllegalStateException("dateTimePattern is only applicable to temporal attributes: " + attribute);
      }
      if (this.localeDateTimePattern != null) {
        throw new IllegalStateException("localeDateTimePattern has already been set for attribute: " + attribute);
      }
      this.dateTimePattern = dateTimePattern;
      this.dateTimeFormatter = ofPattern(dateTimePattern);
      return (B) this;
    }

    @Override
    public final B localeDateTimePattern(LocaleDateTimePattern localeDateTimePattern) {
      requireNonNull(localeDateTimePattern, "localeDateTimePattern");
      if (!attribute.type().isTemporal()) {
        throw new IllegalStateException("localeDateTimePattern is only applicable to temporal attributes: " + attribute);
      }
      if (this.dateTimePattern != null) {
        throw new IllegalStateException("dateTimePattern has already been set for attribute: " + attribute);
      }
      this.localeDateTimePattern = localeDateTimePattern;
      this.dateTimePattern = localeDateTimePattern.dateTimePattern();
      this.dateTimeFormatter = localeDateTimePattern.createFormatter();

      return (B) this;
    }

    @Override
    public final B maximumFractionDigits(int maximumFractionDigits) {
      if (!attribute.type().isDecimal()) {
        throw new IllegalStateException("maximumFractionDigits is only applicable to decimal attributes: " + attribute);
      }
      ((NumberFormat) format).setMaximumFractionDigits(maximumFractionDigits);
      return (B) this;
    }

    @Override
    public final B decimalRoundingMode(RoundingMode decimalRoundingMode) {
      if (!attribute.type().isDecimal()) {
        throw new IllegalStateException("decimalRoundingMode is only applicable to decimal attributes: " + attribute);
      }
      this.decimalRoundingMode = requireNonNull(decimalRoundingMode, "decimalRoundingMode");
      return (B) this;
    }

    @Override
    public B comparator(Comparator<T> comparator) {
      this.comparator = requireNonNull(comparator);
      return (B) this;
    }

    private static Format defaultFormat(Attribute<?> attribute) {
      if (attribute.type().isNumerical()) {
        NumberFormat numberFormat = defaultNumberFormat(attribute);
        if (attribute.type().isDecimal()) {
          ((DecimalFormat) numberFormat).setParseBigDecimal(attribute.type().isBigDecimal());
          numberFormat.setMaximumFractionDigits(AttributeDefinition.MAXIMUM_FRACTION_DIGITS.get());
        }

        return numberFormat;
      }

      return null;
    }

    private static NumberFormat defaultNumberFormat(Attribute<?> attribute) {
      boolean grouping = NUMBER_FORMAT_GROUPING.get();
      if (attribute.type().isInteger() || attribute.type().isLong()) {
        return setSeparators(grouping ? NumberFormat.getIntegerInstance() : nonGroupingIntegerFormat());
      }

      return setSeparators(grouping ? NumberFormat.getNumberInstance() : nonGroupingNumberFormat());
    }

    private static NumberFormat nonGroupingNumberFormat() {
      NumberFormat format = NumberFormat.getNumberInstance();
      format.setGroupingUsed(false);

      return format;
    }

    private static NumberFormat nonGroupingIntegerFormat() {
      NumberFormat format = NumberFormat.getIntegerInstance();
      format.setGroupingUsed(false);

      return format;
    }

    private static NumberFormat setSeparators(NumberFormat numberFormat) {
      if (numberFormat instanceof DecimalFormat) {
        Character defaultGroupingSeparator = GROUPING_SEPARATOR.get();
        Character defaultDecimalSeparator = DECIMAL_SEPARATOR.get();
        if (notNull(defaultGroupingSeparator, defaultDecimalSeparator)) {
          DecimalFormatSymbols symbols = ((DecimalFormat) numberFormat).getDecimalFormatSymbols();
          symbols.setDecimalSeparator(defaultDecimalSeparator);
          symbols.setGroupingSeparator(defaultGroupingSeparator);
          ((DecimalFormat) numberFormat).setDecimalFormatSymbols(symbols);
        }
      }

      return numberFormat;
    }

    private static <T> Comparator<T> defaultComparator(Attribute<T> attribute) {
      if (attribute.type().isString() && USE_LEXICAL_STRING_COMPARATOR.get()) {
        return (Comparator<T>) LEXICAL_COMPARATOR;
      }
      if (Comparable.class.isAssignableFrom(attribute.type().valueClass())) {
        return (Comparator<T>) COMPARABLE_COMPARATOR;
      }

      return (Comparator<T>) TO_STRING_COMPARATOR;
    }

    private Number defaultMinimumValue() {
      if (attribute.type().isNumerical()) {
        if (attribute.type().isShort()) {
          return Short.MIN_VALUE;
        }
        if (attribute.type().isInteger()) {
          return Integer.MIN_VALUE;
        }
        if (attribute.type().isLong()) {
          return Long.MIN_VALUE;
        }
        if (attribute.type().isDouble()) {
          return -Double.MAX_VALUE;
        }
      }

      return null;
    }

    private Number defaultMaximumValue() {
      if (attribute.type().isNumerical()) {
        if (attribute.type().isShort()) {
          return Short.MAX_VALUE;
        }
        if (attribute.type().isInteger()) {
          return Integer.MAX_VALUE;
        }
        if (attribute.type().isLong()) {
          return Long.MAX_VALUE;
        }
        if (attribute.type().isDouble()) {
          return Double.MAX_VALUE;
        }
      }

      return null;
    }
  }
}