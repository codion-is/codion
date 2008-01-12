/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.model;

import org.jminor.common.Constants;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a entity property, for example a database column.
 * Contains some representation data
 */
public class Property implements Serializable {

  /**
   * The property type
   */
  public final Type propertyType;

  /**
   * The property identifier, should be unique within an Entity.
   * Serves as column name for database properties.
   * @see #getColumnName
   */
  public final String propertyID;

  /**
   * The caption to use when this property is presented
   */
  private final String caption;

  /**
   * The preferred column width when this property is presented in a table
   */
  private final int preferredColumnWidth;

  /**
   * True if this property should be hidden in table views
   */
  private final boolean hidden;

  /**
   * True if this property is for selecting only, implicitly not updatable
   * and not used in insert statements
   */
  private final boolean selectOnly;

  /**
   * True if this property is updatable
   */
  private final boolean isUpdatable;

  /**
   * A reference to a parent property, if one exists
   */
  private EntityProperty parentProperty;

  /**
   * A default value for this property in new Entity instances
   */
  private Object defaultValue = null;

  /**
   * Cached hash code
   */
  private final int hashCode;

  /**
   * Cached select column index
   */
  public int selectIndex = -1;

  public Property(final String propertyID) {
    this(propertyID, Type.INT);
  }

  public Property(final String propertyID, final Type propertyType) {
    this(propertyID, propertyType, null);
  }

  public Property(final String propertyID, final Type propertyType, final String caption) {
    this(propertyID, propertyType, caption, caption == null);
  }

  public Property(final String propertyID, final Type propertyType, final String caption, final boolean hidden) {
    this(propertyID, propertyType, caption, hidden, false);
  }

  public Property(final String propertyID, final Type propertyType, final String caption, final boolean hidden,
                  final boolean selectOnly) {
    this(propertyID, propertyType, caption, hidden, selectOnly, Constants.INT_NULL_VALUE);
  }

  public Property(final String propertyID, final Type propertyType, final String caption, final boolean hidden,
                  final boolean selectOnly, final int preferredColumnWidth) {
    this(propertyID, propertyType, caption, hidden, selectOnly, preferredColumnWidth, !selectOnly);
  }

  private Property(final String propertyID, final Type propertyType, final String caption, final boolean hidden,
                   final boolean selectOnly, final int preferredColumnWidth, final boolean isUpdatable) {
    if (propertyID == null)
      throw new IllegalArgumentException("Property ID must be specified");
    this.propertyID = propertyID;
    this.propertyType = propertyType;
    this.caption = caption;
    this.hidden = hidden;
    this.preferredColumnWidth = preferredColumnWidth;
    this.selectOnly = selectOnly;
    this.isUpdatable = isUpdatable;
    this.hashCode = initHashCode(propertyID);
  }

  /**
   * @return a String representation of this property
   */
  public String toString() {
    return caption != null ? caption : propertyID;
  }

  /**
   * @return the columnName/property identifier of this property
   */
  public String getColumnName() {
    return this.propertyID;
  }

  /**
   * @return true if this property is updatable
   */
  public boolean isUpdatable() {
    return this.isUpdatable;
  }

  /**
   * @return true if this property is for select only
   */
  public boolean isSelectOnly() {
    return this.selectOnly;
  }

  /**
   * @return the default value for this property
   */
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  /**
   * Sets the default value for this property
   * @param defaultValue the value to use as default
   * @return the property
   */
  public Property setDefaultValue(final Object defaultValue) {
    this.defaultValue = defaultValue;

    return this;
  }

  /**
   * @return true if this property has a parent property
   */
  public boolean hasParentProperty() {
    return this.parentProperty != null;
  }

  /**
   * Sets the parent property
   * @param parentProperty the property to set as parent property
   */
  public void setParentProperty(final EntityProperty parentProperty) {
    this.parentProperty = parentProperty;
  }

  /**
   * @return the caption used when the value of this property is presented
   */
  public String getCaption() {
    return caption;
  }

  /**
   * @return the data type of the value of this property 
   */
  public Type getPropertyType() {
    return propertyType;
  }

