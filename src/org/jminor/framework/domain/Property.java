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
import java.text.Format;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Represents a entity property, for example a database column.
 */
public class Property implements Serializable {

  private static final long serialVersionUID = 1;

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
   * The name of the underlying column
   * Only applicable to properties that map to an underlying table column
   */
  private final String columnName;

  /**
   * A reference to a parent foreign key property, if one exists
   */
  private ForeignKeyProperty parentProperty;

  /**
   * A default value for this property in new Entity instances
   */
  private Object defaultValue;

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
   * True if this property is updatable
   */
  private boolean updatable = true;

  /**
   * True if the value of this property is allowed to be null
   */
  private boolean nullable = true;

  /**
   * True if this property should be included when searching for entities
   */
  private boolean searchable = true;

  /**
   * True if the underlying column has a default value
   */
  private boolean columnHasDefaultValue = false;

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
   * Cached select column index
   */
  private int selectIndex = -1;

  /**
   * The Format used when presenting this Property value
   */
  private Format format;

  /**
   * Caching this frequently referenced attribute
   */
  private Class<?> typeClass;

  /**
   * This is based on an immutable field, so cache it
   */
  private int hashCode;

  /**
   * Instantiates a new property of the type Types.INTEGER
   * @param propertyID the property ID, this is used as the underlying column name,
   * override by calling setColumnName()
   */
  public Property(final String propertyID) {
    this(propertyID, Types.INTEGER);
  }

  /**
   * @param propertyID the property ID, this is used as the underlying column name,
   * override by calling setColumnName()
   * @param type the data type of this property
   */
  public Property(final String propertyID, final int type) {
    this(propertyID, type, null);
  }

  /**
   * @param propertyID the property ID, this is used as the underlying column name
   * @param type the data type of this property
   * @param caption the caption of this property, if this is null then this property is defined as hidden
   */
  public Property(final String propertyID, final int type, final String caption) {
    Util.rejectNullValue(propertyID);
    this.propertyID = propertyID;
    this.hashCode = propertyID.hashCode();
    this.type = type;
    this.caption = caption;
    this.columnName = propertyID;
    setHidden(caption == null);
    setFormat(initializeFormat());
  }

  /**
   * @return a String representation of this property
   */
  @Override
  public String toString() {
    return caption != null ? caption : propertyID;
  }

  /**
   * @param property the property
   * @return true if this property is of the given type
   */
  public boolean is(final Property property) {
    return is(property.propertyID);
  }

  /**
   * @return true if this is a numerical Property, that is, Integer or Double
   */
  public boolean isNumerical() {
    return isInteger() || isDouble();
  }

  /**
   * @return true if this is a time based property, Date or Timestamp
   */
  public boolean isTime() {
    return isDate() || isTimestamp();
  }

  /**
   * @return true if this is a date property
   */
  public boolean isDate() {
    return isType(Types.DATE);
  }

  /**
   * @return true if this is a timestamp property
   */
  public boolean isTimestamp() {
    return isType(Types.TIMESTAMP);
  }

  /**
   * @return true if this is a character property
   */
  public boolean isCharacter() {
    return isType(Types.CHAR);
  }

  /**
   * @return true if this is a string property
   */
  public boolean isString() {
    return isType(Types.VARCHAR);
  }

  /**
   * @return true if this is a integer property
   */
  public boolean isInteger() {
    return isType(Types.INTEGER);
  }

  /**
   * @return true if this is a double property
   */
  public boolean isDouble() {
    return isType(Types.DOUBLE);
  }

  /**
   * @return true if this is a boolean property
   */
  public boolean isBoolean() {
    return isType(Types.BOOLEAN);
  }

  /**
   * @return true if this is a reference property
   */
  public boolean isReference() {
    return isType(Types.REF);
  }

  /**
   * @param propertyID the property ID
   * @return true if this property is of the given type
   */
  public boolean is(final String propertyID) {
    return this.propertyID.equals(propertyID);
  }

  /**
   * @return the property identifier of this property
   */
  public String getPropertyID() {
    return this.propertyID;
  }

