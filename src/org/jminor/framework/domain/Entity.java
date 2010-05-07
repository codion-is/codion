/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Event;
import org.jminor.common.model.valuemap.ValueChangeMap;
import org.jminor.common.model.valuemap.ValueChangeMapImpl;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.Collator;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a row in a database table, providing access to the column values via the ValueMap interface.
 */
public final class Entity extends ValueChangeMapImpl<String, Object> implements Serializable, Comparable<Entity> {

  private static final long serialVersionUID = 1;

  /**
   * The primary key of this entity
   */
  private final Key primaryKey;

  /**
   * The foreign key values referenced by this entity
   */
  private final ValueChangeMap<String, Entity> foreignKeyValues = new ValueChangeMapImpl<String, Entity>() {
    @Override
    public String getMapTypeID() {
      return getEntityID();
    }
  };

  /**
   * Used to cache the return value of the frequently called toString(),
   * invalidated each time a property value changes
   */
  private transient String toString;

  /**
   * Caches the result of <code>getReferencedPrimaryKey</code> method
   */
  private transient Map<Property.ForeignKeyProperty, Key> referencedPrimaryKeysCache;

  /**
   * Caching this frequently referenced map
   */
  private transient Map<String, Property> properties;

  private static Map<String, Proxy> proxies;
  private static Proxy defaultProxy = new Proxy();

  /**
   * Instantiates a new Entity
   * @param entityID the ID of the entity type
   */
  public Entity(final String entityID) {
    this(new Key(entityID));
  }

  /**
   * Instantiates a new Entity
   * @param primaryKey the primary key
   */
  public Entity(final Key primaryKey) {
    if (primaryKey == null)
      throw new IllegalArgumentException("Can not instantiate a Entity without a primary key");
    this.primaryKey = primaryKey;
  }

  /**
   * @return the entity ID
   */
  public String getEntityID() {
    return primaryKey.getEntityID();
  }

  @Override
  public String getMapTypeID() {
    return getEntityID();
  }

  /**
   * @return the primary key of this entity
   */
  public Key getPrimaryKey() {
    return primaryKey;
  }

  /**
   * @param entityID the entityID
   * @return true if this entity is of the given type
   */
  public boolean is(final String entityID) {
    return getEntityID().equals(entityID);
  }

  /**
   * Retrieves the property identified by propertyID from the entity repository
   * @param propertyID the ID of the property to retrieve
   * @return the property identified by propertyID
   */
  public Property getProperty(final String propertyID) {
    if (properties == null)
      properties = EntityRepository.getProperties(getEntityID());

    return properties.get(propertyID);
  }

  /**
   * @param propertyID the property ID
   * @return true if the property has been modified since the entity was instantiated
   */
  @Override
  public boolean isModified(final String propertyID) {
    return super.isModified(propertyID) || primaryKey.isModified(propertyID);
  }

  /**
   * @return true if one or more properties have been modified
   * since the entity was instantiated
   */
  @Override
  public boolean isModified() {
    return super.isModified() || primaryKey.isModified();
  }

  /**
   * Returns the value this property had when the entity was loaded
   * @param propertyID the property identifier
   * @return the original value of the property
   */
  @Override
  public Object getOriginalValue(final String propertyID) {
    final Property property = getProperty(propertyID);
    if (property instanceof Property.PrimaryKeyProperty)
      return primaryKey.getOriginalValue(propertyID);
    if (property instanceof Property.ForeignKeyProperty)
      return foreignKeyValues.getOriginalValue(propertyID);

    return super.getOriginalValue(propertyID);
  }

  @Override
  public Object setValue(final String propertyID, final Object value) {
    return setValue(getProperty(propertyID), value);
  }

  public Object setValue(final Property property, final Object value) {
    return setValue(property, value, true);
  }

