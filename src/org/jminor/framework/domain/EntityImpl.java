/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.common.model.valuemap.ValueChangeMapImpl;
import org.jminor.common.model.valuemap.ValueMap;
import org.jminor.common.model.valuemap.ValueMapImpl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.Format;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.Color;

/**
 * Represents a row in a database table, providing access to the column values via the ValueMap interface.
 */
final class EntityImpl extends ValueChangeMapImpl<String, Object> implements Entity, Serializable, Comparable<Entity> {

  private static final long serialVersionUID = 1;

  private static final String PROPERTY_PARAM = "property";
  private static final String PROPERTY_ID_PARAM = "propertyID";

  /**
   * Used to cache the return value of the frequently called toString(),
   * invalidated each time a property value changes
   */
  private String toString;

  /**
   * Caches the result of <code>getReferencedPrimaryKey</code> method
   */
  private Map<Property.ForeignKeyProperty, Key> referencedPrimaryKeysCache;

  /**
   * Keep a reference to this frequently referenced map
   */
  private EntityDefinition definition;

  /**
   * The primary key of this entity
   */
  private Key primaryKey;

  /**
   * Instantiates a new Entity
   * @param definition the definition of the entity type
   */
  EntityImpl(final EntityDefinition definition) {
    this.definition = definition;
  }

  /**
   * Instantiates a new Entity
   * @param definition the definition of the entity type
   * @param primaryKey the primary key
   */
  EntityImpl(final EntityDefinition definition, final Key primaryKey) {
    this(definition);
    Util.rejectNullValue(primaryKey, "primaryKey");
    for (final Property.PrimaryKeyProperty property : primaryKey.getProperties()) {
      setValue(property, primaryKey.getValue(property.getPropertyID()));
    }
    this.primaryKey = primaryKey;
  }

  /** {@inheritDoc} */
  public String getEntityID() {
    return definition.getEntityID();
  }

  /** {@inheritDoc} */
  public Key getPrimaryKey() {
    if (primaryKey == null) {
      primaryKey = new KeyImpl(definition);
      for (final Property.PrimaryKeyProperty property : definition.getPrimaryKeyProperties()) {
        primaryKey.setValue(property.getPropertyID(), getValue(property));
      }
    }

    return primaryKey;
  }

  /** {@inheritDoc} */
  public Key getOriginalPrimaryKey() {
    final Key key = new KeyImpl(definition);
    for (final Property.PrimaryKeyProperty property : definition.getPrimaryKeyProperties()) {
      key.setValue(property.getPropertyID(), getOriginalValue(property.getPropertyID()));
    }

    return key;
  }

  /** {@inheritDoc} */
  public boolean is(final String entityID) {
    return definition.getEntityID().equals(entityID);
  }

  /** {@inheritDoc} */
  public Property getProperty(final String propertyID) {
    Util.rejectNullValue(propertyID, PROPERTY_ID_PARAM);
    final Property property = definition.getProperties().get(propertyID);
    if (property == null) {
      throw new IllegalArgumentException("Property " + propertyID + " not found in entity: " + definition.getEntityID());
    }

    return property;
  }

  /**
   * Returns true if one or more writable properties have been modified,
   * read only and non-updatable properties are excluded unless they
   * are transient.
   * @return true if one or more properties have been modified
   * since the entity was instantiated
   */
  @Override
  public boolean isModified() {
    return writablePropertiesModified();
  }

  /** {@inheritDoc} */
  @Override
  public Object setValue(final String key, final Object value) {
    return setValue(getProperty(key), value);
  }

  /** {@inheritDoc} */
  public Object setValue(final Property property, final Object value) {
    return setValue(property, value, true, EntityDefinitionImpl.getEntityDefinitionMap());
  }

  /**
   * Initializes the given value assuming it has no previously set value.
   * This method does not propagate foreign key values but does set denormalized values if any exist.
   * This method should be used with care, if at all.
   * @param key the ID of the property for which to initialize the value
   * @param value the value
   */
  @Override
  public void initializeValue(final String key, final Object value) {
    initializeValue(getProperty(key), value);
  }

