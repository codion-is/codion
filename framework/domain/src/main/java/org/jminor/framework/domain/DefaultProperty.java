/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Formats;
import org.jminor.common.Item;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.ValueConverter;
import org.jminor.common.db.ValueFetcher;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * A default Property implementation
 */
class DefaultProperty implements Property {

  private static final long serialVersionUID = 1;

  private static final ValueConverter<Object, Object> DEFAULT_VALUE_CONVERTER = new DefaultValueConverter();
  private static final ValueConverter<LocalDate, java.sql.Date> DATE_VALUE_CONVERTER = new DateValueConverter();
  private static final ValueConverter<LocalDateTime, java.sql.Timestamp> TIMESTAMP_VALUE_CONVERTER = new TimestampValueConverter();
  private static final ValueConverter<LocalTime, java.sql.Time> TIME_VALUE_CONVERTER = new TimeValueConverter();
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

  @Override
  public final String toString() {
    return getCaption();
  }

  @Override
  public final boolean is(final String propertyId) {
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
  public final String getPropertyId() {
    return propertyId;
  }

  @Override
  public String getEntityId() {
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
  public boolean isReadOnly() {
    return this.readOnly;
  }

  @Override
  public boolean hasDefaultValue() {
    return !(this.defaultValueProvider instanceof DefaultValueProvider);
  }

  @Override
  public final Object getDefaultValue() {
    return this.defaultValueProvider.getValue();
  }

  @Override
  public boolean isNullable() {
    return nullable;
  }

  @Override
  public final int getMaxLength() {
    return maxLength;
  }

  @Override
  public final Double getMax() {
    return max;
  }

  @Override
  public final Double getMin() {
    return min;
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
      throw new IllegalStateException("Maximum fraction digits is only applicable for numerical formats");
    }

    return ((NumberFormat) format).getMaximumFractionDigits();
  }

  @Override
  public String getCaption() {
    if (caption == null) {
      return propertyId;
    }

    return caption;
  }

  @Override
  public final boolean equals(final Object obj) {
    return this == obj || obj instanceof Property && this.propertyId.equals(((Property) obj).getPropertyId());
  }

  @Override
  public final int hashCode() {
    return hashCode;
  }

  @Override
  public final Class getTypeClass() {
    return typeClass;
  }

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

  static class DefaultColumnProperty extends DefaultProperty implements ColumnProperty {

    private static final long serialVersionUID = 1;

    private final int columnType;
    private int primaryKeyIndex = -1;
    private boolean columnHasDefaultValue = false;
    private boolean updatable = true;
    private ForeignKeyProperty foreignKeyProperty = null;

    private final transient ValueFetcher<Object> valueFetcher;
    private final transient ResultPacker<Object> resultPacker;
    private transient String columnName;
    private transient ValueConverter<Object, Object> valueConverter;
    private transient boolean groupingColumn = false;
    private transient boolean aggregateColumn = false;
    private transient boolean selectable = true;

    DefaultColumnProperty(final String propertyId, final int type, final String caption) {
      this(propertyId, type, caption, type);
    }

    DefaultColumnProperty(final String propertyId, final int type, final String caption, final int columnType) {
      super(propertyId, type, caption, getTypeClass(type));
      this.columnName = propertyId;
      this.columnType = columnType;
      this.valueConverter = initializeValueConverter(this);
      this.valueFetcher = initializeValueFetcher(this);
      this.resultPacker = new PropertyResultPacker();
    }

    @Override
    public final String getColumnName() {
      return columnName;
    }

    @Override
    public final int getColumnType() {
      return columnType;
    }

    @Override
    public final Object toColumnValue(final Object value) {
      return valueConverter.toColumnValue(value);
    }

    @Override
    public final Object fromColumnValue(final Object object) {
      return valueConverter.fromColumnValue(object);
    }

    @Override
    public final boolean columnHasDefaultValue() {
      return columnHasDefaultValue;
    }

    @Override
    public final boolean isUpdatable() {
      return this.updatable;
    }

    @Override
    public boolean isDenormalized() {
      return false;
    }

    @Override
    public final int getPrimaryKeyIndex() {
      return primaryKeyIndex;
    }

    @Override
    public final boolean isGroupingColumn() {
      return groupingColumn;
    }

    @Override
    public final boolean isAggregateColumn() {
      return aggregateColumn;
    }

    @Override
    public final boolean isSelectable() {
      return selectable;
    }

    @Override
    public final ForeignKeyProperty getForeignKeyProperty() {
      return foreignKeyProperty;
    }

    @Override
    public final boolean isForeignKeyProperty() {
      return foreignKeyProperty != null;
    }

    @Override
    public final boolean isPrimaryKeyProperty() {
      return primaryKeyIndex >= 0;
    }

    @Override
    public final boolean isReadOnly() {
      if (foreignKeyProperty != null) {
        return foreignKeyProperty.isReadOnly();
      }

      return super.isReadOnly();
    }

    @Override
    public final String getCaption() {
      final String superCaption = super.getCaption();
      if (superCaption == null && isForeignKeyProperty()) {
        return foreignKeyProperty.getCaption();
      }

      return superCaption;
    }

    @Override
    public final Object fetchValue(final ResultSet resultSet, final int index) throws SQLException {
      return valueFetcher.fetchValue(resultSet, index);
    }

    @Override
    public final ResultPacker<Object> getResultPacker() {
      return resultPacker;
    }

    void setColumnName(final String columnName) {
      this.columnName = requireNonNull(columnName, "columnName");
    }

    void setColumnHasDefaultValue(final boolean columnHasDefaultValue) {
      this.columnHasDefaultValue = columnHasDefaultValue;
    }

    void setUpdatable(final boolean updatable) {
      this.updatable = updatable;
    }

    void  setPrimaryKeyIndex(final int index) {
      if (index < 0) {
        throw new IllegalArgumentException("Primary key index must be at least 0");
      }
      this.primaryKeyIndex = index;
    }

    void setGroupingColumn(final boolean groupingColumn) {
      if (aggregateColumn) {
        throw new IllegalStateException(columnName + " is an aggregate column");
      }
      this.groupingColumn = groupingColumn;
    }

    void setAggregateColumn(final boolean aggregateColumn) {
      if (groupingColumn) {
        throw new IllegalStateException(columnName + " is a grouping column");
      }
      this.aggregateColumn = aggregateColumn;
    }

    void setSelectable(final boolean selectable) {
      this.selectable = selectable;
    }

    void setForeignKeyProperty(final ForeignKeyProperty foreignKeyProperty) {
      this.foreignKeyProperty = foreignKeyProperty;
    }

    void setReadOnly(final boolean readOnly) {
      if (isForeignKeyProperty()) {
        throw new IllegalStateException("Can not set the read only status of a property which is part of a foreign key property");
      }

      super.setReadOnly(readOnly);
    }

    void setValueConverter(final ValueConverter<?, ?> valueConverter) {
      requireNonNull(valueConverter, "valueConverter");
      this.valueConverter = (ValueConverter<Object, Object>) valueConverter;
    }

    private static ValueConverter initializeValueConverter(final ColumnProperty property) {
      if (property.isDate()) {
        return DATE_VALUE_CONVERTER;
      }
      else if (property.isTimestamp()) {
        return TIMESTAMP_VALUE_CONVERTER;
      }
      else if (property.isTime()) {
        return TIME_VALUE_CONVERTER;
      }

      return DEFAULT_VALUE_CONVERTER;
    }

    private static ValueFetcher<Object> initializeValueFetcher(final DefaultColumnProperty property) {
      if (property instanceof MirrorProperty) {
        return null;
      }
      switch (property.columnType) {
        case Types.INTEGER:
          return (resultSet, index) -> property.fromColumnValue(getInteger(resultSet, index));
        case Types.BIGINT:
          return (resultSet, index) -> property.fromColumnValue(getLong(resultSet, index));
        case Types.DOUBLE:
          return (resultSet, index) -> property.fromColumnValue(getDouble(resultSet, index));
        case Types.DECIMAL:
          return (resultSet, index) -> property.fromColumnValue(getBigDecimal(resultSet, index));
        case Types.DATE:
          return (resultSet, index) -> property.fromColumnValue(getDate(resultSet, index));
        case Types.TIMESTAMP:
          return (resultSet, index) -> property.fromColumnValue(getTimestamp(resultSet, index));
        case Types.TIME:
          return (resultSet, index) -> property.fromColumnValue(getTime(resultSet, index));
        case Types.VARCHAR:
          return (resultSet, index) -> property.fromColumnValue(getString(resultSet, index));
        case Types.BOOLEAN:
          return (resultSet, index) -> property.fromColumnValue(getBoolean(resultSet, index));
        case Types.CHAR:
          return (resultSet, index) -> property.fromColumnValue(getCharacter(resultSet, index));
        case Types.BLOB:
          return (resultSet, index) -> null;
        case Types.JAVA_OBJECT:
          return ResultSet::getObject;
        default:
          throw new IllegalArgumentException("Unsupported SQL value type: " + property.columnType);
      }
    }

    private final class PropertyResultPacker implements ResultPacker<Object> {

      private static final int COLUMN_INDEX = 1;

      @Override
      public Object fetch(final ResultSet resultSet) throws SQLException {
        if (isInteger()) {
          return getInteger(resultSet, COLUMN_INDEX);
        }
        else if (isLong()) {
          return getLong(resultSet, COLUMN_INDEX);
        }
        else if (isDouble()) {
          return getDouble(resultSet, COLUMN_INDEX);
        }

        return resultSet.getObject(COLUMN_INDEX);
      }
    }

    private static Boolean getBoolean(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final boolean value = resultSet.getBoolean(columnIndex);

      return !value && resultSet.wasNull() ? null : value;
    }

    private static Integer getInteger(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final int value = resultSet.getInt(columnIndex);

      return value == 0 && resultSet.wasNull() ? null : value;
    }

    private static Long getLong(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final long value = resultSet.getLong(columnIndex);

      return value == 0 && resultSet.wasNull() ? null : value;
    }

    private static Double getDouble(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final double value = resultSet.getDouble(columnIndex);

      return Double.compare(value, 0) == 0 && resultSet.wasNull() ? null : value;
    }

    private static BigDecimal getBigDecimal(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getBigDecimal(columnIndex);
    }

    private static String getString(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getString(columnIndex);
    }

    private static java.util.Date getDate(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getDate(columnIndex);
    }

    private static Timestamp getTimestamp(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getTimestamp(columnIndex);
    }

    private static Time getTime(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getTime(columnIndex);
    }

    private static Character getCharacter(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final String val = getString(resultSet, columnIndex);
      if (!nullOrEmpty(val)) {
        return val.charAt(0);
      }
      else {
        return null;
      }
    }
  }

  static final class DefaultForeignKeyProperty extends DefaultProperty implements Property.ForeignKeyProperty {

    private static final long serialVersionUID = 1;

    private final String foreignEntityId;
    private final List<ColumnProperty> columnProperties;
    private final boolean compositeReference;
    private int fetchDepth = Property.FOREIGN_KEY_FETCH_DEPTH.get();
    private boolean softReference = false;

    transient final List<PropertyDefinition.ColumnPropertyDefinition> columnPropertyDefinitions;

    /**
     * @param propertyId the property ID
     * @param caption the caption
     * @param foreignEntityId the ID of the entity referenced by this foreign key
     * @param columnProperty the underlying column property comprising this foreign key
     */
    DefaultForeignKeyProperty(final String propertyId, final String caption, final String foreignEntityId,
                              final PropertyDefinition.ColumnPropertyDefinition columnProperty) {
      this(propertyId, caption, foreignEntityId, singletonList(columnProperty));
    }

    /**
     * @param propertyId the property ID, note that this is not a column name
     * @param caption the property caption
     * @param foreignEntityId the ID of the entity referenced by this foreign key
     * @param columnPropertyDefinitions the underlying column properties comprising this foreign key
     * @param foreignProperties the properties referenced, in the same order as the column properties,
     * if null then the primary key properties of the referenced entity are used when required
     */
    DefaultForeignKeyProperty(final String propertyId, final String caption, final String foreignEntityId,
                              final List<PropertyDefinition.ColumnPropertyDefinition> columnPropertyDefinitions) {
      super(propertyId, Types.OTHER, caption, Entity.class);
      requireNonNull(foreignEntityId, "foreignEntityId");
      validateParameters(propertyId, foreignEntityId, columnPropertyDefinitions);
      this.columnPropertyDefinitions = columnPropertyDefinitions;
      columnPropertyDefinitions.forEach(columnProperty -> columnProperty.setForeignKeyProperty(this));
      this.foreignEntityId = foreignEntityId;
      this.columnProperties = unmodifiableList(columnPropertyDefinitions.stream().map((Function<PropertyDefinition.ColumnPropertyDefinition,
              ColumnProperty>) PropertyDefinition.ColumnPropertyDefinition::get).collect(toList()));
      this.compositeReference = this.columnProperties.size() > 1;
    }

    @Override
    public boolean isUpdatable() {
      return columnProperties.stream().allMatch(ColumnProperty::isUpdatable);
    }

    @Override
    public String getForeignEntityId() {
      return foreignEntityId;
    }

    @Override
    public List<ColumnProperty> getProperties() {
      return columnProperties;
    }

    @Override
    public boolean isCompositeKey() {
      return compositeReference;
    }

    @Override
    public int getFetchDepth() {
      return fetchDepth;
    }

    @Override
    public boolean isSoftReference() {
      return softReference;
    }

    @Override
    public void validateType(final Object value) {
      super.validateType(value);
      if (value != null) {
        final Entity entity = (Entity) value;
        if (!Objects.equals(foreignEntityId, entity.getEntityId())) {
          throw new IllegalArgumentException("Entity of type " + foreignEntityId +
                  " expected for property " + this + ", got: " + entity.getEntityId());
        }
      }
    }

    void setNullable(final boolean nullable) {
      for (final PropertyDefinition.ColumnPropertyDefinition columnPropertyDefiner : columnPropertyDefinitions) {
        columnPropertyDefiner.setNullable(nullable);
      }

      super.setNullable(nullable);
    }

    void setFetchDepth(final int fetchDepth) {
      this.fetchDepth = fetchDepth;
    }

    void setSoftReference(final boolean softReference) {
      this.softReference = softReference;
    }

    private static void validateParameters(final String propertyId, final String foreignEntityId,
                                           final List<PropertyDefinition.ColumnPropertyDefinition> columnProperties) {
      if (nullOrEmpty(columnProperties)) {
        throw new IllegalArgumentException("No column properties specified");
      }
      for (final PropertyDefinition.ColumnPropertyDefinition columnProperty : columnProperties) {
        requireNonNull(columnProperty, "columnProperty");
        if (columnProperty.get().getPropertyId().equals(propertyId)) {
          throw new IllegalArgumentException(foreignEntityId + ", column propertyId is the same as foreign key propertyId: " + propertyId);
        }
      }
    }
  }

  static final class DefaultMirrorProperty extends DefaultColumnProperty implements MirrorProperty {

    private static final long serialVersionUID = 1;

    DefaultMirrorProperty(final String propertyId) {
      super(propertyId, -1, null);
      super.setReadOnly(true);
    }
  }

  /**
   * A property representing a column that should get its value automatically from a column in a referenced table
   */
  static final class DefaultDenormalizedProperty extends DefaultColumnProperty implements DenormalizedProperty {

    private static final long serialVersionUID = 1;

    private final String foreignKeyPropertyId;
    private final Property denormalizedProperty;

    /**
     * @param propertyId the property ID
     * @param foreignKeyPropertyId the ID of the foreign key property which references the entity which owns
     * the denormalized property
     * @param denormalizedProperty the property from which this property should get its value
     * @param caption the caption if this property
     */
    DefaultDenormalizedProperty(final String propertyId, final String foreignKeyPropertyId,
                                final Property denormalizedProperty, final String caption) {
      super(propertyId, denormalizedProperty.getType(), caption);
      this.foreignKeyPropertyId = foreignKeyPropertyId;
      this.denormalizedProperty = denormalizedProperty;
    }

    @Override
    public String getForeignKeyPropertyId() {
      return foreignKeyPropertyId;
    }

    @Override
    public Property getDenormalizedProperty() {
      return denormalizedProperty;
    }

    @Override
    public boolean isDenormalized() {
      return true;
    }
  }

  static final class DefaultValueListProperty extends DefaultColumnProperty implements ValueListProperty {

    private static final long serialVersionUID = 1;

    private final List<Item> items;

    /**
     * @param propertyId the property ID
     * @param type the data type of this property
     * @param caption the property caption
     * @param items the allowed values for this property
     */
    DefaultValueListProperty(final String propertyId, final int type, final String caption, final List<Item> items) {
      super(propertyId, type, caption);
      this.items = unmodifiableList(items);
    }

    @Override
    public boolean isValid(final Object value) {
      return findItem(value) != null;
    }

    @Override
    public List<Item> getValues() {
      return items;
    }

    @Override
    public String getCaption(final Object value) {
      final Item item = findItem(value);

      return item == null ? "" : item.getCaption();
    }

    private Item findItem(final Object value) {
      for (int i = 0; i < items.size(); i++) {
        final Item item = items.get(i);
        if (Objects.equals(item.getValue(), value)) {
          return item;
        }
      }

      return null;
    }
  }

  static class DefaultTransientProperty extends DefaultProperty implements TransientProperty {

    private static final long serialVersionUID = 1;

    private boolean modifiesEntity = true;

    /**
     * @param propertyId the property ID, since TransientProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     * @param caption the caption of this property
     */
    DefaultTransientProperty(final String propertyId, final int type, final String caption) {
      super(propertyId, type, caption, getTypeClass(type));
    }

    void setModifiesEntity(final boolean modifiesEntity) {
      this.modifiesEntity = modifiesEntity;
    }

    @Override
    public final boolean isModifiesEntity() {
      return modifiesEntity;
    }
  }

  static final class DefaultDerivedProperty extends DefaultTransientProperty implements DerivedProperty {

    private static final long serialVersionUID = 1;

    private final Provider valueProvider;
    private final List<String> sourcePropertyIds;

    DefaultDerivedProperty(final String propertyId, final int type, final String caption,
                           final Provider valueProvider, final String... sourcePropertyIds) {
      super(propertyId, type, caption);
      this.valueProvider = valueProvider;
      if (nullOrEmpty(sourcePropertyIds)) {
        throw new IllegalArgumentException("No source propertyIds, a derived property must be derived from one or more existing properties");
      }
      this.sourcePropertyIds = asList(sourcePropertyIds);
      super.setReadOnly(true);
    }

    @Override
    public Provider getValueProvider() {
      return valueProvider;
    }

    @Override
    public List<String> getSourcePropertyIds() {
      return sourcePropertyIds;
    }

    void setReadOnly(final boolean readOnly) {
      throw new UnsupportedOperationException("Derived properties are always read only");
    }
  }

  static final class DefaultSubqueryProperty extends DefaultColumnProperty implements SubqueryProperty {

    private static final long serialVersionUID = 1;

    private final transient String subquery;

    /**
     * @param propertyId the property ID, since SubqueryProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     * @param caption the caption of this property
     * @param subquery the sql query
     * @param columnType the actual column type
     */
    DefaultSubqueryProperty(final String propertyId, final int type, final String caption, final String subquery,
                            final int columnType) {
      super(propertyId, type, caption, columnType);
      super.setReadOnly(true);
      super.setUpdatable(false);
      this.subquery = subquery;
    }

    @Override
    public String getSubQuery() {
      return subquery;
    }

    void setReadOnly(final boolean readOnly) {
      throw new UnsupportedOperationException("Subquery properties are always read only");
    }
  }

  static class DefaultAuditProperty extends DefaultColumnProperty implements AuditProperty {

    private static final long serialVersionUID = 1;

    private final AuditAction auditAction;

    DefaultAuditProperty(final String propertyId, final int type, final AuditAction auditAction, final String caption) {
      super(propertyId, type, caption);
      this.auditAction = auditAction;
      super.setReadOnly(true);
    }

    @Override
    public final AuditAction getAuditAction() {
      return auditAction;
    }
  }

  static final class DefaultAuditTimeProperty extends DefaultAuditProperty implements AuditTimeProperty {

    private static final long serialVersionUID = 1;

    DefaultAuditTimeProperty(final String propertyId, final AuditAction auditAction, final String caption) {
      super(propertyId, Types.TIMESTAMP, auditAction, caption);
    }
  }

  static final class DefaultAuditUserProperty extends DefaultAuditProperty implements AuditUserProperty {

    private static final long serialVersionUID = 1;

    DefaultAuditUserProperty(final String propertyId, final AuditAction auditAction, final String caption) {
      super(propertyId, Types.VARCHAR, auditAction, caption);
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

  private static final class DefaultValueConverter implements ValueConverter<Object, Object> {
    @Override
    public Object toColumnValue(final Object value) {
      return value;
    }

    @Override
    public Object fromColumnValue(final Object columnValue) {
      return columnValue;
    }
  }

  private static final class DateValueConverter implements ValueConverter<LocalDate, java.sql.Date> {
    @Override
    public java.sql.Date toColumnValue(final LocalDate value) {
      if (value == null) {
        return null;
      }

      return java.sql.Date.valueOf(value);
    }

    @Override
    public LocalDate fromColumnValue(final java.sql.Date columnValue) {
      if (columnValue == null) {
        return null;
      }

      return columnValue.toLocalDate();
    }
  }

  private static final class TimestampValueConverter implements ValueConverter<LocalDateTime, java.sql.Timestamp> {
    @Override
    public java.sql.Timestamp toColumnValue(final LocalDateTime value) {
      if (value == null) {
        return null;
      }

      return java.sql.Timestamp.valueOf(value);
    }

    @Override
    public LocalDateTime fromColumnValue(final java.sql.Timestamp columnValue) {
      if (columnValue == null) {
        return null;
      }

      return columnValue.toLocalDateTime();
    }
  }

  private static final class TimeValueConverter implements ValueConverter<LocalTime, java.sql.Time> {
    @Override
    public java.sql.Time toColumnValue(final LocalTime value) {
      if (value == null) {
        return null;
      }

      return java.sql.Time.valueOf(value);
    }

    @Override
    public LocalTime fromColumnValue(final java.sql.Time columnValue) {
      if (columnValue == null) {
        return null;
      }

      return columnValue.toLocalTime();
    }
  }
}