  /**
   * @return the preferred column width of this property when
   * presented in a table
   */
  public int getPreferredColumnWidth() {
    return preferredColumnWidth;
  }

  /**
   * Sets the select column index
   * @param index the index
   */
  public void setSelectIndex(final int index) {
    this.selectIndex = index;
  }

  /**
   * @return the select column index
   */
  public int getSelectIndex() {
    return selectIndex;
  }

  /**
   * @return true if this property maps to a database column
   */
  public boolean isDatabaseProperty() {
    return !(this instanceof Property.NonDbProperty);
  }

  /**
   * @return true if this property should be hidden in table views
   */
  public boolean isHidden() {
    return hidden;
  }

  /**
   * @param object the object to compare with
   * @return true if object is a Property instance and has a
   * property identifier equal to that of this property
   */
  public boolean equals(final Object object) {
    return this == object || object instanceof Property && this.propertyID.equals(((Property) object).propertyID);
  }

  /**
   * @return the hash code of the property identifier/column name of this property
   */
  public int hashCode() {
    return hashCode;
  }

  /**
   * Calculates the hash code for this property instance
   * @param propertyID the property identifier
   * @return the hash code of this property, by default the
   * hash code of the property identifier
   */
  protected int initHashCode(final String propertyID) {
    return propertyID.hashCode();
  }

  /**
   * Performes a basic data validation of <code>value</code>, checking if the
   * <code>value</code> data type is consistent with the data type of this
   * property, returns the value
   *
   * @param value the value to validate
   * @param property the property
   * @return the value
   * @throws IllegalArgumentException when the value is not of the same type as the propertyValue
   */
  public static Object validateValue(final Property property, final Object value) throws IllegalArgumentException {
    final Type propertyType = property.propertyType;
    if (value == null)
      return value;

    final String propertyID = property.propertyID;
    switch (propertyType) {
      case INT : {
        if (!(value instanceof Integer))
          throw new IllegalArgumentException("Integer value expected for property: " + propertyID + " (" + value.getClass() + ")");
        return value;
      }
      case DOUBLE : {
        if (!(value instanceof Double))
          throw new IllegalArgumentException("Double value expected for property: " + propertyID + " (" + value.getClass() + ")");
        return value;
      }
      case BOOLEAN : {
        if (!(value instanceof Type.Boolean))
          throw new IllegalArgumentException("Boolean value expected for property: " + propertyID + " (" + value.getClass() + ")");
        return value;
      }
      case LONG_DATE :
      case SHORT_DATE : {
        if (!(value instanceof java.util.Date))
          throw new IllegalArgumentException("Date value expected for property: " + propertyID + " (" + value.getClass() + ")");
        return value;
      }
      case ENTITY : {
        if (!(value instanceof Entity) && !(value instanceof EntityKey))
          throw new IllegalArgumentException("Entity or EntityKey value expected for property: " + propertyID + "(" + value.getClass() + ")");
        return value;
      }
      case CHAR : {
        if (!(value instanceof Character))
          throw new IllegalArgumentException("Character value expected for property: " + propertyID + "(" + value.getClass() + ")");
        return value;
      }
      case STRING : {
        if (!(value instanceof String))
          throw new IllegalArgumentException("String value expected for propertyValue: " + propertyID + "(" + value.getClass() + ")");
        return value;
      }
    }

    throw new IllegalArgumentException("Unknown type " + propertyType);
  }

  /**
   * A property that is part of a entities primary key
   */
  public static class PrimaryKeyProperty extends Property {

    public final int primaryKeyIndex;

    public PrimaryKeyProperty(final String propertyID) {
      this(propertyID, Type.INT);
    }

    public PrimaryKeyProperty(final String propertyID, final Type propertyType) {
      this(propertyID, propertyType, null);
    }

    public PrimaryKeyProperty(final String propertyID, final Type propertyType, final String caption) {
      this(propertyID, propertyType, caption, 0);
    }