  /** {@inheritDoc} */
  public void initializeValue(final Property property, final Object value) {
    Util.rejectNullValue(property, PROPERTY_PARAM);
    super.initializeValue(property.getPropertyID(), value);
  }

  /**
   * @param key the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>
   */
  @Override
  public Object getValue(final String key) {
    return getValue(getProperty(key));
  }

  /** {@inheritDoc} */
  public Object getValue(final Property property) {
    Util.rejectNullValue(property, PROPERTY_PARAM);
    if (property instanceof Property.DenormalizedViewProperty) {
      return getDenormalizedViewValue((Property.DenormalizedViewProperty) property);
    }
    if (property instanceof Property.DerivedProperty) {
      return definition.getDerivedValue(this, (Property.DerivedProperty) property);
    }

    return super.getValue(property.getPropertyID());
  }

  /** {@inheritDoc} */
  @Override
  public void handleValueRemoved(final String key, final Object value) {
    super.handleValueRemoved(key, value);
    final Property property = getProperty(key);
    if (property instanceof Property.ForeignKeyProperty) {
      for (final Property fkProperty : ((Property.ForeignKeyProperty) property).getReferenceProperties()) {
        removeValue(fkProperty.getPropertyID());
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValueNull(final String key) {
    return isValueNull(getProperty(key));
  }

  /** {@inheritDoc} */
  public boolean isValueNull(final Property property) {
    return super.isValueNull(Util.rejectNullValue(property, PROPERTY_PARAM).getPropertyID());
  }

  /** {@inheritDoc} */
  public Entity getForeignKeyValue(final String foreignKeyPropertyID) {
    final Property property = getProperty(foreignKeyPropertyID);
    if (property instanceof Property.ForeignKeyProperty) {
      return (Entity) getValue(property.getPropertyID());
    }

    throw new RuntimeException(foreignKeyPropertyID + " is not a foreign key property");
  }

  /** {@inheritDoc} */
  public boolean isLoaded(final String foreignKeyPropertyID) {
    return !isValueNull(foreignKeyPropertyID);
  }

  /** {@inheritDoc} */
  public Date getDateValue(final String propertyID) {
    return (Date) getValue(propertyID);
  }

  /** {@inheritDoc} */
  public Timestamp getTimestampValue(final String propertyID) {
    return (Timestamp) getValue(propertyID);
  }

  /** {@inheritDoc} */
  public String getStringValue(final String propertyID) {
    return (String) getValue(propertyID);
  }

  /** {@inheritDoc} */
  public Integer getIntValue(final String propertyID) {
    return (Integer) getValue(propertyID);
  }

  /** {@inheritDoc} */
  public Boolean getBooleanValue(final String propertyID) {
    return (Boolean) getValue(propertyID);
  }

  /** {@inheritDoc} */
  public Character getCharValue(final String propertyID) {
    return (Character) getValue(propertyID);
  }

  /** {@inheritDoc} */
  public Double getDoubleValue(final String propertyID) {
    return (Double) getValue(propertyID);
  }

  /** {@inheritDoc} */
  public String getValueAsString(final Property property) {
    if (property instanceof Property.DenormalizedViewProperty) {
      return getDenormalizedViewValueFormatted((Property.DenormalizedViewProperty) property);
    }
    if (property instanceof Property.ValueListProperty) {
      return ((Property.ValueListProperty) property).getCaption(getValue(property));
    }

    return getFormattedValue(property);
  }

  /** {@inheritDoc} */
  public String getFormattedValue(final String propertyID) {
    return getFormattedValue(getProperty(propertyID));
  }

  /** {@inheritDoc} */
  public String getFormattedValue(final String propertyID, final Format format) {
    return getFormattedValue(getProperty(propertyID), format);
  }

  /** {@inheritDoc} */
  public String getFormattedValue(final Property property) {
    Util.rejectNullValue(property, PROPERTY_PARAM);
    return getFormattedValue(property, property.getFormat());
  }

  /** {@inheritDoc} */
  public String getFormattedValue(final Property property, final Format format) {
    return definition.getFormattedValue(this, property, format);
  }

  /**
   * @param key the ID of the property for which to retrieve the value
   * @return a String representation of the value of the property identified by <code>propertyID</code>
   * @see #getFormattedValue(Property, java.text.Format)
   */
  @Override
  public String getValueAsString(final String key) {
    return getValueAsString(getProperty(key));
  }

  /** {@inheritDoc} */
  public boolean isNull() {
    return getPrimaryKey().isNull();
  }

  /** {@inheritDoc} */
  public boolean propertyValuesEqual(final Entity entity) {
    Util.rejectNullValue(entity, "entity");
    for (final Property property : definition.getProperties().values()) {
      if (property instanceof Property.ColumnProperty && !Util.equal(getValue(property), entity.getValue(property))) {
        return false;
      }
    }

    return true;
  }

  /**
   * @param obj the object to compare with
   * @return true if the given object is an Entity and it´s primary key is equal to this ones
   */
  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof Entity && ((Entity) obj).getPrimaryKey().equals(getPrimaryKey());
  }

  /**
   * @param o the entity to compare with
   * @return the compare result from comparing <code>entity</code> with this Entity instance
   */
  public int compareTo(final Entity o) {
    return definition.compareTo(this, o);
  }

  /**
   * Returns the hash code of the primary key
   */
  @Override
  public int hashCode() {
    return getPrimaryKey().hashCode();
  }

  /**
   * @return a string representation of this entity
   * @see org.jminor.framework.domain.EntityDefinition#toString(Entity)
   */
  @Override
  public String toString() {
    if (toString == null) {
      toString = definition.toString(this);
    }

    return toString;
  }

  /** {@inheritDoc} */
  public Color getBackgroundColor() {
    return definition.getBackgroundColor(this);
  }

  /**
   * @return a new Entity instance with the same entityID as this entity
   */
  @Override
  public Entity getInstance() {
    return new EntityImpl(definition);
  }

  /** {@inheritDoc} */
  public Key getReferencedPrimaryKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    Key referencedPrimaryKey = getCachedReferenceKey(foreignKeyProperty);
    if (referencedPrimaryKey != null) {
      return referencedPrimaryKey;
    }
    if (foreignKeyProperty.isCompositeReference()) {
      referencedPrimaryKey = initializeCompositeKey(foreignKeyProperty, EntityDefinitionImpl.getEntityDefinitionMap());
    }
    else {
      referencedPrimaryKey = initializeSingleValueKey(foreignKeyProperty, EntityDefinitionImpl.getEntityDefinitionMap());
    }

    cacheReferencedKey(foreignKeyProperty, referencedPrimaryKey);

    return referencedPrimaryKey;
  }

  /** {@inheritDoc} */
  public boolean containsValue(final Property property) {
    return containsValue(Util.rejectNullValue(property, PROPERTY_PARAM).getPropertyID());
  }

  /**
   * Returns true if one or more of the properties involved in the given foreign key is null
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key is null
   */
  public boolean isForeignKeyNull(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    for (final Property property : foreignKeyProperty.getReferenceProperties()) {
      if (isValueNull(property.getPropertyID())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Initializes a new Entity based on the given values.
   * @param definition the entity definition
   * @param values the values
   * @param originalValues the original values
   * @return an initialized Entity
   */
  static Entity entityInstance(final EntityDefinition definition, final Map<String, Object> values, final Map<String, Object> originalValues) {
    Util.rejectNullValue(values, "values");
    final EntityImpl entity = new EntityImpl(definition);
    for (final Map.Entry<String, Object> entry : values.entrySet()) {
      entity.setValue(entry.getKey(), entry.getValue());
    }
    if (originalValues != null) {
      for (final Map.Entry<String, Object> entry : originalValues.entrySet()) {
        entity.setOriginalValue(entry.getKey(), originalValues.get(entry.getKey()));
      }
    }

    return entity;
  }

  /** {@inheritDoc} */
  @Override
  protected void handleClear() {
    super.handleClear();
    primaryKey = null;
  }

  /** {@inheritDoc} */
  @Override
  protected void handleValueChangedEventInitialized() {
    addValueListener(new ValueChangeListener<String, Object>() {
      /** {@inheritDoc} */
      @Override
      protected void valueChanged(final ValueChangeEvent<String, Object> event) {
        final Collection<String> linkedPropertyIDs = definition.getLinkedPropertyIDs(event.getKey());
        for (final String propertyID : linkedPropertyIDs) {
          final Object linkedValue = getValue(propertyID);
          notifyValueChange(propertyID, linkedValue, linkedValue, false);
        }
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  protected void handleSetAs(final ValueMap<String, Object> sourceMap) {
    super.handleSetAs(sourceMap);
    toString = null;
    if (sourceMap instanceof Entity) {
      toString = sourceMap.toString();
    }
  }

  private void propagateForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity newValue,
                                         final Collection<Property.PrimaryKeyProperty> referenceEntityPKProperties,
                                         final boolean initialization, final Map<String, EntityDefinition> entityDefinitions) {
    setForeignKeyValues(foreignKeyProperty, newValue, referenceEntityPKProperties, initialization, entityDefinitions);
    if (definition.hasDenormalizedProperties()) {
      setDenormalizedValues(foreignKeyProperty, newValue, initialization, entityDefinitions);
    }
  }

  /**
   * Sets the values of the properties used in the reference to the corresponding values found in <code>referencedEntity</code>.
   * Example: EntityOne references EntityTwo via entityTwoID, after a call to this method the EntityOne.entityTwoID
   * property has the value of EntityTwo's primary key property. If <code>referencedEntity</code> is null then
   * the corresponding reference values are set to null.
   * @param foreignKeyProperty the entity reference property
   * @param referencedEntity the referenced entity
   * @param referenceEntityPKProperties the referenced primary key properties
   * @param initialization true if the values are being initialized
   * @param entityDefinitions a global entity definition map
   */
  private void setForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity,
                                   final Collection<Property.PrimaryKeyProperty> referenceEntityPKProperties,
                                   final boolean initialization, final Map<String, EntityDefinition> entityDefinitions) {
    referencedPrimaryKeysCache = null;
    for (final Property.PrimaryKeyProperty primaryKeyProperty : referenceEntityPKProperties) {
      final Property referenceProperty = foreignKeyProperty.getReferenceProperties().get(primaryKeyProperty.getIndex());
      if (!(referenceProperty instanceof Property.MirrorProperty)) {
        final Object value;
        if (referencedEntity == null) {
          value = null;
        }
        else {
          value = referencedEntity.getValue(primaryKeyProperty);
        }
        if (initialization) {
          initializeValue(referenceProperty, value);
        }
        else {
          setValue(referenceProperty, value, false, entityDefinitions);
        }
      }
    }
  }

  /**
   * Sets the denormalized property values
   * @param foreignKeyProperty the foreign key property referring to the value source
   * @param referencedEntity the entity value owning the denormalized values
   * @param initialization true if the values are being initialized
   * @param entityDefinitions a global entity definition map
   */
  private void setDenormalizedValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity,
                                     final boolean initialization, final Map<String, EntityDefinition> entityDefinitions) {
    final Collection<Property.DenormalizedProperty> denormalizedProperties =
            definition.getDenormalizedProperties(foreignKeyProperty.getPropertyID());
    if (denormalizedProperties != null) {
      for (final Property.DenormalizedProperty denormalizedProperty : denormalizedProperties) {
        final Object value;
        if (referencedEntity == null) {
          value = null;
        }
        else {
          value = referencedEntity.getValue(denormalizedProperty.getDenormalizedProperty());
        }
        if (initialization) {
          initializeValue(denormalizedProperty, value);
        }
        else {
          setValue(denormalizedProperty, value, false, entityDefinitions);
        }
      }
    }
  }

  private Object getDenormalizedViewValue(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = (Entity) getValue(denormalizedViewProperty.getForeignKeyPropertyID());
    if (valueOwner == null) {
      return null;
    }

    return valueOwner.getValue(denormalizedViewProperty.getDenormalizedProperty().getPropertyID());
  }

  private String getDenormalizedViewValueFormatted(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = (Entity) getValue(denormalizedViewProperty.getForeignKeyPropertyID());
    if (valueOwner == null) {
      return null;
    }

    return valueOwner.getFormattedValue(denormalizedViewProperty.getDenormalizedProperty());
  }

  private Key initializeSingleValueKey(final Property.ForeignKeyProperty foreignKeyProperty,
                                       final Map<String, EntityDefinition> entityDefinitions) {
    final Property referenceKeyProperty = foreignKeyProperty.getReferenceProperties().get(0);
    final Object value = getValue(referenceKeyProperty);
    if (value == null) {
      return null;
    }

    return new KeyImpl(entityDefinitions.get(foreignKeyProperty.getReferencedEntityID()), value);
  }

  private Key initializeCompositeKey(final Property.ForeignKeyProperty foreignKeyProperty,
                                     final Map<String, EntityDefinition> entityDefinitions) {
    final Key key = new KeyImpl(entityDefinitions.get(foreignKeyProperty.getReferencedEntityID()));
    for (final Property referenceKeyProperty : foreignKeyProperty.getReferenceProperties()) {
      final Object value = getValue(referenceKeyProperty);
      if (value == null) {
        return null;
      }
      else {
        key.setValue(foreignKeyProperty.getReferencedPropertyID(referenceKeyProperty), value);
      }
    }

    return key;
  }

  private boolean writablePropertiesModified() {
    for (final String propertyID : getOriginalValueKeys()) {
      final Property property = getProperty(propertyID);
      if (property instanceof Property.ColumnProperty) {
        final Property.ColumnProperty columnProperty = (Property.ColumnProperty) property;
        if (!columnProperty.isReadOnly() && columnProperty.isUpdatable()) {
          return true;
        }
      }
      if (property instanceof Property.TransientProperty) {
        return true;
      }
    }

    return false;
  }

  private void cacheReferencedKey(final Property.ForeignKeyProperty foreignKeyProperty, final Key referencedPrimaryKey) {
    if (referencedPrimaryKey != null) {
      if (referencedPrimaryKeysCache == null) {
        referencedPrimaryKeysCache = new HashMap<Property.ForeignKeyProperty, Key>();
      }
      referencedPrimaryKeysCache.put(foreignKeyProperty, referencedPrimaryKey);
    }
  }

  private Key getCachedReferenceKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    if (referencedPrimaryKeysCache == null) {
      return null;
    }

    return referencedPrimaryKeysCache.get(foreignKeyProperty);
  }

  /**
   * Sets the property value
   * @param property the property
   * @param value the value
   * @param validateType if true then type validation is performed
   * @param entityDefinitions a global entity definition map
   * @return the old value
   */
  private Object setValue(final Property property, final Object value, final boolean validateType,
                          final Map<String, EntityDefinition> entityDefinitions) {
    Util.rejectNullValue(property, PROPERTY_PARAM);
    if (property instanceof Property.PrimaryKeyProperty) {
      this.primaryKey = null;
    }

    validateValue(this, property, value);
    if (validateType) {
      validateType(property, value);
    }

    toString = null;
    if (property instanceof Property.ForeignKeyProperty && (value == null || value instanceof Entity)) {
      final Property.ForeignKeyProperty fkProperty = (Property.ForeignKeyProperty) property;
      final Collection<Property.PrimaryKeyProperty> referenceEntityPKProperties =
              entityDefinitions.get(fkProperty.getReferencedEntityID()).getPrimaryKeyProperties();
      propagateForeignKeyValues((Property.ForeignKeyProperty) property, (Entity) value,
              referenceEntityPKProperties, false, entityDefinitions);
    }

    return super.setValue(property.getPropertyID(), value);
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getEntityID());
    final boolean isModified = isModified();
    stream.writeBoolean(isModified);
    for (final Property property : definition.getProperties().values()) {
      if (!(property instanceof Property.DerivedProperty) && !(property instanceof Property.DenormalizedViewProperty)) {
        final String propertyID = property.getPropertyID();
        stream.writeObject(super.getValue(propertyID));
        if (isModified) {
          final boolean valueModified = isModified(propertyID);
          stream.writeBoolean(valueModified);
          if (valueModified) {
            stream.writeObject(getOriginalValue(propertyID));
          }
        }
      }
    }
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    final String entityID = (String) stream.readObject();
    final boolean isModified = stream.readBoolean();
    definition = EntityDefinitionImpl.getEntityDefinitionMap().get(entityID);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityID);
    }
    for (final Property property : definition.getProperties().values()) {
      if (!(property instanceof Property.DerivedProperty) && !(property instanceof Property.DenormalizedViewProperty)) {
        final String propertyID = property.getPropertyID();
        super.initializeValue(propertyID, stream.readObject());
        if (isModified) {
          if (stream.readBoolean()) {
            setOriginalValue(propertyID, stream.readObject());
          }
        }
      }
    }
  }

  /**
   * Performs a basic data validation of <code>value</code>, checking if the <code>value</code> data type is
   * consistent with the data type of this property, returns the value.
   * For foreign key properties this method also checks if the value entityID fits the key.
   * @param value the value to validate
   * @param property the property
   * @return the value to validate
   * @throws IllegalArgumentException when the value type does not fit the property type
   */
  private static Object validateType(final Property property, final Object value) {
    if (value == null) {
      return value;
    }

    final Class type = Util.getTypeClass(property.getType());
    if (!type.equals(value.getClass()) && !type.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Value of type " + type + " expected for property " + property + ", got: " + value.getClass());
    }

    return value;
  }

  private static void validateValue(final Entity entity, final Property property, final Object value) {
    if (property instanceof Property.DenormalizedViewProperty) {
      throw new IllegalArgumentException("Can not set the value of a denormalized view property");
    }
    if (property instanceof Property.DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof Property.ValueListProperty && value != null && !((Property.ValueListProperty) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid value list value: " + value + " for property " + property.getPropertyID());
    }
    if (value instanceof Entity && value.equals(entity)) {
      throw new IllegalArgumentException("Circular entity reference detected: " + entity + "->" + property.getPropertyID());
    }
  }

  /**
   * A class representing column key objects for entities, contains the values for those columns.
   */
  static final class KeyImpl extends ValueMapImpl<String, Object> implements Entity.Key, Serializable {

    private static final long serialVersionUID = 1;

    private static final int INTEGER_NULL_VALUE = Integer.MAX_VALUE;

    /**
     * true if this key consists of a single integer value
     */
    private boolean singleIntegerKey;

    /**
     * true if this key consists of multiple properties
     */
    private boolean compositeKey;

    /**
     * Caching the hash code
     */
    private int hashCode = INTEGER_NULL_VALUE;

    /**
     * True if the value of a key property has changed, thereby invalidating the cached hash code value
     */
    private boolean hashCodeDirty = true;

    /**
     * Caching this extremely frequently referenced attribute
     */
    private EntityDefinition definition;

    /**
     * Instantiates a new Key for the given entity type
     * @param definition the entity definition
     */
    KeyImpl(final EntityDefinition definition) {
      this.definition = definition;
      final List<Property.PrimaryKeyProperty> properties = definition.getPrimaryKeyProperties();
      final int propertyCount = properties.size();
      this.singleIntegerKey = propertyCount == 1 && properties.get(0).isInteger();
      this.compositeKey = propertyCount > 1;
    }

    /**
     * Instantiates a new Key for the given entity type, assuming it is a single value key
     * @param definition the entity definition
     * @param value the value
     * @throws RuntimeException in case this key is a multi value key
     */
    KeyImpl(final EntityDefinition definition, final Object value) {
      this(definition);
      if (compositeKey) {
        throw new IllegalArgumentException("Not a single value key");
      }

      final Property property = definition.getPrimaryKeyProperties().get(0);
      setValue(property.getPropertyID(), value);
    }

    /** {@inheritDoc} */
    public String getEntityID() {
      return definition.getEntityID();
    }

    /** {@inheritDoc} */
    public List<Property.PrimaryKeyProperty> getProperties() {
      return definition.getPrimaryKeyProperties();
    }

    /** {@inheritDoc} */
    public Property.PrimaryKeyProperty getFirstKeyProperty() {
      if (getPropertyCount() == 0) {
        throw new IllegalStateException("No properties defined for primary key");
      }

      return getProperties().get(0);
    }

    /** {@inheritDoc} */
    public Object getFirstKeyValue() {
      return getValue(getFirstKeyProperty().getPropertyID());
    }

    /**
     * @return a string representation of this key
     */
    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      int i = 0;
      for (final Property.PrimaryKeyProperty property : getProperties()) {
        stringBuilder.append(property.getPropertyID()).append(":").append(getValue(property.getPropertyID()));
        if (i++ < getPropertyCount() - 1) {
          stringBuilder.append(",");
        }
      }

      return stringBuilder.toString();
    }

    /**
     * @return an Key instance with the entityID of this key
     */
    @Override
    public Key getInstance() {
      return new KeyImpl(definition);
    }

    /** {@inheritDoc} */
    public int getPropertyCount() {
      if (singleIntegerKey || !compositeKey) {
        return 1;
      }

      return getProperties().size();
    }

    /** {@inheritDoc} */
    public boolean isSingleIntegerKey() {
      return singleIntegerKey;
    }

    /** {@inheritDoc} */
    public boolean isCompositeKey() {
      return compositeKey;
    }

    /**
     * Key objects are equal if the entityIDs match as well as all property values.
     * @param obj the object to compare with
     * @return true if object is equal to this key
     */
    @SuppressWarnings({"SimplifiableIfStatement"})
    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof Key) {
        final String entityID = definition.getEntityID();
        final Key key = (Key) obj;
        if (singleIntegerKey) {
          return key.isSingleIntegerKey() && hashCode() == key.hashCode() && entityID.equals(key.getEntityID());
        }
        else {
          return !key.isSingleIntegerKey() && entityID.equals(key.getEntityID()) && super.equals(key);
        }
      }

      return false;
    }

