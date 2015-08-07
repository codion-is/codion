/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.ValueConverter;
import org.jminor.common.db.ValueFetcher;
import org.jminor.common.model.Item;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A default Property implementation
 */
class DefaultProperty implements Property {

  private static final ValueConverter DEFAULT_VALUE_CONVERTER = new DefaultValueConverter();
  private static final ValueConverter DATE_VALUE_CONVERTER = new DateValueConverter();

  /**
   * The ID of the entity this property is associated with
   */
  private String entityID;

  /**
   * The property identifier, should be unique within an Entity.
   * By default this ID serves as column name for database properties.
   * @see #getPropertyID()
   */
  private final String propertyID;

  /**
   * The property type, java.sql.Types
   */
  private final int type;

  /**
   * The class representing the values associated with this property
   */
  private final Class<?> typeClass;

  /**
   * The caption to use when this property is presented
   */
  private final String caption;

  /**
   * This is based on an immutable field, so cache it
   */
  private final int hashCode;

  /**
   * Very frequently used, so cache it
   */
  private final boolean isDouble;

  /**
   * The default value for this property
   */
  private Object defaultValue;

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
   * @param propertyID the property ID, this is used as the underlying column name
   * @param type the data type of this property
   * @param caption the caption of this property, if this is null then this property is defined as hidden
   */
  DefaultProperty(final String propertyID, final int type, final String caption) {
    Util.rejectNullValue(propertyID, "propertyID");
    this.propertyID = propertyID;
    this.hashCode = propertyID.hashCode();
    this.type = type;
    this.caption = caption;
    this.typeClass = getTypeClass(type);
    this.isDouble = isType(Types.DOUBLE);
    setHidden(caption == null);
    setFormat(initializeDefaultFormat());
  }