    public PrimaryKeyProperty(final String propertyID, final Type propertyType,
                              final String caption, final int primaryKeyIndex) {
      this(propertyID, propertyType, caption, primaryKeyIndex, -1);
    }

    public PrimaryKeyProperty(final String propertyID, final Type propertyType,
                              final String caption, final int primaryKeyIndex,
                              final int preferredWidth) {
      super(propertyID, propertyType, caption, caption == null || caption.length() == 0,
              true, preferredWidth, false);
      if (primaryKeyIndex < 0)
        throw new IllegalArgumentException("Primary key index must be at least 0");

      this.primaryKeyIndex = primaryKeyIndex;
    }
  }

  /**
   * Represents a foreign key reference to a defined entity
   */
  public static class EntityProperty extends Property {

    public final String referenceEntityID;

    public final List<Property> referenceProperties;
    public final boolean isWeakReference;

    public EntityProperty(final String propertyName, final String caption, final String referenceEntityID,
                          final Property... referenceProperties) {
      this(propertyName, caption, referenceEntityID, -1, referenceProperties);
    }

    public EntityProperty(final String propertyName, final String caption, final String referenceEntityID,
                          final int preferredWidth, final Property... referenceProperties) {
      this(propertyName, caption, referenceEntityID, preferredWidth, false, referenceProperties);
    }

    public EntityProperty(final String propertyName, final String caption, final String referenceEntityID,
                          final int preferredWidth, final boolean isWeakReference,
                          final Property... referenceProperties) {
      super(propertyName, Type.ENTITY, caption, false, false, preferredWidth);
      for (final Property referenceProperty : referenceProperties)
        if (referenceProperty.propertyID.equals(propertyName))
          throw new IllegalArgumentException(referenceEntityID + ", reference property does not have a unique name: " + propertyName);
      if (referenceEntityID == null)
        throw new IllegalArgumentException("entityID is null: " + propertyName);

      for (final Property referenceProperty : referenceProperties)
        referenceProperty.setParentProperty(this);
      this.referenceEntityID = referenceEntityID;
      this.referenceProperties = Arrays.asList(referenceProperties);
      this.isWeakReference = isWeakReference;
    }

    /** {@inheritDoc} */
    protected int initHashCode(final String propertyID) {
      int ret = super.initHashCode(propertyID);
      if (referenceProperties != null)
        for (final Property property : referenceProperties)
          ret += property.hashCode();

      return ret;
    }

    /**
     * @return Value for property 'multiColumnReference'.
     */
    public boolean isMultiColumnReference() {
      return this.referenceProperties.size() > 1;
    }
  }

  /**
   * Represents a child entity property that is already included as part of another reference entity property,
   * and should not handle updating the underlying property
   */
  //todo better explanation
  public static class MirrorProperty extends Property {

    public MirrorProperty(final String propertyName) {
      super(propertyName);
    }
  }

  /**
   * A property that gets its value from a entity property, when that property is updated
   */
  public static class DenormalizedProperty extends Property {

    public final String ownerEntityID;
    public final String denormalizedPropertyName;

    public DenormalizedProperty(final String propertyName, final String ownerEntityID,
                                final String property) {
      this(propertyName, ownerEntityID, property, null);
    }

    public DenormalizedProperty(final String propertyName, final String ownerEntityID,
                                final String property, final String caption) {
      this(propertyName, ownerEntityID, property, caption, -1);
    }

    public DenormalizedProperty(final String propertyName, final String ownerEntityID,
                                final String property, final String caption, final int preferredWidth) {
      super(propertyName, Entity.repository.getProperty(ownerEntityID, property).propertyType, caption,
              caption == null, false, preferredWidth, true);
      this.ownerEntityID = ownerEntityID;
      this.denormalizedPropertyName = property;
    }
  }

  /**
   * A property that does not map to a underlying database column
   */
  public static class NonDbProperty extends Property {

    public NonDbProperty(final String propertyName, final Type type) {
      this(propertyName, type, null);
    }

    public NonDbProperty(final String propertyName, final Type type, final String caption) {
      this(propertyName, type, caption, -1);
    }

