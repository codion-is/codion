/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Item;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;

import java.io.Serializable;
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
class PropertyImpl implements Property, Serializable {

  private static final long serialVersionUID = 1;

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
   * The caption to use when this property is presented
   */
  private final String caption;

  /**
   * This is based on an immutable field, so cache it
   */
  private final int hashCode;

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
   * Caching this frequently referenced attribute
   */
  private Class<?> typeClass;

  /**
   * @param propertyID the property ID, this is used as the underlying column name
   * @param type the data type of this property
   * @param caption the caption of this property, if this is null then this property is defined as hidden
   */
  PropertyImpl(final String propertyID, final int type, final String caption) {
    Util.rejectNullValue(propertyID, "propertyID");
    this.propertyID = propertyID;
    this.hashCode = propertyID.hashCode();
    this.type = type;
    this.caption = caption;
    setHidden(caption == null);
    setFormat(initializeDefaultFormat());
  }

  /**
   * @return a String representation of this property
   */
  @Override
  public final String toString() {
    if (caption == null) {
      return propertyID;
    }

    return caption;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean is(final Property property) {
    return is(property.getPropertyID());
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNumerical() {
    return isInteger() || isDouble();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isTime() {
    return isDate() || isTimestamp();
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
  public final boolean isBoolean() {
    return isType(Types.BOOLEAN);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean is(final String propertyID) {
    return this.propertyID.equals(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final String getPropertyID() {
    return this.propertyID;
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
    if (isNumerical() && !(format instanceof NumberFormat)) {
      throw new IllegalArgumentException("NumberFormat expected for numerical property: " + propertyID);
    }
    if (isTime() && !(format instanceof DateFormat)) {
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
  public boolean equals(final Object obj) {
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
    if (typeClass == null) {
      typeClass = getTypeClass(type);
    }

    return typeClass;
  }

  private Format initializeDefaultFormat() {
    if (isTime()) {
      if (isDate()) {
        return Configuration.getDefaultDateFormat();
      }
      else {
        return Configuration.getDefaultTimestampFormat();
      }
    }
    else if (isNumerical()) {
      final NumberFormat numberFormat = Util.getNonGroupingNumberFormat(isInteger());
      if (isDouble()) {
        numberFormat.setMaximumFractionDigits(Configuration.getIntValue(Configuration.DEFAULT_MAXIMUM_FRACTION_DIGITS));
      }

      return numberFormat;
    }

    return null;
  }

  /**
   * @param sqlType the type
   * @return the Class representing the given type
   */
  private static Class<?> getTypeClass(final int sqlType) {
    switch (sqlType) {
      case Types.INTEGER:
        return Integer.class;
      case Types.DOUBLE:
        return Double.class;
      case Types.DATE:
        return Date.class;
      case Types.TIMESTAMP:
        return Timestamp.class;
      case Types.VARCHAR:
        return String.class;
      case Types.BOOLEAN:
        return Boolean.class;
      case Types.CHAR:
        return Character.class;

      default:
        return Object.class;
    }
  }

  static class ColumnPropertyImpl extends PropertyImpl implements ColumnProperty {

    private static final long serialVersionUID = 1;

    private final String columnName;
    private int selectIndex;
    private boolean columnHasDefaultValue = false;
    private boolean updatable = true;
    private boolean searchable = true;
    private boolean groupingColumn = false;
    private boolean aggregateColumn = false;
    private ForeignKeyProperty foreignKeyProperty = null;

    ColumnPropertyImpl(final String propertyID, final int type, final String caption) {
      super(propertyID, type, caption);
      this.columnName = propertyID;
    }

    /** {@inheritDoc} */
    @Override
    public final String getColumnName() {
      return this.columnName;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean columnHasDefaultValue() {
      return columnHasDefaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public final ColumnProperty setColumnHasDefaultValue(final boolean columnHasDefaultValue) {
      this.columnHasDefaultValue = columnHasDefaultValue;
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isUpdatable() {
      return this.updatable;
    }

    /** {@inheritDoc} */
    @Override
    public final ColumnProperty setUpdatable(final boolean updatable) {
      this.updatable = updatable;
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public ColumnProperty setSearchable(final boolean searchable) {
      this.searchable = searchable;
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSearchable() {
      return searchable;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDenormalized() {
      return false;
    }

    /** {@inheritDoc} */
    @Override
    public final void setSelectIndex(final int selectIndex) {
      this.selectIndex = selectIndex;
    }

    /** {@inheritDoc} */
    @Override
    public final int getSelectIndex() {
      return selectIndex;
    }

    /** {@inheritDoc} */
    @Override
    public ColumnProperty setGroupingColumn(final boolean groupingColumn) {
      if (aggregateColumn) {
        throw new IllegalStateException(columnName + " is an aggregate column");
      }
      this.groupingColumn = groupingColumn;
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGroupingColumn() {
      return groupingColumn;
    }

    /** {@inheritDoc} */
    @Override
    public ColumnProperty setAggregateColumn(final boolean aggregateColumn) {
      if (groupingColumn) {
        throw new IllegalStateException(columnName + " is a grouping column");
      }
      this.aggregateColumn = aggregateColumn;
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAggregateColumn() {
      return aggregateColumn;
    }

    /** {@inheritDoc} */
    @Override
    public final void setForeignKeyProperty(final ForeignKeyProperty foreignKeyProperty) {
      this.foreignKeyProperty = foreignKeyProperty;
    }

    /** {@inheritDoc} */
    @Override
    public final ForeignKeyProperty getForeignKeyProperty() {
      return foreignKeyProperty;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isForeignKeyProperty() {
      return foreignKeyProperty != null;
    }

    /** {@inheritDoc} */
    @Override
    public final Property setReadOnly(final boolean readOnly) {
      if (isForeignKeyProperty()) {
        throw new IllegalStateException("Can not set the read only status of a property which is part of a foreign key property");
      }

      return super.setReadOnly(readOnly);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isReadOnly() {
      if (foreignKeyProperty != null) {
        return foreignKeyProperty.isReadOnly();
      }

      return super.isReadOnly();
    }

    /** {@inheritDoc} */
    @Override
    public final String getCaption() {
      final String superCaption = super.getCaption();
      if (superCaption == null && isForeignKeyProperty()) {
        return foreignKeyProperty.getCaption();
      }

      return superCaption;
    }
  }

  static class PrimaryKeyPropertyImpl extends ColumnPropertyImpl implements PrimaryKeyProperty {

    private static final long serialVersionUID = 1;

    private int index = 0;

    PrimaryKeyPropertyImpl(final String propertyID, final int type, final String caption) {
      super(propertyID, type, caption);
      setUpdatable(false);
    }

    /** {@inheritDoc} */
    @Override
    public final int getIndex() {
      return index;
    }

    /** {@inheritDoc} */
    @Override
    public final PrimaryKeyProperty setIndex(final int index) {
      if (index < 0) {
        throw new IllegalArgumentException("Primary key index must be at least 0");
      }
      this.index = index;
      return this;
    }
  }

  static class ForeignKeyPropertyImpl extends PropertyImpl implements Property.ForeignKeyProperty {

    private static final long serialVersionUID = 1;

    private final String referencedEntityID;
    private final List<ColumnProperty> referenceProperties;
    private final boolean compositeReference;
    private Map<Property, String> linkedReferenceProperties;
    private int fetchDepth = Configuration.getIntValue(Configuration.DEFAULT_FOREIGN_KEY_FETCH_DEPTH);

    /**
     * @param propertyID the property ID, since EntityProperties are meta properties, the property ID should not
     * be a underlying table column, it must only be unique for this entity
     * @param caption the property caption
     * @param referencedEntityID the ID of the referenced entity type
     * @param referenceProperty the actual column property involved in the reference
     */
    ForeignKeyPropertyImpl(final String propertyID, final String caption, final String referencedEntityID,
                           final ColumnProperty referenceProperty) {
      this(propertyID, caption, referencedEntityID, new ColumnProperty[] {referenceProperty}, new String[0]);
    }

    /**
     * @param propertyID the property ID, since EntityProperties are meta properties, the property ID should not
     * be a underlying table column, it must only be unique for this entity
     * @param caption the property caption
     * @param referencedEntityID the ID of the referenced entity type
     * @param referenceProperties the actual column properties involved in the reference
     * @param referencedPropertyIDs the IDs of the properties referenced, in the same order as the reference properties
     */
    ForeignKeyPropertyImpl(final String propertyID, final String caption, final String referencedEntityID,
                           final ColumnProperty[] referenceProperties, final String[] referencedPropertyIDs) {
      super(propertyID, Types.REF, caption);
      Util.rejectNullValue(referencedEntityID, "referencedEntityID");
      for (final Property referenceProperty : referenceProperties) {
        Util.rejectNullValue(referenceProperty, "referenceProperty");
        if (referenceProperty.getPropertyID().equals(propertyID)) {
          throw new IllegalArgumentException(referencedEntityID + ", reference property does not have a unique name: " + propertyID);
        }
      }
      if (referenceProperties.length > 1 && referencedPropertyIDs.length != referenceProperties.length) {
        throw new IllegalArgumentException("Reference property count mismatch");
      }

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

    /** {@inheritDoc} */
    @Override
    public final boolean isUpdatable() {
      for (final ColumnProperty referenceProperty : referenceProperties) {
        if (!referenceProperty.isUpdatable()) {
          return false;
        }
      }

      return true;
    }

    /** {@inheritDoc} */
    @Override
    public final ForeignKeyProperty setNullable(final boolean nullable) {
      for (final ColumnProperty columnProperty : referenceProperties) {
        columnProperty.setNullable(nullable);
      }

      return (ForeignKeyProperty) super.setNullable(nullable);
    }

    /** {@inheritDoc} */
    @Override
    public final String getReferencedEntityID() {
      return referencedEntityID;
    }

    /** {@inheritDoc} */
    @Override
    public final List<ColumnProperty> getReferenceProperties() {
      return referenceProperties;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isCompositeReference() {
      return compositeReference;
    }

    /** {@inheritDoc} */
    @Override
    public final int getFetchDepth() {
      return fetchDepth;
    }

    /** {@inheritDoc} */
    @Override
    public final ForeignKeyProperty setFetchDepth(final int fetchDepth) {
      this.fetchDepth = fetchDepth;
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public final String getReferencedPropertyID(final Property referenceProperty) {
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
        linkedReferenceProperties = new HashMap<Property, String>();
      }
      linkedReferenceProperties.put(referenceProperty, referencedPropertyID);
    }
  }

  static class MirrorPropertyImpl extends ColumnPropertyImpl implements MirrorProperty {

    private static final long serialVersionUID = 1;

    MirrorPropertyImpl(final String propertyID) {
      super(propertyID, -1, null);
    }
  }

  /**
   * A property representing a column that should get its value automatically from a column in a referenced table
   */
  static class DenormalizedPropertyImpl extends ColumnPropertyImpl implements DenormalizedProperty {

    private static final long serialVersionUID = 1;

    private final String foreignKeyPropertyID;
    private final Property denormalizedProperty;

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param foreignKeyPropertyID the ID of the foreign key property which references the entity which owns
     * the denormalized property
     * @param denormalizedProperty the property from which this property should get its value
     * @param caption the caption if this property
     */
    DenormalizedPropertyImpl(final String propertyID, final String foreignKeyPropertyID,
                             final Property denormalizedProperty, final String caption) {
      super(propertyID, denormalizedProperty.getType(), caption);
      this.foreignKeyPropertyID = foreignKeyPropertyID;
      this.denormalizedProperty = denormalizedProperty;
    }

    /** {@inheritDoc} */
    @Override
    public final String getForeignKeyPropertyID() {
      return foreignKeyPropertyID;
    }

    /** {@inheritDoc} */
    @Override
    public final Property getDenormalizedProperty() {
      return denormalizedProperty;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isDenormalized() {
      return true;
    }
  }

  static class ValueListPropertyImpl extends ColumnPropertyImpl implements ValueListProperty {

    private static final long serialVersionUID = 1;

    private final List<Item<Object>> values;

    /**
     * @param propertyID the property ID
     * @param type the data type of this property
     * @param caption the property caption
     * @param values the values to base this property on
     */
    ValueListPropertyImpl(final String propertyID, final int type, final String caption,
                          final List<Item<Object>> values) {
      super(propertyID, type, caption);
      this.values = Collections.unmodifiableList(values);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isValid(final Object value) {
      return values.contains(new Item<Object>(value, ""));
    }

    /** {@inheritDoc} */
    @Override
    public final List<Item<Object>> getValues() {
      return values;
    }

    /** {@inheritDoc} */
    @Override
    public final String getCaption(final Object value) {
      final Item item = new Item<Object>(value, "");
      final int index = values.indexOf(item);
      if (index >= 0) {
        return values.get(index).getCaption();
      }

      return "";
    }
  }

  static class TransientPropertyImpl extends PropertyImpl implements TransientProperty {

    private static final long serialVersionUID = 1;

    /**
     * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     * @param caption the caption of this property
     */
    TransientPropertyImpl(final String propertyID, final int type, final String caption) {
      super(propertyID, type, caption);
    }
  }

  static class DerivedPropertyImpl extends TransientPropertyImpl implements DerivedProperty {

    private static final long serialVersionUID = 1;

    private final Provider valueProvider;
    private final List<String> linkedPropertyIDs;

    DerivedPropertyImpl(final String propertyID, final int type, final String caption,
                        final Provider valueProvider, final String... linkedPropertyIDs) {
      super(propertyID, type, caption);
      this.valueProvider = valueProvider;
      if (linkedPropertyIDs == null || linkedPropertyIDs.length == 0) {
        throw new IllegalArgumentException("No linked propertyIDs, a derived property must be derived from one or more properties");
      }
      else {
        this.linkedPropertyIDs = Arrays.asList(linkedPropertyIDs);
      }
      setReadOnly(true);
    }

    /** {@inheritDoc} */
    @Override
    public Provider getValueProvider() {
      return valueProvider;
    }

    /** {@inheritDoc} */
    @Override
    public final List<String> getLinkedPropertyIDs() {
      return linkedPropertyIDs;
    }
  }

  static class DenormalizedViewPropertyImpl extends TransientPropertyImpl implements DenormalizedViewProperty {

    private static final long serialVersionUID = 1;

    private final String foreignKeyPropertyID;
    private final Property denormalizedProperty;

    /**
     * @param propertyID the ID of the property, this should not be a column name since this property does not
     * map to a table column
     * @param foreignKeyPropertyID the ID of the foreign key property from which entity value this property gets its value
     * @param property the property from which this property gets its value
     * @param caption the caption of this property
     */
    DenormalizedViewPropertyImpl(final String propertyID, final String foreignKeyPropertyID, final Property property,
                                 final String caption) {
      super(propertyID, property.getType(), caption);
      this.foreignKeyPropertyID = foreignKeyPropertyID;
      this.denormalizedProperty = property;
    }

    /** {@inheritDoc} */
    @Override
    public final String getForeignKeyPropertyID() {
      return foreignKeyPropertyID;
    }

    /** {@inheritDoc} */
    @Override
    public final Property getDenormalizedProperty() {
      return denormalizedProperty;
    }
  }

  static class SubqueryPropertyImpl extends ColumnPropertyImpl implements SubqueryProperty {

    private static final long serialVersionUID = 1;

    private final String subquery;

    /**
     * @param propertyID the property ID, since SubqueryProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     * @param caption the caption of this property
     * @param subquery the sql query
     */
    SubqueryPropertyImpl(final String propertyID, final int type, final String caption, final String subquery) {
      super(propertyID, type, caption);
      setReadOnly(true);
      setUpdatable(false);
      this.subquery = subquery;
    }

    /** {@inheritDoc} */
    @Override
    public final String getSubQuery() {
      return subquery;
    }
  }

  static class BooleanPropertyImpl extends ColumnPropertyImpl implements BooleanProperty {

    private static final long serialVersionUID = 1;

    private final int columnType;
    private final Object trueValue;
    private final Object falseValue;

    /**
     * Instantiates a BooleanProperty based on the INT data type
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param caption the caption of this property
     */
    BooleanPropertyImpl(final String propertyID, final String caption) {
      this(propertyID, Types.INTEGER, caption);
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param columnType the data type of the underlying column
     * @param caption the caption of this property
     */
    BooleanPropertyImpl(final String propertyID, final int columnType, final String caption) {
      this(propertyID, columnType, caption, Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_TRUE),
              Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_FALSE));
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param columnType the data type of the underlying column
     * @param caption the caption of this property
     * @param trueValue the Object value representing 'true' in the underlying column
     * @param falseValue the Object value representing 'false' in the underlying column
     */
    BooleanPropertyImpl(final String propertyID, final int columnType, final String caption,
                        final Object trueValue, final Object falseValue) {
      super(propertyID, Types.BOOLEAN, caption);
      this.columnType = columnType;
      this.trueValue = trueValue;
      this.falseValue = falseValue;
    }

    /** {@inheritDoc} */
    @Override
    public final int getColumnType() {
      return columnType;
    }

    /** {@inheritDoc} */
    @Override
    public final Boolean toBoolean(final Object object) {
      if (Util.equal(trueValue, object)) {
        return true;
      }
      else if (Util.equal(falseValue, object)) {
        return false;
      }

      return null;
    }

    /** {@inheritDoc} */
    @Override
    public final Object toSQLValue(final Boolean value) {
      if (value == null) {
        return null;
      }

      if (value) {
        return trueValue;
      }

      return falseValue;
    }
  }

  static class AuditPropertyImpl extends ColumnPropertyImpl implements AuditProperty {

    private static final long serialVersionUID = 1;
    private final AuditAction auditAction;

    AuditPropertyImpl(final String propertyID, final int type, final AuditAction auditAction, final String caption) {
      super(propertyID, type, caption);
      this.auditAction = auditAction;
      setReadOnly(true);
    }

    /** {@inheritDoc} */
    @Override
    public final AuditAction getAuditAction() {
      return auditAction;
    }
  }

  static class AuditTimePropertyImpl extends AuditPropertyImpl implements AuditTimeProperty {

    private static final long serialVersionUID = 1;

    AuditTimePropertyImpl(final String propertyID, final AuditAction auditAction, final String caption) {
      super(propertyID, Types.TIMESTAMP, auditAction, caption);
    }
  }

  static class AuditUserPropertyImpl extends AuditPropertyImpl implements AuditUserProperty {

    private static final long serialVersionUID = 1;

    AuditUserPropertyImpl(final String propertyID, final AuditAction auditAction, final String caption) {
      super(propertyID, Types.VARCHAR, auditAction, caption);
    }
  }
}