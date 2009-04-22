/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

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
  private final Integer preferredColumnWidth;

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
   * Cached select column index
   */
  public int selectIndex = -1;

  /**
   * Instantiates a new property of the type Type.INT
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   */
  public Property(final String propertyID) {
    this(propertyID, Type.INT);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param propertyType the datatype of this property
   */
  public Property(final String propertyID, final Type propertyType) {
    this(propertyID, propertyType, null);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param propertyType the datatype of this property
   * @param caption the caption of this property
   */
  public Property(final String propertyID, final Type propertyType, final String caption) {
    this(propertyID, propertyType, caption, caption == null);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param propertyType the datatype of this property
   * @param caption the caption of this property
   * @param hidden indicates that this property should not be visible to the user
   */
  public Property(final String propertyID, final Type propertyType, final String caption, final boolean hidden) {
    this(propertyID, propertyType, caption, hidden, false);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param propertyType the datatype of this property
   * @param caption the caption of this property
   * @param hidden indicates that this property should not be visible to the user
   * @param selectOnly if true then this property is not included during insert/update operations
   */
  public Property(final String propertyID, final Type propertyType, final String caption, final boolean hidden,
                  final boolean selectOnly) {
    this(propertyID, propertyType, caption, hidden, selectOnly, null);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param propertyType the datatype of this property
   * @param caption the caption of this property
   * @param hidden indicates whether this property should be visible to the user
   * @param selectOnly if true then this property is not included during insert/update operations
   * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
   */
  public Property(final String propertyID, final Type propertyType, final String caption, final boolean hidden,
                  final boolean selectOnly, final Integer preferredColumnWidth) {
    this(propertyID, propertyType, caption, hidden, selectOnly, preferredColumnWidth, !selectOnly);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param propertyType the datatype of this property
   * @param caption the caption of this property
   * @param hidden specifies whether this property should not be visible to the user
   * @param selectOnly specifies whether this property should be included during insert/update operations
   * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
   * @param isUpdatable specifies whether this property is updatable
   */
  public Property(final String propertyID, final Type propertyType, final String caption, final boolean hidden,
                  final boolean selectOnly, final Integer preferredColumnWidth, final boolean isUpdatable) {
    if (propertyID == null)
      throw new IllegalArgumentException("Property ID must be specified");
    this.propertyID = propertyID;
    this.propertyType = propertyType;
    this.caption = caption;
    this.hidden = hidden;
    this.preferredColumnWidth = preferredColumnWidth;
    this.selectOnly = selectOnly;
    this.isUpdatable = isUpdatable;
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
    if (caption == null && hasParentProperty())
      return parentProperty.getCaption();

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
   * presented in a table, null if none has been specified
   */
  public Integer getPreferredColumnWidth() {
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
   * @return true if this property maps to a database column
   */
  public boolean isDatabaseProperty() {
    return !(this instanceof TransientProperty);
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
   * A property that is part of a entities primary key
   */
  public static class PrimaryKeyProperty extends Property {

    /**
     * This property's index in the primary key
     */
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
   * A meta property that represents a reference to another entity, typically but not necessarily based on a foreign key.
   * These do not map directly to a underlying table column, but wrap the actual column properties involved in the relation.
   * e.g.: new Property.EntityProperty("reference_property", new Property("reference_id")), where "reference_id" is the
   * actual name of the column involved in the reference, but "reference_property" is simply a descriptive property ID
   */
  public static class EntityProperty extends Property {

    /**
     * the ID of the referenced entity
     */
    public final String referenceEntityID;
    /**
     * the actual reference properties
     */
    public final List<Property> referenceProperties;
    /**
     * if true the referenced entity is automatically loaded with the parent entity,
     * otherwise a shallow entity instance with only the primary key is loaded
     */
    public final boolean isWeakReference;
    /**
     * this is a hint specifying whether this reference entity is suitible for simple lookup operations,
     * such as in combo boxes. Entities based on large data sets, which are not suitible for loading into
     * lists or combo boxes should have this attribute set to false
     */
    public boolean lookup = true;

    /**
     * @param propertyID the property ID, since EntityProperties are meta properties, the property ID should not
     * be a underlying table column, it must only be unique for this entity
     * @param caption the property caption
     * @param referenceEntityID the ID of the referenced entity type
     * @param referenceProperties the actual column properties involved in the reference
     */
    public EntityProperty(final String propertyID, final String caption, final String referenceEntityID,
                          final Property... referenceProperties) {
      this(propertyID, caption, referenceEntityID, -1, referenceProperties);
    }

    /**
     * @param propertyID the property ID, since EntityProperties are meta properties, the property ID should not
     * be a underlying table column, it must only be unique for this entity
     * @param caption the property caption
     * @param referenceEntityID the ID of the referenced entity type
     * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
     * @param referenceProperties the actual column properties involved in the reference
     */
    public EntityProperty(final String propertyID, final String caption, final String referenceEntityID,
                          final int preferredColumnWidth, final Property... referenceProperties) {
      this(propertyID, caption, referenceEntityID, preferredColumnWidth, false, referenceProperties);
    }

    /**
     * @param propertyID the property ID, since EntityProperties are meta properties, the property ID should not
     * be a underlying table column, it must only be unique for this entity
     * @param caption the property caption
     * @param referenceEntityID the ID of the referenced entity type
     * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
     * @param isWeakReference if true then the actual values of this reference property are not automatically loaded
     * @param referenceProperties the actual column properties involved in the reference
     */
    public EntityProperty(final String propertyID, final String caption, final String referenceEntityID,
                          final int preferredColumnWidth, final boolean isWeakReference,
                          final Property... referenceProperties) {
      super(propertyID, Type.ENTITY, caption, caption == null, false, preferredColumnWidth);
      for (final Property referenceProperty : referenceProperties)
        if (referenceProperty.propertyID.equals(propertyID))
          throw new IllegalArgumentException(referenceEntityID + ", reference property does not have a unique name: " + propertyID);
      if (referenceEntityID == null)
        throw new IllegalArgumentException("referenceEntityID is null: " + propertyID);

      for (final Property referenceProperty : referenceProperties)
        referenceProperty.setParentProperty(this);
      this.referenceEntityID = referenceEntityID;
      this.referenceProperties = Arrays.asList(referenceProperties);
      this.isWeakReference = isWeakReference;
    }

    /**
     * @return true if this reference is based on more than on column
     */
    public boolean isMultiColumnReference() {
      return this.referenceProperties.size() > 1;
    }

    /**
     * @return true if this reference entity is suited for simple lookups, as in, if the underlying dataset
     * is sufficiently small for loading into lists or combo boxes
     */
    public boolean isLookup() {
      return lookup;
    }

    /**
     * @param lookup specifies whether the underlying dataset is sufficiently small for usage in lists or combo boxes
     * @return this EntityProperty instance
     */
    public EntityProperty setLookup(final boolean lookup) {
      this.lookup = lookup;
      return this;
    }
  }

  /**
   * Represents a child entity property that is already included as part of another reference entity property,
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

    /**
     * The ID of the reference entity which owns the property, from which this property
     * gets its value
     */
    public final String ownerEntityID;
    /**
     * the property from which this property gets its value
     */
    public final Property valueSourceProperty;

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param ownerEntityID the ID of the referenced entity which owns the value source property
     * @param valueSourceProperty the property from which this property should get its value
     */
    public DenormalizedProperty(final String propertyID, final String ownerEntityID,
                                final Property valueSourceProperty) {
      this(propertyID, ownerEntityID, valueSourceProperty, null);
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param ownerEntityID the ID of the referenced entity which owns the value source property
     * @param valueSourceProperty the property from which this property should get its value
     * @param caption the caption if this property
     */
    public DenormalizedProperty(final String propertyID, final String ownerEntityID,
                                final Property valueSourceProperty, final String caption) {
      this(propertyID, ownerEntityID, valueSourceProperty, caption, -1);
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param ownerEntityID the ID of the referenced entity which owns the value source property
     * @param valueSourceProperty the property from which this property should get its value
     * @param caption the caption if this property
     * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
     */
    public DenormalizedProperty(final String propertyID, final String ownerEntityID, final Property valueSourceProperty,
                                final String caption, final int preferredColumnWidth) {
      super(propertyID, valueSourceProperty.propertyType, caption, caption == null, false, preferredColumnWidth, true);
      this.ownerEntityID = ownerEntityID;
      this.valueSourceProperty = valueSourceProperty;
    }
  }

  /**
   * A property that does not map to an underlying database column, the value must
   * be provided by a EntityProxy, by overriding it's getValue() method
   * @see EntityProxy#setDefaultEntityProxy(EntityProxy)
   * @see EntityProxy#addEntityProxy(String, EntityProxy)
   * @see EntityProxy#getValue(Entity, Property)
   */
  public static class TransientProperty extends Property {

    /**
     * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the datatype of this property
     */
    public TransientProperty(final String propertyID, final Type type) {
      this(propertyID, type, null);
    }

    /**
     * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the datatype of this property
     * @param caption the caption of this property
     */
    public TransientProperty(final String propertyID, final Type type, final String caption) {
      this(propertyID, type, caption, -1);
    }

    /**
     * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the datatype of this property
     * @param caption the caption of this property
     * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
     */
    public TransientProperty(final String propertyID, final Type type, final String caption,
                             final int preferredColumnWidth) {
      super(propertyID, type, caption, caption == null, false, preferredColumnWidth, false);
    }
  }

  /**
   * A property that gets its value from a reference entity, but is for
   * display only, and does not map to a database column
   */
  public static class DenormalizedViewProperty extends TransientProperty {
    /**
     * the reference property (entity) from which this property should retrieve its value
     */
    public final String referencePropertyID;
    /**
     * the property from which this property should get its value
     */
    public final Property denormalizedProperty;

    /**
     * @param propertyID the ID of the property, this should not be a column name since this property does not
     * map to a table column
     * @param referencePropertyID the ID of the reference property from which entity value this property gets its value
     * @param property the property from which this property gets its value
     */
    public DenormalizedViewProperty(final String propertyID, final String referencePropertyID, final Property property) {
      this(propertyID, referencePropertyID, property, null);
    }

    /**
     * @param propertyID the ID of the property, this should not be a column name since this property does not
     * map to a table column
     * @param referencePropertyID the ID of the reference property from which entity value this property gets its value
     * @param property the property from which this property gets its value
     * @param caption the caption of this property
     */
    public DenormalizedViewProperty(final String propertyID, final String referencePropertyID, final Property property,
                                    final String caption) {
      this(propertyID, referencePropertyID, property, caption, -1);
    }

    /**
     * @param propertyID the ID of the property, this should not be a column name since this property does not
     * map to a table column
     * @param referencePropertyID the ID of the reference property from which entity value this property gets its value
     * @param property the property from which this property gets its value
     * @param caption the caption of this property
     * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
     */
    public DenormalizedViewProperty(final String propertyID, final String referencePropertyID, final Property property,
                                    final String caption, final int preferredColumnWidth) {
      super(propertyID, property.propertyType, caption, preferredColumnWidth);
      this.referencePropertyID = referencePropertyID;
      this.denormalizedProperty = property;
    }
  }

  /**
   * A property based on a subquery, returning a single value
   */
  public static class SubQueryProperty extends Property {
    /**
     * the sql query string
     */
    private final String subquery;

    /**
     * @param propertyID the property ID, since SubQueryProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the datatype of this property
     * @param hidden indicates whether this property should be visible to the user
     * @param caption the caption of this property
     * @param subquery the sql query
     */
    public SubQueryProperty(final String propertyID, final Type type, final boolean hidden,
                            final String caption, final String subquery) {
      super(propertyID, type, caption, hidden || caption == null, true, -1, false);
      this.subquery = subquery;
    }

    /**
     * @return the subquery string
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
    /**
     * the datatype of the underlying column
     */
    public final Type columnType;

    /**
     * the Object value representing true
     */
    private final Object trueValue;
    /**
     * the Object value representing false
     */
    private final Object falseValue;
    /**
     * the Object value representing null
     */
    private final Object nullValue;

    /**
     * for quick comparison of 'true' values
     */
    private final int trueValueHash;

    /**
     * for quick comparison of 'false' values
     */
    private final int falseValueHash;

    /**
     * Instantiates a BooleaProperty based on the INT datatype
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param caption the caption of this property
     */
    public BooleanProperty(final String propertyID, final String caption) {
      this(propertyID, Type.INT, caption);
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param columnType the datatype of the underlying column
     * @param caption the caption of this property
     */
    public BooleanProperty(final String propertyID, final Type columnType, final String caption) {
      this(propertyID, columnType, caption, FrameworkSettings.get().getProperty(FrameworkSettings.SQL_BOOLEAN_VALUE_TRUE),
              FrameworkSettings.get().getProperty(FrameworkSettings.SQL_BOOLEAN_VALUE_FALSE));
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param columnType the datatype of the underlying column
     * @param caption the caption of this property
     * @param trueValue the Object value representing 'true'
     * @param falseValue the Object value representing 'false'
     */
    public BooleanProperty(final String propertyID, final Type columnType, final String caption,
                           final Object trueValue, final Object falseValue) {
      this(propertyID, columnType, caption, trueValue, falseValue,
              FrameworkSettings.get().getProperty(FrameworkSettings.SQL_BOOLEAN_VALUE_NULL));
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param columnType the datatype of the underlying column
     * @param caption the caption of this property
     * @param trueValue the Object value representing 'true'
     * @param falseValue the Object value representing 'false'
     * @param nullValue the Object value representing 'null'
     */
    public BooleanProperty(final String propertyID, final Type columnType, final String caption,
                           final Object trueValue, final Object falseValue, final Object nullValue) {
      super(propertyID, Type.BOOLEAN, caption, caption == null);
      this.columnType = columnType;
      this.nullValue = nullValue;
      this.trueValue = trueValue;
      this.falseValue = falseValue;
      this.trueValueHash = trueValue.hashCode();
      this.falseValueHash = falseValue.hashCode();
    }

    /**
     * @param object the Object value to translate into a Type.Boolean value
     * @return the Type.Boolean value of <code>object</code>
     */
    public Type.Boolean toBoolean(final Object object) {
      final int hashCode = object == null ? 0 : object.hashCode();
      if (hashCode == trueValueHash)
        return Type.Boolean.TRUE;
      else if (hashCode == falseValueHash)
        return Type.Boolean.FALSE;

      return null;
    }

    /**
     * @param value the Type.Boolean value to translate into a sql string value
     * @return the sql string value of <code>value</code>
     */
    public String toSQLString(final Type.Boolean value) {
      final Object ret = value == Type.Boolean.FALSE ? falseValue : (value == Type.Boolean.TRUE ? trueValue : nullValue);
      if (columnType == Type.STRING)
        return "'" + ret + "'";
      else
        return ret == null ? "null" : ret.toString();
    }
  }

  public static class BlobProperty extends Property {

    private final String blobColumnName;

    public BlobProperty(final String propertyID, final String blobColumnName, final String caption) {
      super(propertyID, Type.STRING, caption, false, true, -1, true);
      this.blobColumnName = blobColumnName;
    }

    public String getBlobColumnName() {
      return blobColumnName;
    }
  }
}