  /**
   * Sets the property value
   * @param property the property
   * @param value the value
   * @param validateType if true then type validation is performed
   * @return the old value
   */
  public Object setValue(final Property property, final Object value, final boolean validateType) {
    if (property instanceof Property.PrimaryKeyProperty)
      return primaryKey.setValue(property.getPropertyID(), value);
    if (property instanceof Property.DenormalizedViewProperty)
      throw new IllegalArgumentException("Can not set the value of a denormalized view property");
    if (property instanceof Property.DerivedProperty)
      throw new IllegalArgumentException("Can not set the value of a derived property");
    if (value != null && value instanceof Entity && value.equals(this))
      throw new IllegalArgumentException("Circular entity reference detected: " + primaryKey + "->" + property.getPropertyID());

    if (validateType)
      validateType(property, value);

    toString = null;
    if (property instanceof Property.ForeignKeyProperty && (value == null || value instanceof Entity)) {
      propagateReferenceValues((Property.ForeignKeyProperty) property, (Entity) value, false);
      return foreignKeyValues.setValue(property.getPropertyID(), (Entity) value);
    }
    else {
      return super.setValue(property.getPropertyID(), value);
    }
  }

  /**
   * Initializes the given value assuming it has no previously set value.
   * This method does not propagate foreign key values but does set denormalized values if any exist.
   * This method should be used with care, if at all.
   * @param propertyID the ID of the property for which to initialize the value
   * @param value the value
   */
  @Override
  public void initializeValue(final String propertyID, final Object value) {
    initializeValue(getProperty(propertyID), value);
  }