    public NonDbProperty(final String propertyName, final Type type, final String caption,
                         final int preferredWidth) {
      super(propertyName, type, caption, caption == null, false, preferredWidth, false);
    }

    /** {@inheritDoc} */
    public int getSelectIndex() {
      throw new RuntimeException("Non-db  properties do not have select indexes");
    }
  }

  /**
   * A property that gets its value from a reference entity, but is for
   * display only, and does not map to a database column
   */
  public static class DenormalizedViewProperty extends NonDbProperty {

    public final String ownerEntityID;
    public final String denormalizedPropertyName;

    public DenormalizedViewProperty(final String propertyName, final String ownerEntityID,
                                final String property) {
      this(propertyName, ownerEntityID, property, null);
    }

    public DenormalizedViewProperty(final String propertyName, final String ownerEntityID,
                                final String property, final String caption) {
      this(propertyName, ownerEntityID, property, caption, -1);
    }

    public DenormalizedViewProperty(final String propertyName, final String ownerEntityID,
                                final String property, final String caption, final int preferredWidth) {
      super(propertyName, Entity.repository.getProperty(ownerEntityID, property).propertyType, caption, preferredWidth);
      this.ownerEntityID = ownerEntityID;
      this.denormalizedPropertyName = property;
    }
  }

  /**
   * A sub query property
   */
  public static class SubQueryProperty extends Property {

    private final String subquery;

    public SubQueryProperty(final String propertyName, final Type type, final boolean hidden,
                            final String caption, final String subquery) {
      super(propertyName, type, caption, hidden || caption == null, true, -1, false);
      this.subquery = subquery;
    }

    /**
     * @return Value for property 'subQuery'.
     */
    public String getSubQuery() {
      return subquery;
    }
  }

  /**
   * A boolean property, with special handling since different values
   * are used for representing boolean values in different systems
   */
  public static class BooleanProperty extends Property {

    private final Type columnType;

    private final Object trueValue;
    private final Object falseValue;
    private final Object nullValue;

    public BooleanProperty(final String propertyName, final String caption) {
      this(propertyName, Type.INT, caption);
    }

    public BooleanProperty(final String propertyName, final Type columnType, final String caption) {
      this(propertyName, columnType, caption, FrameworkSettings.get().sqlBooleanValueTrue,
              FrameworkSettings.get().sqlBooleanValueFalse);
    }

    public BooleanProperty(final String propertyName, final Type columnType, final String caption,
                           final Object trueValue, final Object falseValue) {
      this(propertyName, columnType, caption, trueValue, falseValue, FrameworkSettings.get().sqlBooleanValueNull);
    }

    public BooleanProperty(final String propertyName, final Type columnType, final String caption,
                           final Object trueValue, final Object falseValue, final Object nullValue) {
      super(propertyName, Type.BOOLEAN, caption, caption == null);
      this.columnType = columnType;
      this.trueValue = trueValue;
      this.falseValue = falseValue;
      this.nullValue = nullValue;
    }

    /**
     * @return Value for property 'columnType'.
     */
    public Type getColumnType() {
      return columnType;
    }

    /**
     * @return Value for property 'falseValue'.
     */
    public Object getFalseValue() {
      return falseValue;
    }

    /**
     * @return Value for property 'nullValue'.
     */
    public Object getNullValue() {
      return nullValue;
    }

    /**
     * @return Value for property 'trueValue'.
     */
    public Object getTrueValue() {
      return trueValue;
    }

    public Type.Boolean toBoolean(final Object object) {
      if (Util.equal(object, nullValue))
        return null;
      else if (object.equals(trueValue))
        return Type.Boolean.TRUE;
      else if (object.equals(falseValue))
        return Type.Boolean.FALSE;
      else
        return Type.Boolean.NULL;
    }

    public String toSQLString(final Type.Boolean value) {
      final Object ret = value == Type.Boolean.FALSE ? falseValue : (value == Type.Boolean.TRUE ? trueValue : nullValue);
      if (columnType == Type.STRING)
        return "'" + ret + "'";
      else
        return ret == null ? "null" : ret.toString();
    }
  }
}
