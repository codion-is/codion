/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.DefaultValueMap;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;

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

/**
 * Represents a row in a database table, providing access to the column values via the {@link org.jminor.common.model.valuemap.ValueMap} interface.
 */
final class EntityImpl extends DefaultValueMap<String, Object> implements Entity, Serializable, Comparable<Entity> {

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
  private Map<String, Key> referencedPrimaryKeysCache;

  /**
   * Keep a reference to this frequently referenced object
   */
  private Definition definition;

  /**
   * The primary key of this entity
   */
  private Key primaryKey;

  /**
   * Instantiates a new EntityImpl
   * @param definition the definition of the entity type
   */
  EntityImpl(final Definition definition) {
    this.definition = definition;
  }

  /**
   * Instantiates a new EntityImpl
   * @param definition the definition of the entity type
   * @param primaryKey the primary key
   */
  EntityImpl(final Definition definition, final Key primaryKey) {
    this(definition);
    Util.rejectNullValue(primaryKey, "primaryKey");
    for (final Property.PrimaryKeyProperty property : primaryKey.getProperties()) {
      setValue(property, primaryKey.getValue(property.getPropertyID()));
    }
    this.primaryKey = primaryKey;
  }

  /**
   * Instantiates a new EntityImpl based on the given values.
   * @param definition the definition of the entity type
   * @param values the values
   * @param originalValues the original values, may be null
   * @return an initialized Entity
   */
  EntityImpl(final Definition definition, final Map<String, Object> values, final Map<String, Object> originalValues) {
    this(definition);
    if (values != null) {
      for (final Map.Entry<String, Object> entry : values.entrySet()) {
        setValue(entry.getKey(), entry.getValue());
      }
    }
    if (originalValues != null) {
      for (final Map.Entry<String, Object> entry : originalValues.entrySet()) {
        setOriginalValue(entry.getKey(), originalValues.get(entry.getKey()));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getEntityID() {
    return definition.getEntityID();
  }

  /** {@inheritDoc} */
  @Override
  public Key getPrimaryKey() {
    if (primaryKey == null) {
      primaryKey = initializePrimaryKey(false);
    }

    return primaryKey;
  }

  /** {@inheritDoc} */
  @Override
  public Key getOriginalPrimaryKey() {
    return initializePrimaryKey(true);
  }

  /** {@inheritDoc} */
  @Override
  public boolean is(final String entityID) {
    return definition.getEntityID().equals(entityID);
  }

  /** {@inheritDoc} */
  @Override
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
    return super.isModified() && writablePropertiesModified();
  }

  /** {@inheritDoc} */
  @Override
  public Object setValue(final String key, final Object value) {
    return setValue(getProperty(key), value);
  }

  /** {@inheritDoc} */
  @Override
  public Object setValue(final Property property, final Object value) {
    return setValue(property, value, true);
  }

  /** {@inheritDoc} */
  @Override
  public Object setValue(final Property property, final Object value, final boolean validateType) {
    return setValue((DefaultProperty) property, value, validateType, DefaultEntityDefinition.getDefinitionMap());
  }

  /**
   * @param key the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>
   */
  @Override
  public Object getValue(final String key) {
    return getValue(getProperty(key));
  }

  /**
   * Returns the value associated with the given property.
   * Foreign key values which have non-null references but have not been loaded are simply returned
   * as null, use {@link #getForeignKeyValue(org.jminor.framework.domain.Property.ForeignKeyProperty)}
   * to get an empty entity instance
   * @param property the property for which to retrieve the value
   * @return the value associated with the given property.
   * @see #getForeignKeyValue(org.jminor.framework.domain.Property.ForeignKeyProperty)
   * @see #isLoaded(String)
   */
  @Override
  public Object getValue(final Property property) {
    Util.rejectNullValue(property, PROPERTY_PARAM);
    if (property instanceof Property.DenormalizedViewProperty) {
      return getDenormalizedViewValue((Property.DenormalizedViewProperty) property);
    }
    if (property instanceof Property.DerivedProperty) {
      return getDerivedValue((Property.DerivedProperty) property);
    }

    return super.getValue(((DefaultProperty) property).propertyID);
  }

  /**
   * Returns true if the value associated with the given property is null.
   * In case of foreign key properties the value of the underlying reference
   * property is checked.
   * @param propertyID the property ID
   * @return true if the value associated with the property is null
   */
  @Override
  public boolean isValueNull(final String propertyID) {
    return isValueNull(getProperty(propertyID));
  }

  /**
   * Returns true if the value associated with the given property is null.
   * In case of foreign key properties the value of the underlying reference
   * property is checked.
   * @param property the property
   * @return true if the value associated with the property is null
   */
  @Override
  public boolean isValueNull(final Property property) {
    Util.rejectNullValue(property, PROPERTY_PARAM);
    if (property instanceof Property.ForeignKeyProperty) {
      return isForeignKeyNull((Property.ForeignKeyProperty) property);
    }

    return super.isValueNull(property.getPropertyID());
  }

  /** {@inheritDoc} */
  @Override
  public Entity getForeignKeyValue(final String foreignKeyPropertyID) {
    final Property property = getProperty(foreignKeyPropertyID);
    if (property instanceof Property.ForeignKeyProperty) {
      return getForeignKeyValue((Property.ForeignKeyProperty) property);
    }

    throw new IllegalArgumentException(foreignKeyPropertyID + " is not a foreign key property");
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLoaded(final String foreignKeyPropertyID) {
    return super.getValue(foreignKeyPropertyID) != null;
  }

  /** {@inheritDoc} */
  @Override
  public Date getDateValue(final String propertyID) {
    return (Date) getValue(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public Timestamp getTimestampValue(final String propertyID) {
    return (Timestamp) getValue(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public String getStringValue(final String propertyID) {
    return (String) getValue(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public Integer getIntValue(final String propertyID) {
    return (Integer) getValue(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public Boolean getBooleanValue(final String propertyID) {
    return (Boolean) getValue(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public Character getCharValue(final String propertyID) {
    return (Character) getValue(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public Double getDoubleValue(final String propertyID) {
    return (Double) getValue(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public String getValueAsString(final Property property) {
    if (property instanceof Property.DenormalizedViewProperty) {
      return getDenormalizedViewValueFormatted((Property.DenormalizedViewProperty) property);
    }
    if (property instanceof Property.ValueListProperty) {
      return ((Property.ValueListProperty) property).getCaption(getValue(property));
    }
    if (property instanceof Property.ForeignKeyProperty && !isLoaded(property.getPropertyID())) {
      final Entity.Key referencedKey = getReferencedPrimaryKey((Property.ForeignKeyProperty) property);
      if (referencedKey != null) {
        return referencedKey.toString();
      }
    }

    return getFormattedValue(property);
  }

  /** {@inheritDoc} */
  @Override
  public String getFormattedValue(final String propertyID) {
    return getFormattedValue(getProperty(propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public String getFormattedValue(final String propertyID, final Format format) {
    return getFormattedValue(getProperty(propertyID), format);
  }

  /** {@inheritDoc} */
  @Override
  public String getFormattedValue(final Property property) {
    Util.rejectNullValue(property, PROPERTY_PARAM);
    return getFormattedValue(property, property.getFormat());
  }

  /** {@inheritDoc} */
  @Override
  public String getFormattedValue(final Property property, final Format format) {
    final Object value = getValue(property);
    if (value == null) {
      return "";
    }

    if (format == null) {
      return value.toString();
    }

    return format.format(value);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return a String representation of the value of the property identified by <code>propertyID</code>
   * @see #getFormattedValue(Property, java.text.Format)
   */
  @Override
  public String getValueAsString(final String propertyID) {
    return getValueAsString(getProperty(propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public boolean isPrimaryKeyNull() {
    return getPrimaryKey().isNull();
  }

  /** {@inheritDoc} */
  @Override
  public void clearPrimaryKeyValues() {
    for (final Property.PrimaryKeyProperty primaryKeyProperty : definition.getPrimaryKeyProperties()) {
      removeValue(primaryKeyProperty.getPropertyID());
      removeOriginalValue(primaryKeyProperty.getPropertyID());
    }
    this.primaryKey = null;
  }

  /** {@inheritDoc} */
  @Override
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
   * @param entity the entity to compare with
   * @return the compare result from comparing <code>entity</code> with this Entity instance
   * @see Definition#setComparator(java.util.Comparator)
   */
  @Override
  public int compareTo(final Entity entity) {
    return definition.compareTo(this, entity);
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
   * @see Definition#setStringProvider(org.jminor.framework.domain.Entity.ToString)
   * @see Definition#toString(Entity)
   */
  @Override
  public String toString() {
    if (toString == null) {
      toString = definition.toString(this);
    }

    return toString;
  }

  /** {@inheritDoc} */
  @Override
  public Object getBackgroundColor(final Property property) {
    return definition.getBackgroundColor(this, property);
  }

  /**
   * @return a new Entity instance with the same entityID as this entity
   */
  @Override
  public Entity getInstance() {
    return new EntityImpl(definition);
  }

  /** {@inheritDoc} */
  @Override
  public Key getReferencedPrimaryKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    final String propertyID = foreignKeyProperty.getPropertyID();
    Key referencedPrimaryKey = getCachedReferencedKey(propertyID);
    if (referencedPrimaryKey != null) {
      return referencedPrimaryKey;
    }

    referencedPrimaryKey = initializeReferencedKey(foreignKeyProperty);
    if (referencedPrimaryKey != null) {
      cacheReferencedKey(propertyID, referencedPrimaryKey);
    }

    return referencedPrimaryKey;
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsValue(final Property property) {
    return containsValue(Util.rejectNullValue(property, PROPERTY_PARAM).getPropertyID());
  }

  /**
   * Returns true if one or more of the properties involved in the given foreign key is null
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key is null
   */
  @Override
  public boolean isForeignKeyNull(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    for (final Property property : foreignKeyProperty.getReferenceProperties()) {
      if (isValueNull(property.getPropertyID())) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  protected Object copyValue(final Object value) {
    if (value instanceof Entity) {
      return ((Entity) value).getCopy();
    }

    return super.copyValue(value);
  }

  /** {@inheritDoc} */
  @Override
  protected void handleClear() {
    primaryKey = null;
    referencedPrimaryKeysCache = null;
    toString = null;
  }

  /** {@inheritDoc} */
  @Override
  protected void handleValueRemoved(final String key, final Object value) {
    final Property property = getProperty(key);
    if (property instanceof Property.ForeignKeyProperty) {
      for (final Property referenceProperty : ((Property.ForeignKeyProperty) property).getReferenceProperties()) {
        removeValue(referenceProperty.getPropertyID());
      }
    }
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

  private void propagateForeignKeyValues(final DefaultProperty.DefaultForeignKeyProperty foreignKeyProperty, final Entity newValue,
                                         final Map<String, Definition> entityDefinitions) {
    setForeignKeyValues(foreignKeyProperty, newValue, entityDefinitions);
    if (definition.hasDenormalizedProperties()) {
      setDenormalizedValues(foreignKeyProperty, newValue, entityDefinitions);
    }
  }

  /**
   * Sets the values of the properties used in the reference to the corresponding values found in <code>referencedEntity</code>.
   * Example: EntityOne references EntityTwo via entityTwoID, after a call to this method the EntityOne.entityTwoID
   * property has the value of EntityTwo's primary key property. If <code>referencedEntity</code> is null then
   * the corresponding reference values are set to null.
   * @param foreignKeyProperty the entity reference property
   * @param referencedEntity the referenced entity
   * @param entityDefinitions a global entity definition map
   */
  private void setForeignKeyValues(final DefaultProperty.DefaultForeignKeyProperty foreignKeyProperty, final Entity referencedEntity,
                                   final Map<String, Definition> entityDefinitions) {
    referencedPrimaryKeysCache = null;
    final Collection<Property.PrimaryKeyProperty> referenceEntityPKProperties =
            entityDefinitions.get(foreignKeyProperty.getReferencedEntityID()).getPrimaryKeyProperties();
    for (final Property.PrimaryKeyProperty primaryKeyProperty : referenceEntityPKProperties) {
      final DefaultProperty referenceProperty = (DefaultProperty) foreignKeyProperty.getReferenceProperties().get(primaryKeyProperty.getIndex());
      if (!(referenceProperty instanceof Property.MirrorProperty)) {
        final Object value;
        if (referencedEntity == null) {
          value = null;
        }
        else {
          value = referencedEntity.getValue(primaryKeyProperty);
        }
        setValue(referenceProperty, value, false, entityDefinitions);
      }
    }
  }

  /**
   * Sets the denormalized property values
   * @param foreignKeyProperty the foreign key property referring to the value source
   * @param referencedEntity the entity value owning the denormalized values
   * @param entityDefinitions a global entity definition map
   */
  private void setDenormalizedValues(final DefaultProperty.DefaultForeignKeyProperty foreignKeyProperty, final Entity referencedEntity,
                                     final Map<String, Definition> entityDefinitions) {
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
        setValue((DefaultProperty) denormalizedProperty, value, false, entityDefinitions);
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

  private Key initializeReferencedKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    if (foreignKeyProperty.isCompositeReference()) {
      final Key key = new KeyImpl(DefaultEntityDefinition.getDefinitionMap().get(foreignKeyProperty.getReferencedEntityID()));
      for (final Property referenceKeyProperty : foreignKeyProperty.getReferenceProperties()) {
        final Object value = super.getValue(referenceKeyProperty.getPropertyID());
        if (value == null) {
          return null;
        }
        else {
          key.setValue(foreignKeyProperty.getReferencedPropertyID(referenceKeyProperty), value);
        }
      }

      return key;
    }
    else {
      final Property referenceKeyProperty = foreignKeyProperty.getReferenceProperties().get(0);
      final Object value = super.getValue(referenceKeyProperty.getPropertyID());
      if (value == null) {
        return null;
      }

      return new KeyImpl(DefaultEntityDefinition.getDefinitionMap().get(foreignKeyProperty.getReferencedEntityID()), value);
    }
  }

  private void cacheReferencedKey(final String fkPropertyID, final Key referencedPrimaryKey) {
    if (referencedPrimaryKeysCache == null) {
      referencedPrimaryKeysCache = new HashMap<String, Key>();
    }
    referencedPrimaryKeysCache.put(fkPropertyID, referencedPrimaryKey);
  }

  private Key getCachedReferencedKey(final String fkPropertyID) {
    if (referencedPrimaryKeysCache == null) {
      return null;
    }

    return referencedPrimaryKeysCache.get(fkPropertyID);
  }

  /**
   * Initializes a Key for this Entity instance
   * @param originalValues if true then the original values of the properties involved are used
   * @return a Key based on the values in this Entity instance
   */
  private Key initializePrimaryKey(final boolean originalValues) {
    final Key key = new KeyImpl(definition);
    for (final Property.PrimaryKeyProperty property : definition.getPrimaryKeyProperties()) {
      final String propertyID = ((DefaultProperty) property).propertyID;
      key.setValue(propertyID, originalValues ? getOriginalValue(propertyID) : super.getValue(propertyID));
    }

    return key;
  }

  private Object getDerivedValue(final Property.DerivedProperty derivedProperty) {
    final Map<String, Object> values = new HashMap<String, Object>(derivedProperty.getLinkedPropertyIDs().size());
    for (final String linkedPropertyID : derivedProperty.getLinkedPropertyIDs()) {
      values.put(linkedPropertyID, getValue(linkedPropertyID));
    }

    return derivedProperty.getValueProvider().getValue(values);
  }

  private Entity getForeignKeyValue(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Entity value = (Entity) super.getValue(foreignKeyProperty.getPropertyID());
    if (value == null) {//possibly not loaded
      final Entity.Key referencedKey = getReferencedPrimaryKey(foreignKeyProperty);
      if (referencedKey != null) {
        return new EntityImpl(DefaultEntityDefinition.getDefinitionMap().get(referencedKey.getEntityID()), referencedKey);
      }
    }

    return value;
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

  /**
   * Sets the property value, propagates foreign key values and rounds floating point values
   * @param property the property
   * @param value the value
   * @param validateType if true then type validation is performed
   * @param entityDefinitions a global entity definition map
   * @return the old value
   */
  private Object setValue(final DefaultProperty property, final Object value, final boolean validateType,
                          final Map<String, Definition> entityDefinitions) {
    Util.rejectNullValue(property, PROPERTY_PARAM);
    if (property instanceof Property.PrimaryKeyProperty) {
      primaryKey = null;
    }
    validateValue(this, property, value);
    if (validateType) {
      validateType(property, value);
    }

    toString = null;
    if (property instanceof Property.ForeignKeyProperty) {
      propagateForeignKeyValues((DefaultProperty.DefaultForeignKeyProperty) property, (Entity) value, entityDefinitions);
    }

    return super.setValue(property.propertyID, property.prepareValue(value));
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getEntityID());
    final boolean isModified = isModified();
    stream.writeBoolean(isModified);
    for (final Property property : definition.getProperties().values()) {
      if (!(property instanceof Property.DerivedProperty) && !(property instanceof Property.DenormalizedViewProperty)) {
        final String propertyID = ((DefaultProperty) property).propertyID;
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
    definition = DefaultEntityDefinition.getDefinitionMap().get(entityID);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityID);
    }
    for (final Property property : definition.getProperties().values()) {
      if (!(property instanceof Property.DerivedProperty) && !(property instanceof Property.DenormalizedViewProperty)) {
        final String propertyID = ((DefaultProperty) property).propertyID;
        super.setValue(propertyID, stream.readObject());
        if (isModified && stream.readBoolean()) {
          setOriginalValue(propertyID, stream.readObject());
        }
      }
    }
  }

  /**
   * Performs a basic data validation of <code>value</code>, checking if the <code>value</code> data type is
   * consistent with the data type of this property, returns the value.
   * @param value the value to validate
   * @param property the property
   * @return the value to validate
   * @throws IllegalArgumentException when the value type does not fit the property type
   */
  private static Object validateType(final Property property, final Object value) {
    if (value == null) {
      return value;
    }

    final Class type = property.getTypeClass();
    if (!type.equals(value.getClass()) && !type.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Value of type " + type + " expected for property " + property + ", got: " + value.getClass());
    }

    return value;
  }

  private static void validateValue(final EntityImpl entity, final Property property, final Object value) {
    if (property instanceof Property.DenormalizedViewProperty) {
      throw new IllegalArgumentException("Can not set the value of a denormalized view property");
    }
    if (property instanceof Property.DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof Property.ValueListProperty && value != null && !((Property.ValueListProperty) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid value list value: " + value + " for property " + property.getPropertyID());
    }
    if (value instanceof Entity && primaryKeysEqual(entity, (Entity) value)) {
      throw new IllegalArgumentException("Circular entity reference detected: " + entity + "->" + property.getPropertyID());
    }
  }

  private static boolean primaryKeysEqual(final EntityImpl entity1, final Entity entity2) {
    if (entity1.getEntityID().equals(entity2.getEntityID())) {
      for (final Property.PrimaryKeyProperty property : entity1.definition.getPrimaryKeyProperties()) {
        if (!Util.equal(entity1.getValue(property.getPropertyID()), entity2.getValue(property.getPropertyID()))) {
          return false;
        }
      }
    }

    return false;
  }

  /**
   * A class representing column key objects for entities, contains the values for those columns.
   */
  static final class KeyImpl extends DefaultValueMap<String, Object> implements Entity.Key, Serializable {

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
     * Caching this extremely frequently referenced object
     */
    private Definition definition;

    /**
     * Instantiates a new Key for the given entity type
     * @param definition the entity definition
     */
    KeyImpl(final Definition definition) {
      this.definition = definition;
      final List<Property.PrimaryKeyProperty> properties = definition.getPrimaryKeyProperties();
      final int propertyCount = properties.size();
      this.singleIntegerKey = propertyCount == 1 && properties.get(0).isInteger();
      this.compositeKey = propertyCount > 1;
    }

    /**
     * Instantiates a new KeyImpl for the given entity type, assuming it is a single value key
     * @param definition the entity definition
     * @param value the value
     * @throws IllegalArgumentException in case this key is a composite key
     */
    KeyImpl(final Definition definition, final Object value) {
      this(definition);
      if (compositeKey) {
        throw new IllegalArgumentException(definition.getEntityID() + " has a composite primary key");
      }

      final Property property = definition.getPrimaryKeyProperties().get(0);
      setValue(property.getPropertyID(), value);
    }

    /** {@inheritDoc} */
    @Override
    public String getEntityID() {
      return definition.getEntityID();
    }

    /** {@inheritDoc} */
    @Override
    public List<Property.PrimaryKeyProperty> getProperties() {
      return definition.getPrimaryKeyProperties();
    }

    /** {@inheritDoc} */
    @Override
    public Property.PrimaryKeyProperty getFirstKeyProperty() {
      if (getPropertyCount() == 0) {
        throw new IllegalStateException(definition.getEntityID() + " has no primary key properties");
      }

      return getProperties().get(0);
    }

    /** {@inheritDoc} */
    @Override
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

    /** {@inheritDoc} */
    @Override
    public int getPropertyCount() {
      if (singleIntegerKey || !compositeKey) {
        return 1;
      }

      return getProperties().size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSingleIntegerKey() {
      return singleIntegerKey;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompositeKey() {
      return compositeKey;
    }

    /**
     * Key objects are equal if the entityIDs match as well as all property values.
     * @param obj the object to compare with
     * @return true if object is equal to this key
     */
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
    @Override
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
        setHashCode((Integer) value);
      }
    }

    /** {@inheritDoc} */
    @Override
    protected void handleClear() {
      hashCode = INTEGER_NULL_VALUE;
      hashCodeDirty = false;
    }

    private void setHashCode(final Integer value) {
      if (value == null) {
        hashCode = INTEGER_NULL_VALUE;
      }
      else {
        hashCode = value;
      }
      hashCodeDirty = false;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(definition.getEntityID());
      for (final Property property : definition.getPrimaryKeyProperties()) {
        stream.writeObject(getValue(property.getPropertyID()));
      }
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
      final String entityID = (String) stream.readObject();
      definition = DefaultEntityDefinition.getDefinitionMap().get(entityID);
      if (definition == null) {
        throw new IllegalArgumentException("Undefined entity: " + entityID);
      }
      final List<Property.PrimaryKeyProperty> properties = definition.getPrimaryKeyProperties();
      final int propertyCount = properties.size();
      singleIntegerKey = propertyCount == 1 && properties.get(0).isInteger();
      compositeKey = propertyCount > 1;
      for (final Property property : properties) {
        setValue(((DefaultProperty) property).propertyID, stream.readObject());
      }
    }
  }
}