  /**
   * Initializes the given value assuming it has no previously set value.
   * This method does not propagate foreign key values but does set denormalized values if any exist.
   * This method should be used with care, if at all.
   * @param property the property for which to initialize the value
   * @param value the value
   */
  public void initializeValue(final Property property, final Object value) {
    if (property instanceof Property.PrimaryKeyProperty)
      primaryKey.initializeValue(property.getPropertyID(), value);
    else if (property instanceof Property.ForeignKeyProperty) {
      foreignKeyValues.initializeValue(property.getPropertyID(), (Entity) value);
      if (EntityRepository.hasDenormalizedProperties(getEntityID()))
        setDenormalizedValues((Entity) value, (Property.ForeignKeyProperty) property, true);
    }
    else
      super.initializeValue(property.getPropertyID(), value);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>
   */
  @Override
  public Object getValue(final String propertyID) {
    return getValue(getProperty(propertyID));
  }

  /**
   * @param property the property for which to retrieve the value
   * @return the value of the given property
   */
  public Object getValue(final Property property) {
    if (property instanceof Property.PrimaryKeyProperty)
      return primaryKey.getValue(property.getPropertyID());
    if (property instanceof Property.ForeignKeyProperty)
      return foreignKeyValues.getValue(property.getPropertyID());
    if (property instanceof Property.DenormalizedViewProperty)
      return getDenormalizedViewValue((Property.DenormalizedViewProperty) property);
    if (property instanceof Property.DerivedProperty)
      return getProxy(getEntityID()).getDerivedValue(this, (Property.DerivedProperty) property);

    if (containsValue(property.getPropertyID()))
      return super.getValue(property.getPropertyID());
    else
      return property.getDefaultValue();
  }

  @Override
  public List<Object> getValues() {
    final List<Object> values = new ArrayList<Object>(primaryKey.getValues());
    values.addAll(foreignKeyValues.getValues());
    values.addAll(super.getValues());

    return values;
  }

  @Override
  public Object removeValue(final String propertyID) {
    final Property property = getProperty(propertyID);
    if (property instanceof Property.PrimaryKeyProperty)
      return primaryKey.removeValue(propertyID);
    if (property instanceof Property.ForeignKeyProperty) {
      for (final Property fkProperty : ((Property.ForeignKeyProperty) property).getReferenceProperties())
        removeValue(fkProperty.getPropertyID());
      return foreignKeyValues.removeValue(propertyID);
    }

    return super.removeValue(propertyID);
  }

  @Override
  public boolean isValueNull(final String propertyID) {
    return isValueNull(getProperty(propertyID));
  }

  public boolean isValueNull(final Property property) {
    if (property instanceof Property.PrimaryKeyProperty)
      return primaryKey.isValueNull(property.getPropertyID());
    if (property instanceof Property.ForeignKeyProperty) {
      return foreignKeyValues.isValueNull(property.getPropertyID());
    }

    return super.isValueNull(property.getPropertyID());
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is an Entity
   * @throws RuntimeException if the property is not a foreign key property
   */
  public Entity getEntityValue(final String propertyID) {
    final Property property = getProperty(propertyID);
    if (property instanceof Property.ForeignKeyProperty)
      return foreignKeyValues.getValue(property.getPropertyID());

    throw new RuntimeException(propertyID + " is not a foreign key property");
  }

  /**
   * Returns true if the entity referenced via the given foreign key property has been loaded
   * @param foreignKeyPropertyID the property id
   * @return true if the reference entity has been loaded
   */
  public boolean isLoaded(final String foreignKeyPropertyID) {
    return foreignKeyValues.isValueNull(foreignKeyPropertyID);
  }

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Date
   * @throws ClassCastException if the value is not a Date instance
   */
  public Date getDateValue(final String propertyID) {
    return (Date) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Timestamp
   * @throws ClassCastException if the value is not a Timestamp instance
   */
  public Timestamp getTimestampValue(final String propertyID) {
    return (Timestamp) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a String
   * @throws ClassCastException if the value is not a String instance
   */
  public String getStringValue(final String propertyID) {
    return (String) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is an Integer
   * @throws ClassCastException if the value is not a Integer instance
   */
  public Integer getIntValue(final String propertyID) {
    return (Integer) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Boolean
   * @throws ClassCastException if the value is not a Boolean instance
   */
  public Boolean getBooleanValue(final String propertyID) {
    return (Boolean) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Character
   * @throws ClassCastException if the value is not a Character instance
   */
  public Character getCharValue(final String propertyID) {
    return (Character) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Double
   * @throws ClassCastException if the value is not a Double instance
   */
  public Double getDoubleValue(final String propertyID) {
    return (Double) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return a String representation of the value of the property identified by <code>propertyID</code>
   * @see #getFormattedValue(Property, java.text.Format)
   */
  public String getValueAsString(final String propertyID) {
    return getValueAsString(getProperty(propertyID));
  }

  /**
   * This method returns a String representation of the value associated with the given property,
   * if the property has a format it is used.
   * @param property the property for which to retrieve the value
   * @return a String representation of the value of <code>property</code>
   * @see #getFormattedValue(Property, java.text.Format)
   */
  public String getValueAsString(final Property property) {
    if (property instanceof Property.DenormalizedViewProperty)
      return getDenormalizedViewValueFormatted((Property.DenormalizedViewProperty) property);

    return getFormattedValue(property);
  }

  public String getFormattedValue(final String propertyID) {
    return getFormattedValue(getProperty(propertyID));
  }

  public String getFormattedValue(final String propertyID, final Format format) {
    return getFormattedValue(getProperty(propertyID), format);
  }

  public String getFormattedValue(final Property property) {
    return getFormattedValue(property, property.getFormat());
  }

  public String getFormattedValue(final Property property, final Format format) {
    return getProxy(getEntityID()).getFormattedValue(this, property, format);
  }

  @Override
  public void revertValue(final String key) {
    final Property property = getProperty(key);
    if (property instanceof Property.PrimaryKeyProperty)
      primaryKey.revertValue(key);
    else if (property instanceof Property.ForeignKeyProperty)
      foreignKeyValues.revertValue(key);
    else
      super.revertValue(key);
  }

  @Override
  public void revertAll() {
    primaryKey.revertAll();
    foreignKeyValues.revertAll();
    super.revertAll();
  }

  @Override
  public void clear() {
    primaryKey.clear();
    foreignKeyValues.clear();
    super.clear();
  }

  /**
   * @return true if the this entity instance has a null primary key
   */
  public boolean isNull() {
    return getPrimaryKey().isNull();
  }

  /**
   * @param entity the entity to compare to
   * @return true if all property values are equal
   */
  public boolean propertyValuesEqual(final Entity entity) {
    return entity.primaryKey.equals(primaryKey) && super.equals(entity);
  }

  /**
   * Returns true if the value of the property identified by <code>propertyID</code>
   * is equal to <code>value</code>
   * @param propertyID the property ID
   * @param value the value
   * @return true if the given value is equal to the value identified by the given property ID
   */
  public boolean valueEquals(final String propertyID, final Object value) {
    return valuesEqual(getValue(propertyID), value);
  }

  @Override
  protected boolean valuesEqual(final Object valueOne, final Object valueTwo) {
    if (valueOne instanceof Entity && valueTwo instanceof Entity) {
      return ((Entity) valueOne).getPrimaryKey().equals(((Entity) valueTwo).getPrimaryKey());
    }
    return super.valuesEqual(valueOne, valueTwo);
  }

  /**
   * @param object the object to compare with
   * @return true if the given object is an Entity and it´s primary key is equal to this ones
   */
  @Override
  public boolean equals(final Object object) {
    return this == object || object instanceof Entity && ((Entity) object).primaryKey.equals(primaryKey);
  }

  /**
   * @param entity the entity to compare with
   * @return the compare result from comparing <code>entity</code> with this Entity instance
   */
  public int compareTo(final Entity entity) {
    return getProxy(getEntityID()).compareTo(this, entity);
  }

  /**
   * Returns the hash code of the primary key
   */
  @Override
  public int hashCode() {
    return primaryKey.hashCode();
  }

  /**
   * @return a string representation of this entity
   * @see org.jminor.framework.domain.Entity.Proxy#toString(Entity)
   */
  @Override
  public String toString() {
    if (toString == null)
      toString = getProxy(getEntityID()).toString(this);

    return toString;
  }

  /**
   * @return a deep copy of this Entity
   */
  public Entity getCopy() {
    return getCopy(true);
  }

  /**
   * @param includePrimaryKey if true then primary key values are included
   * @return a deep copy of this Entity
   */
  public Entity getCopy(final boolean includePrimaryKey) {
    final Key key = getPrimaryKey().getCopy();
    final Entity copy = new Entity(getEntityID());
    copy.setAs(this);
    if (!includePrimaryKey)
      copy.getPrimaryKey().setAs(key);

    return copy;
  }

  /**
   * @return a deep copy of this entity in its original state
   */
  public Entity getOriginalCopy() {
    final Entity copy = getCopy();
    copy.revertAll();

    return copy;
  }

  /**
   * Makes this entity identical to <code>sourceEntity</code>.
   * Reference entity values, which are mutable, are deep copied with getCopy()
   * @param valueMap the Entity to copy
   */
  @Override
  public void setAs(final ValueChangeMap<String, Object> valueMap) {
    super.setAs(valueMap);
    final Entity entity = (Entity) valueMap;
    primaryKey.setAs(entity.getPrimaryKey());
    foreignKeyValues.setAs(entity.foreignKeyValues);
    toString = valueMap.toString();
  }

//  private static Map<String, Map<Key, Entity>> cache = new HashMap<String, Map<Key, Entity>>();
//  public Object readResolve() throws ObjectStreamException {
//    Map<Key, Entity> entityCache = cache.get(getEntityID());
//    if (entityCache == null) {
//      entityCache = new HashMap<Key, Entity>();
//      cache.put(getEntityID(), entityCache);
//    }
//    Entity entity = entityCache.get(primaryKey);
//    if (entity == null) {
//      entityCache.put(primaryKey, entity = this);
//      System.out.println("cached: " + this);
//    }
//    else
//      System.out.println("from cache: " + entity);
//
//    return entity;
//  }

  /**
   * Returns the primary key of the entity referenced by the given ForeignKeyProperty
   * @param foreignKeyProperty the foreign key property for which to retrieve the underlying EntityKey
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  public Key getReferencedPrimaryKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    Key primaryKey = referencedPrimaryKeysCache == null ? null : referencedPrimaryKeysCache.get(foreignKeyProperty);
    if (primaryKey != null)
      return primaryKey;

    primaryKey = foreignKeyProperty.getReferenceProperties().size() == 1 ?
            initializeSinglePropertyKey(foreignKeyProperty) : initializeMultiPropertyKey(foreignKeyProperty);

    if (referencedPrimaryKeysCache == null)
      referencedPrimaryKeysCache = new HashMap<Property.ForeignKeyProperty, Key>();
    referencedPrimaryKeysCache.put(foreignKeyProperty, primaryKey);

    return primaryKey;
  }

  @Override
  public void addValueListener(final ActionListener valueListener) {
    primaryKey.addValueListener(valueListener);
    foreignKeyValues.addValueListener(valueListener);
    super.addValueListener(valueListener);
  }

  @Override
  public void removeValueListener(final ActionListener valueListener) {
    primaryKey.removeValueListener(valueListener);
    foreignKeyValues.removeValueListener(valueListener);
    super.removeValueListener(valueListener);
  }

  @Override
  public boolean containsValue(final String propertyID) {
    return containsValue(getProperty(propertyID));
  }

  public boolean containsValue(final Property property) {
    if (property instanceof Property.PrimaryKeyProperty)
      return primaryKey.containsValue(property.getPropertyID());
    if (property instanceof Property.ForeignKeyProperty)
      return foreignKeyValues.containsValue(property.getPropertyID());

    return super.containsValue(property.getPropertyID());
  }

  @Override
  public Event eventValueChanged() {
    if (evtValueChanged == null)
      primaryKey.addValueListener(evtValueChanged = new Event());

    return evtValueChanged;
  }

  /**
   * True if the given objects are equal. Both objects being null results in true.
   * @param one the first object
   * @param two the second object
   * @return true if the given objects are equal
   */
  public static boolean isEqual(final Object one, final Object two) {
    final boolean oneNull = one == null;
    final boolean twoNull = two == null;

    return oneNull && twoNull || !(oneNull ^ twoNull) && one.equals(two);
  }

  /**
   * Returns true if one or more of the properties involved in the given foreign key is null
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key is null
   */
  public boolean isForeignKeyNull(final Property.ForeignKeyProperty foreignKeyProperty) {
    for (final Property property : foreignKeyProperty.getReferenceProperties())
      if (isValueNull(property.getPropertyID()))
        return true;

    return false;
  }

  /**
   * Returns a copy of the given value.
   * If the value is an entity it is deep copied.
   * @param value the value to copy
   * @return a copy of <code>value</code>
   */
  public static Object copyPropertyValue(final Object value) {
    return value instanceof Entity ? ((Entity) value).getCopy() : value;
  }

  /**
   * @param entities the entities to check
   * @return true if any of the given entities has a modified primary key
   */
  public static boolean isPrimaryKeyModified(final Collection<Entity> entities) {
    if (entities == null || entities.size() == 0)
      return false;

    for (final Entity entity : entities)
      if (entity.primaryKey.isModified())
        return true;

    return false;
  }

  /**
   * Sets the global default static proxy instance
   * @param proxy sets the default Entity.Proxy instance used if no entity specific one is specified
   * @see org.jminor.framework.domain.Entity.Proxy
   */
  public static void setDefaultProxy(final Proxy proxy) {
    defaultProxy = proxy;
  }

  /**
   * Sets a entity specific proxy instance
   * @param entityID the ID of the entity for which this proxy instance is used
   * @param entityProxy the proxy instance to link to the given entity ID
   * @see org.jminor.framework.domain.Entity.Proxy
   */
  public static void setProxy(final String entityID, final Proxy entityProxy) {
    if (proxies == null)
      proxies = new HashMap<String, Proxy>();

    if (proxies.containsKey(entityID))
      throw new RuntimeException("Proxy already set for: " + entityID);

    proxies.put(entityID, entityProxy);
  }

  /**
   * Returns the proxy instance assigned to the given entity ID or the default proxy if none has been assigned
   * @param entityID the entity ID for which to retrieve the proxy
   * @return the proxy instance assigned to the given entity ID
   * @see org.jminor.framework.domain.Entity.Proxy
   */
  public static Proxy getProxy(final String entityID) {
    if (proxies != null && proxies.containsKey(entityID))
      return proxies.get(entityID);

    return defaultProxy;
  }

  static Entity initialize(final String entityID, final Map<String, Object> values, final Map<String, Object> originalValues) {
    final Entity entity = new Entity(entityID);
    for (final Map.Entry<String, Object> entry : values.entrySet()) {
      entity.setValue(entry.getKey(), entry.getValue());
    }
    if (originalValues != null) {
      for (final Map.Entry<String, Object> entry : originalValues.entrySet())
        entity.setOriginalValue(entry.getKey(), originalValues.get(entry.getKey()));
    }

    return entity;
  }

  @Override
  protected void notifyValueChange(final String key, final Object value, final boolean initialization, final Object oldValue) {
    if (EntityRepository.hasLinkedDerivedProperties(getEntityID(), key)) {
      final Collection<String> linkedPropertyIDs = EntityRepository.getLinkedDerivedPropertyIDs(getEntityID(), key);
      for (final String propertyID : linkedPropertyIDs)
        super.notifyValueChange(propertyID, getValue(propertyID), false, null);
    }
    super.notifyValueChange(key, value, initialization, oldValue);
  }

  private void propagateReferenceValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity newValue,
                                        final boolean initialization) {
    referencedPrimaryKeysCache = null;
    setForeignKeyValues(foreignKeyProperty, newValue, initialization);
    if (EntityRepository.hasDenormalizedProperties(getEntityID()))
      setDenormalizedValues(newValue, foreignKeyProperty, initialization);
  }

  /**
   * Sets the values of the properties used in the reference to the corresponding values found in <code>referencedEntity</code>.
   * Example: EntityOne references EntityTwo via entityTwoID, after a call to this method the EntityOne.entityTwoID
   * property has the value of EntityTwo's primary key property. If <code>referencedEntity</code> is null then
   * the corresponding reference values are set to null.
   * @param foreignKeyProperty the entity reference property
   * @param referencedEntity the referenced entity
   * @param initialization true if the values are being initialized
   */
  private void setForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity,
                                   final boolean initialization) {
    final Collection<Property.PrimaryKeyProperty> referenceEntityPKProperties =
            referencedEntity != null ? referencedEntity.primaryKey.getProperties()
                    : EntityRepository.getPrimaryKeyProperties(foreignKeyProperty.getReferencedEntityID());
    for (final Property.PrimaryKeyProperty primaryKeyProperty : referenceEntityPKProperties) {
      final Property referenceProperty = foreignKeyProperty.getReferenceProperties().get(primaryKeyProperty.getIndex());
      if (!(referenceProperty instanceof Property.MirrorProperty)) {
        final Object value = referencedEntity != null ? referencedEntity.getValue(primaryKeyProperty) : null;
        if (initialization)
          initializeValue(referenceProperty, value);
        else
          setValue(referenceProperty, value, false);
      }
    }
  }

  /**
   * Sets the denormalized property values
   * @param entity the entity value owning the denormalized values
   * @param foreignKeyProperty the foreign key property refering to the value source
   * @param initialization true if the values are being initialized
   */
  private void setDenormalizedValues(final Entity entity, Property.ForeignKeyProperty foreignKeyProperty,
                                     final boolean initialization) {
      final Collection<Property.DenormalizedProperty> denormalizedProperties =
              EntityRepository.getDenormalizedProperties(getEntityID(), foreignKeyProperty.getPropertyID());
    if (denormalizedProperties != null) {
      for (final Property.DenormalizedProperty denormalizedProperty : denormalizedProperties) {
        final Object value = entity == null ? null : entity.getValue(denormalizedProperty.getDenormalizedProperty());
        if (initialization)
          initializeValue(denormalizedProperty, value);
        else
          setValue(denormalizedProperty, value, false);
      }
    }
  }

  private Object getDenormalizedViewValue(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = (Entity) getValue(denormalizedViewProperty.getForeignKeyPropertyID());

    return valueOwner != null ? valueOwner.getValue(denormalizedViewProperty.getDenormalizedProperty().getPropertyID()) : null;
  }

  private String getDenormalizedViewValueFormatted(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = (Entity) getValue(denormalizedViewProperty.getForeignKeyPropertyID());

    return valueOwner != null ? valueOwner.getFormattedValue(denormalizedViewProperty.getDenormalizedProperty()) : null;
  }

  private Key initializeSinglePropertyKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Property referenceKeyProperty = foreignKeyProperty.getReferenceProperties().get(0);
    final Object value = referenceKeyProperty instanceof Property.PrimaryKeyProperty
            ? this.primaryKey.getValue(referenceKeyProperty.getPropertyID())
            : super.getValue(referenceKeyProperty.getPropertyID());

    return new Entity.Key(foreignKeyProperty.getReferencedEntityID(), value);
  }

  private Key initializeMultiPropertyKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Key primaryKey = new Entity.Key(foreignKeyProperty.getReferencedEntityID());
    for (final Property referenceKeyProperty : foreignKeyProperty.getReferenceProperties()) {
      final Object value = referenceKeyProperty instanceof Property.PrimaryKeyProperty
              ? this.primaryKey.getValue(referenceKeyProperty.getPropertyID())
              : super.getValue(referenceKeyProperty.getPropertyID());
      if (value != null)
        primaryKey.setValue(foreignKeyProperty.getReferencedPropertyID(referenceKeyProperty), value);
      else
        break;
    }

    return primaryKey;
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
  private static Object validateType(final Property property, final Object value) throws IllegalArgumentException {
    if (value == null)
      return value;

    final Class type = Property.getTypeClass(property.getType());
    if (!type.equals(value.getClass()) && !type.isAssignableFrom(value.getClass()))
      throw new IllegalArgumentException("Value of type " + type + " expected for property " + property + ", got: " + value.getClass());

    return value;
  }

  /**
   * A class representing column key objects for entities, contains the values for those columns
   */
  public static class Key extends ValueChangeMapImpl<String, Object> implements Serializable {

    private static final long serialVersionUID = 1;

    /**
     * the entity ID
     */
    private final String entityID;

    /**
     * true if this key consists of a single integer value
     */
    private final boolean singleIntegerKey;

    /**
     * Caching the hash code
     */
    private int hashCode = -Integer.MAX_VALUE;

    /**
     * True if the value of a key property has changed, thereby invalidating the cached hash code value
     */
    private boolean hashCodeDirty = true;

    /**
     * Caching this extremely frequently referenced attribute
     */
    private transient List<Property.PrimaryKeyProperty> properties;

    /**
     * Instantiates a new Key for the given entity type
     * @param entityID the entity ID
     */
    public Key(final String entityID) {
      super(1);
      if (entityID == null)
        throw new IllegalArgumentException("Key can not be instantiated without an entityID");
      this.entityID = entityID;
      this.properties = EntityRepository.getPrimaryKeyProperties(entityID);
      this.singleIntegerKey = properties.size() == 1 && properties.get(0).isInteger();
    }

    /**
     * Instantiates a new Key for the given entity type, assuming it is a single value key
     * @param entityID the entity ID
     * @param value the value
     * @throws RuntimeException in case this key is a multi value key
     */
    public Key(final String entityID, final Object value) {
      this(entityID);
      if (properties.size() > 1)
        throw new RuntimeException("Not a single value key");
      setValue(properties.get(0).getPropertyID(), value);
    }

    /**
     * @return the entity ID
     */
    public String getEntityID() {
      return entityID;
    }

    @Override
    public String getMapTypeID() {
      return getEntityID();
    }

    /**
     * @return a List containing the properties comprising this key
     */
    public List<Property.PrimaryKeyProperty> getProperties() {
      if (properties == null)
        properties = EntityRepository.getPrimaryKeyProperties(entityID);

      return properties;
    }

    /**
     * @param propertyID the property identifier
     * @return the key property identified by propertyID, null if the property is not found
     */
    public Property.PrimaryKeyProperty getProperty(final String propertyID) {
      for (final Property.PrimaryKeyProperty property : getProperties())
        if (property.is(propertyID))
          return property;

      return null;
    }

    /**
     * @return the first key property
     */
    public Property getFirstKeyProperty() {
      if (getProperties().size() == 0)
        throw new RuntimeException("No properties defined for primary key");

      return getProperties().get(0);
    }

    /**
     * @return the first value contained in this key, useful for single property keys
     */
    public Object getFirstKeyValue() {
      return getValue(getFirstKeyProperty().getPropertyID());
    }

    /**
     * @param propertyID the property identifier
     * @return true if this key contains a property with the given identifier
     */
    public boolean containsProperty(final String propertyID) {
      for (final Property.PrimaryKeyProperty property : getProperties())
        if (property.is(propertyID))
          return true;

      return false;
    }

    @Override
    public Object setValue(final String propertyID, final Object newValue) {
      hashCodeDirty = true;
      if (singleIntegerKey) {
        if (!(newValue == null || newValue instanceof Integer))
          throw new IllegalArgumentException("Expecting a Integer value for Key: " + entityID + ", "
                  + propertyID + ", got " + newValue + "; " + newValue.getClass());
        hashCode = newValue == null ? -Integer.MAX_VALUE : (Integer) newValue;
        hashCodeDirty = false;
      }

      return super.setValue(propertyID, newValue);
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
        if (i++ < getPropertyCount() - 1)
          stringBuilder.append(",");
      }

      return stringBuilder.toString();
    }

    /**
     * @return an identical deep copy of this entity key
     */
    public Key getCopy() {
      final Key copy = new Key(entityID);
      copy.setAs(this);

      return copy;
    }

    /**
     * @return the number of properties comprising this key
     */
    public int getPropertyCount() {
      return singleIntegerKey ? 1 : getProperties().size();
    }

    /**
     * @param object the object to compare with
     * @return true if object is equal to this key
     */
    @Override
    public boolean equals(final Object object) {
      return this == object || object instanceof Key && ((Key) object).entityID.equals(entityID)
              && object.hashCode() == hashCode();
    }

    /**
     * @return a hash code based on the values of this key
     */
    @Override
    public int hashCode() {
      if (hashCodeDirty) {
        int hash = 0;
        for (final Object value : getValues())
          hash = hash + (value == null ? 0 : value.hashCode());

        hashCode = hash == 0 ? -Integer.MAX_VALUE : hash;//in case all values were null
        hashCodeDirty = false;
      }

      return hashCode;
    }

    /**
     * @return true if one of the properties has a null value
     */
    public boolean isNull() {
      if (singleIntegerKey)
        return hashCode() == -Integer.MAX_VALUE;

      if (hashCode() == -Integer.MAX_VALUE)
        return true;

      for (final Property property : getProperties())
        if (isValueNull(property.getPropertyID()))
          return true;

      return false;
    }

    public static List<Key> copy(final List<Key> entityKeys) {
      final List<Key> copies = new ArrayList<Key>(entityKeys.size());
      for (final Key key : entityKeys)
        copies.add(key.getCopy());

      return copies;
    }

    /**
     * Copies the values from <code>valueMap</code> to this entity key
     * @param valueMap the key to copy
     */
    @Override
    public void setAs(final ValueChangeMap<String, Object> valueMap) {
      final Key key = (Key) valueMap;
      if (key != null && !key.getEntityID().equals(getEntityID()))
        throw new IllegalArgumentException("Entity ID mismatch, expected: " + getEntityID() + ", actual: " + key.getEntityID());

      hashCodeDirty = true;
      super.setAs(valueMap);
    }

    @Override
    public Object copyValue(final Object value) {
      return copyPropertyValue(value);
    }

    public Key getOriginalCopy() {
      final Key copy = getCopy();
      copy.revertAll();

      return copy;
    }
  }

  /**
   * Acts as a proxy for retrieving values from Entity objects, allowing for plugged
   * in entity specific functionality, such as providing toString() and compareTo() implementations
   */
  public static class Proxy {
    protected final Collator collator = Collator.getInstance();

    public int compareTo(final Entity entity, final Entity entityToCompare) {
      return collator.compare(entity.toString(), entityToCompare.toString());
    }

    public String toString(final Entity entity) {
      final String entityID = entity.getEntityID();
      final ToString<String, Object> stringProvider = EntityRepository.getStringProvider(entityID);

      return stringProvider == null ? new StringBuilder(entityID).append(": ").append(entity.getPrimaryKey()).toString() : stringProvider.toString(entity);
    }

    public Object getDerivedValue(final Entity entity, final Property.DerivedProperty property) {
      return null;
    }

    public String getFormattedValue(final Entity entity, final Property property, final Format format) {
      final Object value = entity.getValue(property);
      return entity.isValueNull(property) ? "" : (format != null ? format.format(value) : value.toString());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public Color getBackgroundColor(final Entity entity) {
      return null;
    }
  }
}