  /**
   * @return a String representation of this property
   */
  @Override
  public final String toString() {
    return getCaption();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean is(final String propertyID) {
    return this.propertyID.equals(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean is(final Property property) {
    return is(property.getPropertyID());
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNumerical() {
    return isInteger() || isDouble() || isLong();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDateOrTime() {
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
    return isDouble;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isBoolean() {
    return isType(Types.BOOLEAN);
  }

  /** {@inheritDoc} */
  @Override
  public final String getPropertyID() {
    return propertyID;
  }

  /** {@inheritDoc} */
  @Override
  public String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public Property setEntityID(final String entityID) {
    if (this.entityID != null) {
      throw new IllegalStateException("entityID (" + this.entityID + ") has already been set for property: " + propertyID);
    }
    this.entityID = entityID;
    return this;
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
  public final Property setHidden(final boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isHidden() {
    return hidden;
  }

  /** {@inheritDoc} */
  @Override
  public Property setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isReadOnly() {
    return this.readOnly;
  }

  /** {@inheritDoc} */
  @Override
  public final Property setDefaultValue(final Object defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Object getDefaultValue() {
    return this.defaultValue;
  }

  /** {@inheritDoc} */
  @Override
  public Property setNullable(final boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isNullable() {
    return nullable;
  }

  /** {@inheritDoc} */
  @Override
  public final Property setMaxLength(final int maxLength) {
    this.maxLength = maxLength;
    return this;
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
  public final Property setMax(final double max) {
    this.max = max;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Double getMin() {
    return min;
  }

  /** {@inheritDoc} */
  @Override
  public final Property setMin(final double min) {
    this.min = min;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property setUseNumberFormatGrouping(final boolean useGrouping) {
    if (!(format instanceof NumberFormat)) {
      throw new IllegalStateException("Grouping can only be set for number formats");
    }

    ((NumberFormat) format).setGroupingUsed(useGrouping);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property setPreferredColumnWidth(final int preferredColumnWidth) {
    this.preferredColumnWidth = preferredColumnWidth;
    return this;
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
  public final Property setDescription(final String description) {
    this.description = description;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Character getMnemonic() {
    return mnemonic;
  }

  /** {@inheritDoc} */
  @Override
  public final Property setMnemonic(final Character mnemonic) {
    this.mnemonic = mnemonic;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Format getFormat() {
    return format;
  }

  /** {@inheritDoc} */
  @Override
  public final Property setFormat(final Format format) {
    Util.rejectNullValue(format, "format");
    if (isNumerical() && !(format instanceof NumberFormat)) {
      throw new IllegalArgumentException("NumberFormat expected for numerical property: " + propertyID);
    }
    if (isDateOrTime() && !(format instanceof DateFormat)) {
      throw new IllegalArgumentException("DateFormat expected for time based property: " + propertyID);
    }
    this.format = format;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Property setMaximumFractionDigits(final int maximumFractionDigits) {
    if (!(format instanceof NumberFormat)) {
      throw new IllegalStateException("Maximum fraction digits is only applicable for numerical formats");
    }

    ((NumberFormat) format).setMaximumFractionDigits(maximumFractionDigits);
    return this;
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
      return propertyID;
    }

    return caption;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean equals(final Object obj) {
    return this == obj || obj instanceof Property && this.propertyID.equals(((Property) obj).getPropertyID());
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return hashCode;
  }

  /** {@inheritDoc} */
  @Override
  public final Class<?> getTypeClass() {
    return typeClass;
  }

  /**
   * Prepares the value according to the property configuration, such as rounding
   * to the correct number of fraction digits in case of doubles
   * @param value the value to prepare
   * @return the prepared value
   */
  Object prepareValue(final Object value) {
    if (value != null && isDouble()) {
      return Util.roundDouble((Double) value, getMaximumFractionDigits());
    }

    return value;
  }

  private Format initializeDefaultFormat() {
    if (isDateOrTime()) {
      if (isDate()) {
        return Configuration.getDefaultDateFormat();
      }
      else if (isTime()) {
        return Configuration.getDefaultTimeFormat();
      }
      else {
        return Configuration.getDefaultTimestampFormat();
      }
    }
    else if (isNumerical()) {
      final NumberFormat numberFormat = Util.getNonGroupingNumberFormat(isInteger());
      if (isDouble()) {
        numberFormat.setMaximumFractionDigits(Configuration.getIntValue(Configuration.MAXIMUM_FRACTION_DIGITS));
      }

      return numberFormat;
    }

    return Util.NULL_FORMAT;
  }

  /**
   * @param sqlType the type
   * @return the Class representing the given type
   */
  private static Class<?> getTypeClass(final int sqlType) {
    switch (sqlType) {
      case Types.BIGINT:
        return Long.class;
      case Types.INTEGER:
        return Integer.class;
      case Types.DOUBLE:
        return Double.class;
      case Types.DATE:
        return Date.class;
      case Types.TIMESTAMP:
        return Timestamp.class;
      case Types.TIME:
        return Time.class;
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

    private final int columnType;
    private final ValueFetcher valueFetcher;
    private String columnName;
    private ValueConverter valueConverter;
    private int selectIndex = -1;
    private int primaryKeyIndex = -1;
    private boolean columnHasDefaultValue = false;
    private boolean updatable = true;
    private boolean searchable = true;
    private boolean groupingColumn = false;
    private boolean aggregateColumn = false;
    private ForeignKeyProperty foreignKeyProperty = null;

    DefaultColumnProperty(final String propertyID, final int type, final String caption) {
      this(propertyID, type, caption, type);
    }

    DefaultColumnProperty(final String propertyID, final int type, final String caption, final int columnType) {
      super(propertyID, type, caption);
      this.columnName = propertyID;
      this.columnType = columnType;
      this.valueConverter = initializeValueConverter(this);
      this.valueFetcher = initializeValueFetcher(this);
    }

    @Override
    public ColumnProperty setColumnName(final String columnName) {
      this.columnName = Util.rejectNullValue(columnName, "columnName");
      return this;
    }

    @Override
    public final String getColumnName() {
      return this.columnName;
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
    public final ColumnProperty setColumnHasDefaultValue(final boolean columnHasDefaultValue) {
      this.columnHasDefaultValue = columnHasDefaultValue;
      return this;
    }

    @Override
    public final boolean isUpdatable() {
      return this.updatable;
    }

    @Override
    public final ColumnProperty setUpdatable(final boolean updatable) {
      this.updatable = updatable;
      return this;
    }

    @Override
    public final ColumnProperty setSearchable(final boolean searchable) {
      this.searchable = searchable;
      return this;
    }

    @Override
    public final boolean isSearchable() {
      return searchable;
    }

    @Override
    public boolean isDenormalized() {
      return false;
    }

    @Override
    public final void setSelectIndex(final int selectIndex) {
      this.selectIndex = selectIndex;
    }

    @Override
    public final int getSelectIndex() {
      return selectIndex;
    }

    @Override
    public final ColumnProperty setPrimaryKeyIndex(final int index) {
      if (index < 0) {
        throw new IllegalArgumentException("Primary key index must be at least 0");
      }
      this.primaryKeyIndex = index;
      setUpdatable(false);
      return this;
    }

    @Override
    public final int getPrimaryKeyIndex() {
      return primaryKeyIndex;
    }

    @Override
    public final ColumnProperty setGroupingColumn(final boolean groupingColumn) {
      if (aggregateColumn) {
        throw new IllegalStateException(columnName + " is an aggregate column");
      }
      this.groupingColumn = groupingColumn;
      return this;
    }

    @Override
    public final boolean isGroupingColumn() {
      return groupingColumn;
    }

    @Override
    public final ColumnProperty setAggregateColumn(final boolean aggregateColumn) {
      if (groupingColumn) {
        throw new IllegalStateException(columnName + " is a grouping column");
      }
      this.aggregateColumn = aggregateColumn;
      return this;
    }

    @Override
    public final boolean isAggregateColumn() {
      return aggregateColumn;
    }

    @Override
    public final void setForeignKeyProperty(final ForeignKeyProperty foreignKeyProperty) {
      this.foreignKeyProperty = foreignKeyProperty;
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
    public final Property setReadOnly(final boolean readOnly) {
      if (isForeignKeyProperty()) {
        throw new IllegalStateException("Can not set the read only status of a property which is part of a foreign key property");
      }

      return super.setReadOnly(readOnly);
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
    public final Object fetchValue(final ResultSet resultSet) throws SQLException {
      return valueFetcher.fetchValue(resultSet);
    }

    @Override
    public final ColumnProperty setValueConverter(final ValueConverter valueConverter) {
      Util.rejectNullValue(valueConverter, "valueConverter");
      this.valueConverter = valueConverter;
      return this;
    }

    private static ValueConverter initializeValueConverter(final ColumnProperty property) {
      if (property.isDate()) {
        return DATE_VALUE_CONVERTER;
      }

      return DEFAULT_VALUE_CONVERTER;
    }

    private static ValueFetcher initializeValueFetcher(final DefaultColumnProperty property) {
      if (property instanceof MirrorProperty) {
        return null;
      }
      switch (property.columnType) {
        case Types.INTEGER:
          return new ValueFetcher() {
            @Override
            public Object fetchValue(final ResultSet resultSet) throws SQLException {
              return property.fromColumnValue(getInteger(resultSet, property.selectIndex));
            }
          };
        case Types.BIGINT:
          return new ValueFetcher() {
            @Override
            public Object fetchValue(final ResultSet resultSet) throws SQLException {
              return property.fromColumnValue(getLong(resultSet, property.selectIndex));
            }
          };
        case Types.DOUBLE:
          return new ValueFetcher() {
            @Override
            public Object fetchValue(final ResultSet resultSet) throws SQLException {
              return property.fromColumnValue(getDouble(resultSet, property.selectIndex));
            }
          };
        case Types.DATE:
          return new ValueFetcher() {
            @Override
            public Object fetchValue(final ResultSet resultSet) throws SQLException {
              return property.fromColumnValue(getDate(resultSet, property.selectIndex));
            }
          };
        case Types.TIMESTAMP:
          return new ValueFetcher() {
            @Override
            public Object fetchValue(final ResultSet resultSet) throws SQLException {
              return property.fromColumnValue(getTimestamp(resultSet, property.selectIndex));
            }
          };
        case Types.TIME:
          return new ValueFetcher() {
            @Override
            public Object fetchValue(final ResultSet resultSet) throws SQLException {
              return property.fromColumnValue(getTime(resultSet, property.selectIndex));
            }
          };
        case Types.VARCHAR:
          return new ValueFetcher() {
            @Override
            public Object fetchValue(final ResultSet resultSet) throws SQLException {
              return property.fromColumnValue(getString(resultSet, property.selectIndex));
            }
          };
        case Types.BOOLEAN:
          return new ValueFetcher() {
            @Override
            public Object fetchValue(final ResultSet resultSet) throws SQLException {
              return property.fromColumnValue(getBoolean(resultSet, property.selectIndex));
            }
          };
        case Types.CHAR:
          return new ValueFetcher() {
            @Override
            public Object fetchValue(final ResultSet resultSet) throws SQLException {
              return property.fromColumnValue(getCharacter(resultSet, property.selectIndex));
            }
          };
        case Types.BLOB:
          return new ValueFetcher() {
            @Override
            public Object fetchValue(final ResultSet resultSet) {
              return null;//blob columns are handled specifically
            }
          };
        default:
          throw new IllegalArgumentException("Unsupported SQL value type: " + property.columnType);
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

      return value == 0 && resultSet.wasNull() ? null : value;
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
      if (!Util.nullOrEmpty(val)) {
        return val.charAt(0);
      }
      else {
        return null;
      }
    }
  }

  static final class DefaultForeignKeyProperty extends DefaultProperty implements Property.ForeignKeyProperty {

    private final String referencedEntityID;
    private final List<ColumnProperty> referenceProperties;
    private final boolean compositeReference;
    private Map<Property, String> linkedReferenceProperties;
    private int fetchDepth = Configuration.getIntValue(Configuration.FOREIGN_KEY_FETCH_DEPTH);

    /**
     * @param propertyID the property ID, since ForeignKeyProperties are meta properties, the property ID should not
     * be na underlying table column, it must only be unique for the entity
     * @param caption the property caption
     * @param referencedEntityID the ID of the referenced entity type
     * @param referenceProperty the actual column property involved in the reference
     */
    DefaultForeignKeyProperty(final String propertyID, final String caption, final String referencedEntityID,
                              final ColumnProperty referenceProperty) {
      this(propertyID, caption, referencedEntityID, new ColumnProperty[] {referenceProperty}, new String[0]);
    }

    /**
     * @param propertyID the property ID, since ForeignKeyProperties are wrapper properties, the property ID should not
     * be an underlying table column, it must only be unique for the entity
     * @param caption the property caption
     * @param referencedEntityID the ID of the referenced entity type
     * @param referenceProperties the actual column properties involved in the reference
     * @param referencedPropertyIDs the IDs of the properties referenced, in the same order as the reference properties
     */
    DefaultForeignKeyProperty(final String propertyID, final String caption, final String referencedEntityID,
                              final ColumnProperty[] referenceProperties, final String[] referencedPropertyIDs) {
      super(propertyID, Types.REF, caption);
      Util.rejectNullValue(referencedEntityID, "referencedEntityID");
      validateParameters(propertyID, referencedEntityID, referenceProperties, referencedPropertyIDs);
      for (int i = 0; i < referenceProperties.length; i++) {
        final ColumnProperty referenceProperty = referenceProperties[i];
        referenceProperty.setForeignKeyProperty(this);
        if (referencedPropertyIDs.length > i) {
          link(referenceProperty, referencedPropertyIDs[i]);
        }
      }
      this.referencedEntityID = referencedEntityID;
      this.referenceProperties = Collections.unmodifiableList(Arrays.asList(referenceProperties));
      this.compositeReference = this.referenceProperties.size() > 1;
    }

    @Override
    public boolean isUpdatable() {
      for (final ColumnProperty referenceProperty : referenceProperties) {
        if (!referenceProperty.isUpdatable()) {
          return false;
        }
      }

      return true;
    }

    @Override
    public ForeignKeyProperty setNullable(final boolean nullable) {
      for (final ColumnProperty columnProperty : referenceProperties) {
        columnProperty.setNullable(nullable);
      }

      return (ForeignKeyProperty) super.setNullable(nullable);
    }

    @Override
    public String getReferencedEntityID() {
      return referencedEntityID;
    }

    @Override
    public List<ColumnProperty> getReferenceProperties() {
      return referenceProperties;
    }

    @Override
    public boolean isCompositeReference() {
      return compositeReference;
    }

    @Override
    public int getFetchDepth() {
      return fetchDepth;
    }

    @Override
    public ForeignKeyProperty setFetchDepth(final int fetchDepth) {
      this.fetchDepth = fetchDepth;
      return this;
    }

    @Override
    public String getReferencedPropertyID(final Property referenceProperty) {
      if (linkedReferenceProperties == null) {
        return null;
      }

      if (!linkedReferenceProperties.containsKey(referenceProperty)) {
        throw new IllegalArgumentException("No referenced property ID associated with reference property: " + referenceProperty);
      }

      return linkedReferenceProperties.get(referenceProperty);
    }

    private void link(final Property referenceProperty, final String referencedPropertyID) {
      if (linkedReferenceProperties == null) {
        linkedReferenceProperties = new HashMap<>();
      }
      linkedReferenceProperties.put(referenceProperty, referencedPropertyID);
    }

    private static void validateParameters(final String propertyID, final String referencedEntityID,
                                           final ColumnProperty[] referenceProperties, final String[] referencedPropertyIDs) {
      if (referenceProperties == null || referenceProperties.length == 0) {
        throw new IllegalArgumentException("No reference properties specified");
      }
      for (final Property referenceProperty : referenceProperties) {
        Util.rejectNullValue(referenceProperty, "referenceProperty");
        if (referenceProperty.getPropertyID().equals(propertyID)) {
          throw new IllegalArgumentException(referencedEntityID + ", reference propertyID is the same as parent propertyID: " + propertyID);
        }
      }
      if (referenceProperties.length > 1 && referencedPropertyIDs == null) {
        throw new IllegalArgumentException("Referenced properties must be specified when referencing a composite key");
      }
      if (referenceProperties.length > 1 && referencedPropertyIDs.length != referenceProperties.length) {
        throw new IllegalArgumentException("The number of referenced properties must be equal to the number of properties referencing them");
      }
    }
  }

  static final class DefaultMirrorProperty extends DefaultColumnProperty implements MirrorProperty {

    DefaultMirrorProperty(final String propertyID) {
      super(propertyID, -1, null);
      setReadOnly(true);
    }
  }

  /**
   * A property representing a column that should get its value automatically from a column in a referenced table
   */
  static final class DefaultDenormalizedProperty extends DefaultColumnProperty implements DenormalizedProperty {

    private final String foreignKeyPropertyID;
    private final Property denormalizedProperty;

    /**
     * @param propertyID the property ID
     * @param foreignKeyPropertyID the ID of the foreign key property which references the entity which owns
     * the denormalized property
     * @param denormalizedProperty the property from which this property should get its value
     * @param caption the caption if this property
     */
    DefaultDenormalizedProperty(final String propertyID, final String foreignKeyPropertyID,
                                final Property denormalizedProperty, final String caption) {
      super(propertyID, denormalizedProperty.getType(), caption);
      this.foreignKeyPropertyID = foreignKeyPropertyID;
      this.denormalizedProperty = denormalizedProperty;
    }

    @Override
    public String getForeignKeyPropertyID() {
      return foreignKeyPropertyID;
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

    private final List<Item> values;

    /**
     * @param propertyID the property ID
     * @param type the data type of this property
     * @param caption the property caption
     * @param values the values to base this property on
     */
    DefaultValueListProperty(final String propertyID, final int type, final String caption,
                             final List<Item> values) {
      super(propertyID, type, caption);
      this.values = Collections.unmodifiableList(values);
    }

    @Override
    public boolean isValid(final Object value) {
      return values.contains(new Item<>(value, ""));
    }

    @Override
    public List<Item> getValues() {
      return values;
    }

    @Override
    public String getCaption(final Object value) {
      final Item<Object> item = new Item<>(value, "");
      final int index = values.indexOf(item);
      if (index >= 0) {
        return values.get(index).getCaption();
      }

      return "";
    }
  }

  static class DefautTransientProperty extends DefaultProperty implements TransientProperty {

    /**
     * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     * @param caption the caption of this property
     */
    DefautTransientProperty(final String propertyID, final int type, final String caption) {
      super(propertyID, type, caption);
    }
  }

  static final class DefaultDerivedProperty extends DefautTransientProperty implements DerivedProperty {

    private final Provider valueProvider;
    private final List<String> linkedPropertyIDs;

    DefaultDerivedProperty(final String propertyID, final int type, final String caption,
                           final Provider valueProvider, final String... linkedPropertyIDs) {
      super(propertyID, type, caption);
      this.valueProvider = valueProvider;
      if (linkedPropertyIDs == null || linkedPropertyIDs.length == 0) {
        throw new IllegalArgumentException("No linked propertyIDs, a derived property must be derived from one or more existing properties");
      }
      else {
        this.linkedPropertyIDs = Arrays.asList(linkedPropertyIDs);
      }
      setReadOnly(true);
    }

    @Override
    public Provider getValueProvider() {
      return valueProvider;
    }

    @Override
    public List<String> getLinkedPropertyIDs() {
      return linkedPropertyIDs;
    }
  }

  static final class DefaultDenormalizedViewProperty extends DefautTransientProperty implements DenormalizedViewProperty {

    private final String foreignKeyPropertyID;
    private final Property denormalizedProperty;

    /**
     * @param propertyID the ID of the property, this should not be a column name since this property does not
     * map to a table column
     * @param foreignKeyPropertyID the ID of the foreign key property from which entity value this property gets its value
     * @param property the property from which this property gets its value
     * @param caption the caption of this property
     */
    DefaultDenormalizedViewProperty(final String propertyID, final String foreignKeyPropertyID, final Property property,
                                    final String caption) {
      super(propertyID, property.getType(), caption);
      this.foreignKeyPropertyID = foreignKeyPropertyID;
      this.denormalizedProperty = property;
      setReadOnly(true);
    }

    @Override
    public String getForeignKeyPropertyID() {
      return foreignKeyPropertyID;
    }

    @Override
    public Property getDenormalizedProperty() {
      return denormalizedProperty;
    }
  }

  static final class DefaultSubqueryProperty extends DefaultColumnProperty implements SubqueryProperty {

    private final String subquery;

    /**
     * @param propertyID the property ID, since SubqueryProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     * @param caption the caption of this property
     * @param subquery the sql query
     * @param columnType the actual column type
     */
    DefaultSubqueryProperty(final String propertyID, final int type, final String caption, final String subquery,
                            final int columnType) {
      super(propertyID, type, caption, columnType);
      setReadOnly(true);
      setUpdatable(false);
      this.subquery = subquery;
    }

    @Override
    public String getSubQuery() {
      return subquery;
    }
  }

  static class DefaultAuditProperty extends DefaultColumnProperty implements AuditProperty {

    private final AuditAction auditAction;

    DefaultAuditProperty(final String propertyID, final int type, final AuditAction auditAction, final String caption) {
      super(propertyID, type, caption);
      this.auditAction = auditAction;
      setReadOnly(true);
    }

    @Override
    public final AuditAction getAuditAction() {
      return auditAction;
    }
  }

  static final class DefaultAuditTimeProperty extends DefaultAuditProperty implements AuditTimeProperty {

    DefaultAuditTimeProperty(final String propertyID, final AuditAction auditAction, final String caption) {
      super(propertyID, Types.TIMESTAMP, auditAction, caption);
    }
  }

  static final class DefaultAuditUserProperty extends DefaultAuditProperty implements AuditUserProperty {

    DefaultAuditUserProperty(final String propertyID, final AuditAction auditAction, final String caption) {
      super(propertyID, Types.VARCHAR, auditAction, caption);
    }
  }

  static final class BooleanValueConverter implements ValueConverter {

    private final Object trueValue;
    private final Object falseValue;

    BooleanValueConverter() {
      this(Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_TRUE),
              Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_FALSE));
    }

    BooleanValueConverter(final Object trueValue, final Object falseValue) {
      this.trueValue = trueValue;
      this.falseValue = falseValue;
    }

    @Override
    public Boolean fromColumnValue(final Object columnValue) {
      if (Util.equal(trueValue, columnValue)) {
        return true;
      }
      else if (Util.equal(falseValue, columnValue)) {
        return false;
      }

      return null;
    }

    @Override
    public Object toColumnValue(final Object value) {
      if (value == null) {
        return null;
      }

      if ((Boolean) value) {
        return trueValue;
      }

      return falseValue;
    }
  }

  private static final class DefaultValueConverter implements ValueConverter {
    @Override
    public Object toColumnValue(final Object value) {
      return value;
    }

    @Override
    public Object fromColumnValue(final Object columnValue) {
      return columnValue;
    }
  }

  private static final class DateValueConverter implements ValueConverter {
    @Override
    public Object toColumnValue(final Object value) {
      if (value != null && !(value instanceof java.sql.Date)) {
        return new java.sql.Date(((java.util.Date) value).getTime());
      }
      return value;
    }

    @Override
    public Object fromColumnValue(final Object columnValue) {
      return columnValue;
    }
  }
}