    /**
     * @return a hash code based on the values of this key, for single integer keys
     * the hash code is simply the key value.
     */
    @Override
    public int hashCode() {
      if (hashCodeDirty) {
        final Collection values = getValues();
        boolean nullValue = values.isEmpty();
        int hash = 0;
        if (!nullValue) {
          for (final Object value : values) {
            if (value != null) {
              hash = hash + value.hashCode();
            }
            else {
              nullValue = true;
              break;
            }
          }
        }

        if (nullValue) {
          hashCode = INTEGER_NULL_VALUE;
        }
        else {
          hashCode = hash;
        }
        hashCodeDirty = false;
      }

      return hashCode;
    }

    /** {@inheritDoc} */
    public boolean isNull() {
      if (singleIntegerKey) {
        return hashCode() == INTEGER_NULL_VALUE;
      }

      if (hashCode() == INTEGER_NULL_VALUE) {
        return true;
      }

      for (final Object value : getValues()) {
        if (value == null) {
          return true;
        }
      }

      return false;
    }

    /** {@inheritDoc} */
    @Override
    protected void handleValueSet(final String key, final Object value, final Object previousValue,
                                  final boolean initialization) {
      hashCodeDirty = true;
      if (singleIntegerKey) {
        if (!(value == null || value instanceof Integer)) {
          throw new IllegalArgumentException("Expecting a Integer value for Key: "
                  + definition.getEntityID() + ", "
                  + key + ", got " + value + "; " + value.getClass());
        }
        setHashcode(value);
      }
    }

    /** {@inheritDoc} */
    @Override
    protected void handleClear() {
      hashCode = INTEGER_NULL_VALUE;
      hashCodeDirty = false;
    }

    private void setHashcode(final Object value) {
      if (value == null) {
        hashCode = INTEGER_NULL_VALUE;
      }
      else {
        hashCode = (Integer) value;
      }
      hashCodeDirty = false;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(definition.getEntityID());
      for (final Property property : definition.getPrimaryKeyProperties()) {
        stream.writeObject(getValue(property.getPropertyID()));
      }
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
      final String entityID = (String) stream.readObject();
      definition = EntityDefinitionImpl.getEntityDefinitionMap().get(entityID);
      if (definition == null) {
        throw new IllegalArgumentException("Undefined entity: " + entityID);
      }
      final List<Property.PrimaryKeyProperty> properties = definition.getPrimaryKeyProperties();
      final int propertyCount = properties.size();
      singleIntegerKey = propertyCount == 1 && properties.get(0).isInteger();
      compositeKey = propertyCount > 1;
      for (final Property property : properties) {
        setValue(property.getPropertyID(), stream.readObject());
      }
    }
  }
}