  /**
   * @return the data type of the value of this property
   */
  public int getType() {
    return type;
  }

  /**
   * @param type the type to check
   * @return true if the type of this property is the one given
   */
  public boolean isType(final int type) {
    return this.type == type;
  }

  /**
   * @return the name of the underlying column
   */
  public String getColumnName() {
    return this.columnName;
  }

  /**
   * @param hidden specifies whether this property should not be visible to the user
   * @return this Property instance
   */
  public Property setHidden(final boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  /**
   * @return true if this property should be hidden in table views
   */
  public boolean isHidden() {
    return hidden;
  }

  /**
   * @param updatable specifies whether this property is updatable
   * @return this Property instance
   */
  public Property setUpdatable(final boolean updatable) {
    if (hasParentProperty()) {
      throw new RuntimeException("Can not set the upatable status of a property with a parent property");
    }

    this.updatable = updatable;
    return this;
  }

  /**
   * Specifies whether or not this property is updatable, in case of
   * properties that have parent properties (properties which comprise a fk property fx)
   * inherit the updatable state of the parent property.
   * @return true if this property is updatable
   */
  public boolean isUpdatable() {
    if (parentProperty != null) {
      return parentProperty.isUpdatable();
    }

    return this.updatable;
  }

  /**
   * @param readOnly specifies whether this property should be included during insert/update operations
   * @return this Property instance
   */
  public Property setReadOnly(final boolean readOnly) {
    if (hasParentProperty()) {
      throw new RuntimeException("Can not set the read only status of a property with a parent property");
    }

    this.readOnly = readOnly;
    return this;
  }

  /**
   * Specifies whether or not this property is read only, in case of
   * properties that have parent properties (properties which comprise a fk property fx)
   * inherit the read only state of the parent property.
   * @return true if this property is for select only
   */
  public boolean isReadOnly() {
    if (parentProperty != null) {
      return parentProperty.isReadOnly();
    }

    return this.readOnly;
  }

  /**
   * Specifies whether or not this property is nullable, in case of
   * properties that have parent properties (properties which comprise a fk property fx)
   * inherit the nullable state of the parent property.
   * @param nullable specifies whether or not this property accepts a null value
   * @return this Property instance
   */
  public Property setNullable(final boolean nullable) {
    if (hasParentProperty()) {
      throw new RuntimeException("Can not set the nullable status of a property with a parent property");
    }

    this.nullable = nullable;
    return this;
  }

  /**
   * @return true if this property accepts a null value
   */
  public boolean isNullable() {
    if (parentProperty != null) {
      return parentProperty.isNullable();
    }

    return nullable;
  }

  /**
   * @param searchable specifies whether or not this property should be included when searching for entities
   * @return this Property instance
   */
  public Property setSearchable(final boolean searchable) {
    this.searchable = searchable;
    return this;
  }

  /**
   * @return true if this property shuld be included in search criteria
   */
  public boolean isSearchable() {
    return searchable;
  }

  /**
   * @return true if the underlying column has a default value
   */
  public boolean columnHasDefaultValue() {
    return columnHasDefaultValue;
  }

  /**
   * @param columnHasDefaultValue specifies whether or not the underlying column has a default value
   * @return this Property instance
   */
  public Property setColumnHasDefaultValue(final boolean columnHasDefaultValue) {
    this.columnHasDefaultValue = columnHasDefaultValue;
    return this;
  }

  /**
   * Sets the default value for this property, overrides the underlying column default value, if any
   * @param defaultValue the value to use as default
   * @return the property
   */
  public Property setDefaultValue(final Object defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * @return the default value for this property
   */
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  /**
   * Sets the maximum length of this property value
   * @param maxLength the maximum length
   * @return this Property instance
   */
  public Property setMaxLength(final int maxLength) {
    this.maxLength = maxLength;
    return this;
  }

  /**
   * @return the maximum length of this property value, -1 is returned if the max length is undefined
   */
  public int getMaxLength() {
    return maxLength;
  }

  /**
   * Sets the maximum fraction digits to show for this property, only applicable to DOUBLE properties
   * This setting is overridden during subsquence calls to <code>setFormat</code>
   * @param maximumFractionDigits the maximum fraction digits
   * @return this Property instance
   */
  public Property setMaximumFractionDigits(final int maximumFractionDigits) {
    if (!(format instanceof NumberFormat)) {
      throw new RuntimeException("Maximum fraction digits only good for number formats");
    }

    ((NumberFormat) format).setMaximumFractionDigits(maximumFractionDigits);
    return this;
  }

  /**
   * @return the maximum number of fraction digits to show for this property value
   */
  public int getMaximumFractionDigits() {
    return ((NumberFormat) format).getMaximumFractionDigits();
  }

  /**
   * @return the maximum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  public Double getMax() {
    return max;
  }

  /**
   * Sets the maximum allowed value for this property, only applicable to numerical properties
   * @param max the maximum allowed value
   * @return this Property instance
   */
  public Property setMax(final double max) {
    this.max = max;
    return this;
  }

  /**
   * @return the minimum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  public Double getMin() {
    return min;
  }

  /**
   * Sets the minimum allowed value for this property, only applicable to numerical properties
   * @param min the minimum allowed value
   * @return this Property instance
   */
  public Property setMin(final double min) {
    this.min = min;
    return this;
  }

  /**
   * Specifies whether to use number grouping when presenting this value.
   * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
   * Only applicable to numerical properties.
   * This setting is overridden during subsquence calls to <code>setFormat</code>
   * @param useGrouping if true then number grouping is used
   * @return this Property instance
   */
  public Property setUseNumberFormatGrouping(final boolean useGrouping) {
    if (!(format instanceof NumberFormat)) {
      throw new RuntimeException("Grouping only good for number formats");
    }

    ((NumberFormat) format).setGroupingUsed(useGrouping);
    return this;
  }

  /**
   * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
   * @return this Property instance
   */
  public Property setPreferredColumnWidth(final int preferredColumnWidth) {
    this.preferredColumnWidth = preferredColumnWidth;
    return this;
  }

  /**
   * @return the preferred column width of this property when
   * presented in a table, null if none has been specified
   */
  public Integer getPreferredColumnWidth() {
    return preferredColumnWidth;
  }

  /**
   * @return a String describing this property
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description a String describing this property
   * @return this Property instance
   */
  public Property setDescription(final String description) {
    this.description = description;
    return this;
  }

  /**
   * @return true if this property has a description
   */
  public boolean hasDescription() {
    return description != null;
  }

  /**
   * @return the mnemonic to use when creating a label for this property
   */
  public Character getMnemonic() {
    return mnemonic;
  }

  /**
   * Sets the mnemonic to use when creating a label for this property
   * @param mnemonic the mnemonic character
   * @return this Property instance
   */
  public Property setMnemonic(final Character mnemonic) {
    this.mnemonic = mnemonic;
    return this;
  }

  /**
   * @return the Format object used to format the value of properties when being presented
   */
  public Format getFormat() {
    return format;
  }

  /**
   * Sets the Format to use when presenting property values
   * @param format the format to use
   * @return this Property instance
   */
  public Property setFormat(final Format format) {
    this.format = format;
    return this;
  }

  /**
   * @return true if this property has a parent property
   */
  public boolean hasParentProperty() {
    return this.parentProperty != null;
  }

  /**
   * @return true if this property is a denormalized one which should get
   * its value from a parent foreign key property
   */
  public boolean isDenormalized() {
    return false;
  }

  /**
   * @return the caption used when the value of this property is presented
   */
  public String getCaption() {
    if (caption == null && hasParentProperty()) {
      return parentProperty.getCaption();
    }

    return caption;
  }

  /**
   * Sets the select column index
   * @param index the index
   */
  public void setSelectIndex(final int index) {
    this.selectIndex = index;
  }

  /**
   * @return the index of this property in a select query
   */
  public int getSelectIndex() {
    return selectIndex;
  }

  /**
   * @return true if this property maps to a database column
   */
  public boolean isDatabaseProperty() {
    return true;
  }

  /**
   * @param obj the object to compare with
   * @return true if object is a Property instance and has a
   * property identifier equal to that of this property
   */
  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof Property && this.propertyID.equals(((Property) obj).propertyID);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * @return the Class representing this property type
   */
  public Class<?> getTypeClass() {
    if (typeClass == null) {
      typeClass = getTypeClass(type);
    }

    return typeClass;
  }

  /**
   * @param sqlType the type
   * @return the Class representing the given type
   */
  public static Class<?> getTypeClass(final int sqlType) {
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

  private Format initializeFormat() {
    if (isTime()) {
      return isDate() ? Configuration.getDefaultDateFormat() : Configuration.getDefaultTimestampFormat();
    }
    else if (isNumerical()) {
      return NumberFormat.getInstance();
    }

    return null;
  }

  /**
   * Sets the parent property
   * @param parentProperty the property to set as parent property
   */
  private void setParentProperty(final ForeignKeyProperty parentProperty) {
    this.parentProperty = parentProperty;
  }

  /**
   * A property that is part of a entities primary key.
   * A primary key property is by default non-updatable.
   */
  public static class PrimaryKeyProperty extends Property {

    /**
     * This property's index in the primary key
     */
    private int index = 0;

    public PrimaryKeyProperty(final String propertyID) {
      this(propertyID, Types.INTEGER);
    }

    public PrimaryKeyProperty(final String propertyID, final int type) {
      this(propertyID, type, null);
    }

    public PrimaryKeyProperty(final String propertyID, final int type, final String caption) {
      super(propertyID, type, caption);
      setUpdatable(false);
    }

    public int getIndex() {
      return index;
    }

    public PrimaryKeyProperty setIndex(final int primaryKeyIndex) {
      if (primaryKeyIndex < 0) {
        throw new IllegalArgumentException("Primary key index must be at least 0");
      }
      this.index = primaryKeyIndex;
      return this;
    }
  }

  /**
   * A meta property that represents a reference to another entity, typically but not necessarily based on a foreign key.
   * These do not map directly to a underlying table column, but wrap the actual column properties involved in the relation.
   * e.g.: new Property.ForeignKeyProperty("reference_fk", new Property("reference_id")), where "reference_id" is the
   * actual name of the column involved in the reference, but "reference_fk" is simply a descriptive property ID
   */
  public static class ForeignKeyProperty extends Property {

    private final String referencedEntityID;

    private final List<Property> referenceProperties;
    private Map<Property, String> linkedReferenceProperties;

    private int fetchDepth = Configuration.getIntValue(Configuration.DEFAULT_FOREIGN_KEY_FETCH_DEPTH);

    /**
     * @param propertyID the property ID, since EntityProperties are meta properties, the property ID should not
     * be a underlying table column, it must only be unique for this entity
     * @param caption the property caption
     * @param referencedEntityID the ID of the referenced entity type
     * @param referenceProperty the actual column property involved in the reference
     */
    public ForeignKeyProperty(final String propertyID, final String caption, final String referencedEntityID,
                              final Property referenceProperty) {
      this(propertyID, caption, referencedEntityID, new Property[] {referenceProperty}, new String[0]);
    }

    /**
     * @param propertyID the property ID, since EntityProperties are meta properties, the property ID should not
     * be a underlying table column, it must only be unique for this entity
     * @param caption the property caption
     * @param referencedEntityID the ID of the referenced entity type
     * @param referenceProperties the actual column properties involved in the reference
     * @param referencedPropertyIDs the IDs of the properties referenced, in the same order as the properties
     */
    public ForeignKeyProperty(final String propertyID, final String caption, final String referencedEntityID,
                              final Property[] referenceProperties, final String[] referencedPropertyIDs) {
      super(propertyID, Types.REF, caption);
      for (final Property referenceProperty : referenceProperties) {
        if (referenceProperty.propertyID.equals(propertyID)) {
          throw new IllegalArgumentException(referencedEntityID + ", reference property does not have a unique name: " + propertyID);
        }
      }
      if (referencedEntityID == null) {
        throw new IllegalArgumentException("referencedEntityID is null: " + propertyID);
      }
      if (referenceProperties.length > 1 && referencedPropertyIDs.length != referenceProperties.length) {
        throw new IllegalArgumentException("Reference property count mismatch");
      }

      for (int i = 0; i < referenceProperties.length; i++) {
        final Property referenceProperty = referenceProperties[i];
        referenceProperty.setParentProperty(this);
        if (referencedPropertyIDs.length > i) {
          link(referenceProperty, referencedPropertyIDs[i]);
        }
      }
      this.referencedEntityID = referencedEntityID;
      this.referenceProperties = Collections.unmodifiableList(Arrays.asList(referenceProperties));
    }

    /**
     * @return the ID of the referenced entity
     */
    public String getReferencedEntityID() {
      return referencedEntityID;
    }

    /**
     * Returns a list containing the actual reference properties,
     * N.B. this list should not be modified.
     * @return the reference properties
     */
    public List<Property> getReferenceProperties() {
      return referenceProperties;
    }

    /**
     * @return true if this reference is based on more than on column
     */
    public boolean isCompositeReference() {
      return this.referenceProperties.size() > 1;
    }

    @Override
    public final String getColumnName() {
      throw new RuntimeException("Foreign key properties do not have column names");
    }

    public int getFetchDepth() {
      return fetchDepth;
    }

    public ForeignKeyProperty setFetchDepth(final int fetchDepth) {
      this.fetchDepth = fetchDepth;
      return this;
    }

    public String getReferencedPropertyID(final Property referenceProperty) {
      if (linkedReferenceProperties == null) {
        return null;
      }

      if (!linkedReferenceProperties.containsKey(referenceProperty)) {
        throw new RuntimeException("No referenced property ID associated with reference property: " + referenceProperty);
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

  /**
   * Represents a child foreign key property that is already included as part of another reference foreign key property,
   * and should not handle updating the underlying property
   */
  //todo better explanation
  public static class MirrorProperty extends Property {

    public MirrorProperty(final String propertyID) {
      super(propertyID);
    }
  }

  /**
   * A property representing a column that should get its value automatically from a column in a referenced table
   */
  public static class DenormalizedProperty extends Property {

    private final String foreignKeyPropertyID;

    private final Property denormalizedProperty;

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param foreignKeyPropertyID the ID of the foreign key property which references the entity which owns
     * the denormalized property
     * @param denormalizedProperty the property from which this property should get its value
     */
    public DenormalizedProperty(final String propertyID, final String foreignKeyPropertyID,
                                final Property denormalizedProperty) {
      this(propertyID, foreignKeyPropertyID, denormalizedProperty, null);
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param foreignKeyPropertyID the ID of the foreign key property which references the entity which owns
     * the denormalized property
     * @param denormalizedProperty the property from which this property should get its value
     * @param caption the caption if this property
     */
    public DenormalizedProperty(final String propertyID, final String foreignKeyPropertyID,
                                final Property denormalizedProperty, final String caption) {
      super(propertyID, denormalizedProperty.getType(), caption);
      this.foreignKeyPropertyID = foreignKeyPropertyID;
      this.denormalizedProperty = denormalizedProperty;
    }

    /**
     * @return the id of the foreign key property (entity) from which this property should retrieve its value
     */
    public String getForeignKeyPropertyID() {
      return foreignKeyPropertyID;
    }

    /**
     * @return the property from which this property gets its value
     */
    public Property getDenormalizedProperty() {
      return denormalizedProperty;
    }

    @Override
    public boolean isDenormalized() {
      return true;
    }
  }

  /**
   * A property based on a list of values, each with a displayable caption.
   */
  public static class ValueListProperty extends Property {

    private final List<Item<Object>> values;

    /**
     * Instantiates a new hidden value list property
     * @param propertyID the property ID
     * @param type the data type of this property
     * @param values the values to base this property on
     */
    public ValueListProperty(final String propertyID, final int type, final List<Item<Object>> values) {
      this(propertyID, type, null, values);
    }

    /**
     * @param propertyID the property ID
     * @param type the data type of this property
     * @param caption the property caption
     * @param values the values to base this property on
     */
    public ValueListProperty(final String propertyID, final int type, final String caption,
                             final List<Item<Object>> values) {
      super(propertyID, type, caption);
      this.values = Collections.unmodifiableList(values);
    }

    public boolean isValid(final Object value) {
      return values.contains(new Item<Object>(value, ""));
    }

    /**
     * @return an unmodifiable view of the values
     */
    public List<Item<Object>> getValues() {
      return values;
    }

    public String getCaption(final Object value) {
      final Item item = new Item<Object>(value, "");
      final int index = values.indexOf(item);
      if (index >= 0) {
        return values.get(index).getCaption();
      }

      return "";
    }
  }

  /**
   * A property that does not map to an underlying database column. The value of a transient property
   * is initialized to null when entities are loaded, which means transient properties always have
   * a original value of null.
   * The value of transient properties can be set and retrieved like normal properties.
   */
  public static class TransientProperty extends Property {

    /**
     * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     */
    public TransientProperty(final String propertyID, final int type) {
      this(propertyID, type, null);
    }

    /**
     * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     * @param caption the caption of this property
     */
    public TransientProperty(final String propertyID, final int type, final String caption) {
      super(propertyID, type, caption);
      super.setUpdatable(false);
      super.setSearchable(false);
    }

    @Override
    public final Property setUpdatable(final boolean updatable) {
      throw new IllegalArgumentException("TransientProperty can not be updatable");
    }

    @Override
    public final Property setSearchable(final boolean searchable) {
      throw new IllegalArgumentException("TransientProperty can not be searchable");
    }

    /**
     * @return true if this property maps to a database column
     */
    @Override
    public final boolean isDatabaseProperty() {
      return false;
    }

    @Override
    public final String getColumnName() {
      throw new RuntimeException("Transient properties do not have column names");
    }
  }

  /**
   * A property which value is derived from the values of one or more properties.
   * For the property to be updated when the parent properties are you must
   * link the properties together using the <code>addLinkedPropertyIDs()</code>method.
   * @see org.jminor.framework.domain.Entity.Proxy#getDerivedValue(Entity, org.jminor.framework.domain.Property.DerivedProperty)
   */
  public static class DerivedProperty extends TransientProperty {

    private Collection<String> linkedPropertyIDs;

    public DerivedProperty(final String propertyID, final int type, final String caption) {
      super(propertyID, type, caption);
      super.setReadOnly(true);
    }

    @Override
    public Property setReadOnly(final boolean readOnly) {
      throw new RuntimeException("Derived properties are read only by definition");
    }

    /**
     * @return the IDs of properties that trigger a change event for this property
     */
    public Collection<String> getLinkedPropertyIDs() {
      return linkedPropertyIDs;
    }

    /**
     * Adds a property change link on the property identified by <code>linkedPropertyID</code>,
     * so that changes in that property trigger a change in this property
     * @param linkedPropertyIDs the IDs of the properties on which to link
     * @return this TransientProperty instance
     */
    public DerivedProperty addLinkedPropertyIDs(final String... linkedPropertyIDs) {
      if (this.linkedPropertyIDs == null) {
        this.linkedPropertyIDs = new HashSet<String>();
      }
      this.linkedPropertyIDs.addAll(Arrays.asList(linkedPropertyIDs));
      return this;
    }
  }

  /**
   * A property that gets its value from a entity referenced by a foreign key, but is for
   * display only, and does not map to a database column
   */
  public static class DenormalizedViewProperty extends TransientProperty {

    private final String foreignKeyPropertyID;

    private final Property denormalizedProperty;

    /**
     * @param propertyID the ID of the property, this should not be a column name since this property does not
     * map to a table column
     * @param foreignKeyPropertyID the ID of the foreign key property from which entity value this property gets its value
     * @param property the property from which this property gets its value
     */
    public DenormalizedViewProperty(final String propertyID, final String foreignKeyPropertyID, final Property property) {
      this(propertyID, foreignKeyPropertyID, property, null);
    }

    /**
     * @param propertyID the ID of the property, this should not be a column name since this property does not
     * map to a table column
     * @param foreignKeyPropertyID the ID of the foreign key property from which entity value this property gets its value
     * @param property the property from which this property gets its value
     * @param caption the caption of this property
     */
    public DenormalizedViewProperty(final String propertyID, final String foreignKeyPropertyID, final Property property,
                                    final String caption) {
      super(propertyID, property.getType(), caption);
      this.foreignKeyPropertyID = foreignKeyPropertyID;
      this.denormalizedProperty = property;
    }


    /**
     * @return the id of the foreign key property (entity) from which this property should retrieve its value
     */
    public String getForeignKeyPropertyID() {
      return foreignKeyPropertyID;
    }

    /**
     * @return the property from which this property gets its value
     */
    public Property getDenormalizedProperty() {
      return denormalizedProperty;
    }

    @Override
    public boolean isDenormalized() {
      return true;
    }
  }

  /**
   * A property based on a subquery, returning a single value
   */
  public static class SubqueryProperty extends Property {

    private final String subquery;

    /**
     * @param propertyID the property ID, since SubqueryProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     * @param caption the caption of this property
     * @param subquery the sql query
     */
    public SubqueryProperty(final String propertyID, final int type, final String caption, final String subquery) {
      super(propertyID, type, caption);
      super.setReadOnly(true);
      super.setUpdatable(false);
      this.subquery = subquery;
    }

    @Override
    public Property setUpdatable(final boolean updatable) {
      throw new IllegalArgumentException("SubqueryProperty can not be updatable");
    }

    @Override
    public Property setReadOnly(final boolean readOnly) {
      throw new IllegalArgumentException("SubqueryProperty can only be select only");
    }

    /**
     * @return the subquery string
     */
    public String getSubQuery() {
      return subquery;
    }

    @Override
    public final String getColumnName() {
      throw new RuntimeException("Subquery properties do not have column names");
    }
  }

  /**
   * A boolean property, with special handling since different values
   * are used for representing boolean values in different systems
   */
  public static class BooleanProperty extends Property {

    private final int columnType;
    /**
     * the Object value representing true
     */
    private final Object trueValue;
    /**
     * the Object value representing false
     */
    private final Object falseValue;

    /**
     * Instantiates a BooleanProperty based on the INT data type
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param caption the caption of this property
     */
    public BooleanProperty(final String propertyID, final String caption) {
      this(propertyID, Types.INTEGER, caption);
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param columnType the data type of the underlying column
     * @param caption the caption of this property
     */
    public BooleanProperty(final String propertyID, final int columnType, final String caption) {
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
    public BooleanProperty(final String propertyID, final int columnType, final String caption,
                           final Object trueValue, final Object falseValue) {
      super(propertyID, Types.BOOLEAN, caption);
      this.columnType = columnType;
      this.trueValue = trueValue;
      this.falseValue = falseValue;
    }

    /**
     * @return the data type of the underlying column
     */
    public int getColumnType() {
      return columnType;
    }

    /**
     * @param object the Object value to translate into a Boolean value
     * @return the Boolean value of <code>object</code>
     */
    public Boolean toBoolean(final Object object) {
      if (Util.equal(trueValue, object)) {
        return true;
      }
      else if (Util.equal(falseValue, object)) {
        return false;
      }

      return null;
    }

    /**
     * @param value the Boolean value to translate into a sql string value
     * @return the sql string value of <code>value</code>
     */
    public String toSQLString(final Boolean value) {
      final Object result = toSQLValue(value);
      if (columnType == Types.VARCHAR) {
        return "'" + result + "'";
      }
      else {
        return result == null ? "null" : result.toString();
      }
    }

    public Object toSQLValue(final Boolean value) {
      return value == null ? null : (value ? trueValue : falseValue);
    }
  }

  /**
   * A BLOB property, based on two columns, the actual BLOB column and a column containing the name of the BLOB object.
   */
  public static class BlobProperty extends Property {

    private final String blobColumnName;

    public BlobProperty(final String propertyID, final String blobColumnName, final String caption) {
      super(propertyID, Types.VARCHAR, caption);
      super.setHidden(true);
      this.blobColumnName = blobColumnName;
    }

    public String getBlobColumnName() {
      return blobColumnName;
    }
  }
}