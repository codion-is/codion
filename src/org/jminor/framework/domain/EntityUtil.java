/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Deserializer;
import org.jminor.common.model.Item;
import org.jminor.common.model.Serializer;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueProvider;
import org.jminor.framework.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * A static utility class containing helper methods for working with Entity instances.
 */
public final class EntityUtil {

  private static final Random RANDOM = new Random();
  private static final String ENTITY_PARAM = "entity";
  private static final String ENTITIES_PARAM = "entities";

  private EntityUtil() {}

  /**
   * @param entities the entities
   * @return a List of entities that have been modified
   */
  public static List<Entity> getModifiedEntities(final Collection<Entity> entities) {
    Util.rejectNullValue(entities, ENTITIES_PARAM);
    final List<Entity> modifiedEntities = new ArrayList<Entity>();
    for (final Entity entity : entities) {
      if (entity.isModified()) {
        modifiedEntities.add(entity);
      }
    }

    return modifiedEntities;
  }

  /**
   * Hashes the given entities by their primary key
   * @param entities the entities to hash
   * @return the hashed entities
   */
  public static Map<Entity.Key, Entity> hashByPrimaryKey(final Collection<Entity> entities) {
    Util.rejectNullValue(entities, ENTITIES_PARAM);
    final Map<Entity.Key, Entity> entityMap = new HashMap<Entity.Key, Entity>();
    for (final Entity entity : entities) {
      entityMap.put(entity.getPrimaryKey(), entity);
    }

    return entityMap;
  }

  /**
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  public static List<Entity.Key> getPrimaryKeys(final Collection<Entity> entities) {
    return getPrimaryKeys(entities, false);
  }

  /**
   * @param entities the entities
   * @param originalValue if true then the original value of the primary key is used
   * @return a List containing the primary keys of the given entities
   */
  public static List<Entity.Key> getPrimaryKeys(final Collection<Entity> entities, final boolean originalValue) {
    Util.rejectNullValue(entities, ENTITIES_PARAM);
    final List<Entity.Key> keys = new ArrayList<Entity.Key>(entities.size());
    for (final Entity entity : entities) {
      keys.add(originalValue ? entity.getOriginalPrimaryKey() : entity.getPrimaryKey());
    }

    return keys;
  }

  /**
   * Retrieves the values of the given keys, assuming they are single column keys.
   * @param keys the keys
   * @return the actual property values of the given keys
   */
  public static Collection<Object> getPropertyValues(final Collection<Entity.Key> keys) {
    Util.rejectNullValue(keys, "keys");
    final List<Object> list = new ArrayList<Object>(keys.size());
    for (final Entity.Key key : keys) {
      list.add(key.getValue(key.getFirstKeyProperty().getPropertyID()));
    }

    return list;
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a Collection containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static Collection<Object> getPropertyValues(final String propertyID, final Collection<Entity> entities) {
    return getPropertyValues(propertyID, entities, true);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a Collection containing the values of the property with the given ID from the given entities
   */
  public static Collection<Object> getPropertyValues(final String propertyID, final Collection<Entity> entities,
                                                     final boolean includeNullValues) {
    if (entities == null || entities.isEmpty()) {
      return new ArrayList<Object>(0);
    }

    return getPropertyValues(entities.iterator().next().getProperty(propertyID), entities, includeNullValues);
  }

  /**
   * @param property the the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a Collection containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static Collection<Object> getPropertyValues(final Property property, final Collection<Entity> entities) {
    return getPropertyValues(property, entities, true);
  }

  /**
   * @param property the the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a Collection containing the values of the property with the given ID from the given entities
   */
  public static Collection<Object> getPropertyValues(final Property property, final Collection<Entity> entities,
                                                     final boolean includeNullValues) {
    Util.rejectNullValue(entities, ENTITIES_PARAM);
    final List<Object> values = new ArrayList<Object>(entities.size());
    for (final Entity entity : entities) {
      if (includeNullValues) {
        values.add(entity.getValue(property));
      }
      else if (!entity.isValueNull(property)) {
        values.add(entity.getValue(property));
      }
    }

    return values;
  }

  /**
   * Returns a Collection containing the distinct values of <code>propertyID</code> from the given entities, excluding null values.
   * If the <code>entities</code> list is null an empty Collection is returned.
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @return a Collection containing the distinct property values, excluding null values
   */
  public static Collection<Object> getDistinctPropertyValues(final String propertyID, final Collection<Entity> entities) {
    return getDistinctPropertyValues(propertyID, entities, false);
  }

  /**
   * Returns a Collection containing the distinct values of <code>propertyID</code> from the given entities.
   * If the <code>entities</code> list is null an empty Collection is returned.
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @param includeNullValue if true then null is considered a value
   * @return a Collection containing the distinct property values
   */
  public static Collection<Object> getDistinctPropertyValues(final String propertyID, final Collection<Entity> entities,
                                                             final boolean includeNullValue) {
    final Set<Object> values = new HashSet<Object>();
    if (entities == null) {
      return values;
    }
    for (final Entity entity : entities) {
      final Object value = entity.getValue(propertyID);
      if (value != null || includeNullValue) {
        values.add(value);
      }
    }

    return values;
  }

  /**
   * Sets the value of the property with ID <code>propertyID</code> to <code>value</code>
   * in the given entities
   * @param propertyID the ID of the property for which to set the value
   * @param value the value
   * @param entities the entities for which to set the value
   * @return the old property values mapped to their respective primary key
   */
  public static Map<Entity.Key, Object> setPropertyValue(final String propertyID, final Object value,
                                                         final Collection<Entity> entities) {
    Util.rejectNullValue(entities, ENTITIES_PARAM);
    final Map<Entity.Key, Object> oldValues = new HashMap<Entity.Key, Object>(entities.size());
    for (final Entity entity : entities) {
      oldValues.put(entity.getPrimaryKey(), entity.setValue(propertyID, value));
    }

    return oldValues;
  }

  /**
   * Returns a Map containing the given entities hashed by the value of the property with ID <code>propertyID</code>
   * @param propertyID the ID of the property which value should be used for mapping
   * @param entities the entities to map by property value
   * @return a Map of entities hashed by property value
   */
  public static Map<Object, Collection<Entity>> hashByPropertyValue(final String propertyID, final Collection<Entity> entities) {
    return Util.map(entities, new Util.HashKeyProvider<Object, Entity>() {
      @Override
      public Object getKey(final Entity value) {
        return value.getValue(propertyID);
      }
    });
  }

  /**
   * Returns a Map containing the given entities hashed by their entityIDs
   * @param entities the entities to map by entityID
   * @return a Map of entities hashed by entityID
   */
  public static Map<String, Collection<Entity>> hashByEntityID(final Collection<Entity> entities) {
    return Util.map(entities, new Util.HashKeyProvider<String, Entity>() {
      @Override
      public String getKey(final Entity value) {
        return value.getEntityID();
      }
    });
  }

  /**
   * Returns a Map containing the given entity keys hashed by their entityIDs
   * @param keys the entity keys to map by entityID
   * @return a Map of entity keys hashed by entityID
   */
  public static Map<String, Collection<Entity.Key>> hashKeysByEntityID(final Collection<Entity.Key> keys) {
    return Util.map(keys, new Util.HashKeyProvider<String, Entity.Key>() {
      @Override
      public String getKey(final Entity.Key value) {
        return value.getEntityID();
      }
    });
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the property IDs
   * @return a list containing the properties identified by the given propertyIDs
   */
  public static Collection<Property> getProperties(final String entityID, final Collection<String> propertyIDs) {
    if (propertyIDs == null || propertyIDs.isEmpty()) {
      return new ArrayList<Property>(0);
    }

    final List<Property> properties = new ArrayList<Property>(propertyIDs.size());
    for (final String propertyID : propertyIDs) {
      properties.add(Entities.getProperty(entityID, propertyID));
    }

    return properties;
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the property IDs
   * @return the given properties sorted by caption, or if that is not available, property ID
   */
  public static List<Property> getSortedProperties(final String entityID, final Collection<String> propertyIDs) {
    final List<Property> properties = new ArrayList<Property>(getProperties(entityID, propertyIDs));
    sort(properties);

    return properties;
  }

  /**
   * Sorts the given properties by caption, or if that is not available, property ID, ignoring case
   * @param properties the properties to sort
   */
  public static void sort(final List<? extends Property> properties) {
    Util.rejectNullValue(properties, "properties");
    final Collator collator = Collator.getInstance();
    Collections.sort(properties, new Comparator<Property>() {
      @Override
      public int compare(final Property o1, final Property o2) {
        return collator.compare(o1.toString().toLowerCase(), o2.toString().toLowerCase());
      }
    });
  }

  /**
   * @param entityID the entity ID
   * @return a list containing all updatable properties associated with the given entity ID
   */
  public static List<Property> getUpdatableProperties(final String entityID) {
    final List<Property.ColumnProperty> columnProperties = Entities.getColumnProperties(entityID,
            !Entities.getIdSource(entityID).isAutoGenerated(), false, false);
    final ListIterator<Property.ColumnProperty> iterator = columnProperties.listIterator();
    while(iterator.hasNext()) {
      final Property.ColumnProperty property = iterator.next();
      if (property.isForeignKeyProperty() || property.isDenormalized()) {
        iterator.remove();
      }
    }
    final List<Property> updatable = new ArrayList<Property>(columnProperties);
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(entityID);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      if (!foreignKeyProperty.isReadOnly() && foreignKeyProperty.isUpdatable()) {
        updatable.add(foreignKeyProperty);
      }
    }
    sort(updatable);

    return updatable;
  }

  /**
   * @param entities the entities to copy
   * @return deep copies of the entities
   */
  public static List<Entity> copyEntities(final List<Entity> entities) {
    Util.rejectNullValue(entities, ENTITIES_PARAM);
    final List<Entity> copies = new ArrayList<Entity>(entities.size());
    for (final Entity entity : entities) {
      copies.add((Entity) entity.getCopy());
    }

    return copies;
  }

  /**
   * @param entityID the entity ID
   * @param referenceEntities entities referenced by the given entity ID
   * @return a Entity instance containing randomized values, based on the property definitions
   */
  public static Entity createRandomEntity(final String entityID, final Map<String, Entity> referenceEntities) {
    return createEntity(entityID, new ValueProvider<Property, Object>() {
      @Override
      public Object getValue(final Property key) {
        return getRandomValue(key, referenceEntities);
      }
    });
  }

  /**
   * @param entityID the entity ID
   * @param valueProvider the value provider
   * @return an Entity instance initialized with values provided by the given value provider
   */
  public static Entity createEntity(final String entityID, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = Entities.entity(entityID);
    final boolean autoGenID = Entities.isPrimaryKeyAutoGenerated(entityID);
    for (final Property.ColumnProperty property : Entities.getColumnProperties(entityID, !autoGenID, false, true)) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()) {
        entity.setValue(property, valueProvider.getValue(property));
      }
    }
    for (final Property.ForeignKeyProperty property : Entities.getForeignKeyProperties(entityID)) {
      entity.setValue(property, valueProvider.getValue(property));
    }

    return entity;
  }

  /**
   * @param entity the entity to randomize
   * @param includePrimaryKey if true then the primary key values are include
   * @param referenceEntities entities referenced by the given entity
   * @return the entity with randomized values
   */
  public static Entity randomize(final Entity entity, final boolean includePrimaryKey, final Map<String, Entity> referenceEntities) {
    Util.rejectNullValue(entity, ENTITY_PARAM);
    for (final Property.ColumnProperty property : Entities.getColumnProperties(entity.getEntityID(), includePrimaryKey, false, true)) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()) {
        entity.setValue(property, getRandomValue(property, referenceEntities));
      }
    }
    for (final Property.ForeignKeyProperty property : Entities.getForeignKeyProperties(entity.getEntityID())) {
      if (referenceEntities != null && referenceEntities.containsKey(property.getReferencedEntityID())) {
        entity.setValue(property, getRandomValue(property, referenceEntities));
      }
    }

    return entity;
  }

  /**
   * @param property the property
   * @param referenceEntities entities referenced by the given property
   * @return a random value
   */
  public static Object getRandomValue(final Property property, final Map<String, Entity> referenceEntities) {
    Util.rejectNullValue(property, "property");
    if (property instanceof Property.ForeignKeyProperty) {
      final String referenceEntityID = ((Property.ForeignKeyProperty) property).getReferencedEntityID();
      return referenceEntities == null ? null : referenceEntities.get(referenceEntityID);
    }
    if (property instanceof Property.ValueListProperty) {
      final List<Item<Object>> items = ((Property.ValueListProperty) property).getValues();
      final Item item = items.get(RANDOM.nextInt(items.size()));

      return item.getItem();
    }
    switch (property.getType()) {
      case Types.BOOLEAN:
        return RANDOM.nextBoolean();
      case Types.CHAR:
        return (char) RANDOM.nextInt();
      case Types.DATE:
        return DateUtil.floorDate(new Date());
      case Types.TIMESTAMP:
        return DateUtil.floorTimestamp(new Timestamp(System.currentTimeMillis()));
      case Types.DOUBLE:
        return getRandomDouble(property);
      case Types.INTEGER:
        return getRandomInteger(property);
      case Types.VARCHAR:
        return getRandomString(property);
      default:
        return null;
    }
  }

  /**
   * @return a Serializer, if one is available on the classpath
   */
  @SuppressWarnings({"unchecked"})
  public static Serializer<Entity> getEntitySerializer() {
    if (!Configuration.entitySerializerAvailable()) {
      throw new IllegalArgumentException("Required configuration property is missing: " + Configuration.ENTITY_SERIALIZER_CLASS);
    }

    try {
      final String serializerClass = Configuration.getStringValue(Configuration.ENTITY_SERIALIZER_CLASS);

      return (Serializer<Entity>) Class.forName(serializerClass).getConstructor().newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return a Deserializer, if one is available on the classpath
   */
  @SuppressWarnings({"unchecked"})
  public static Deserializer<Entity> getEntityDeserializer() {
    if (!Configuration.entityDeserializerAvailable()) {
      throw new IllegalArgumentException("Required configuration property is missing: " + Configuration.ENTITY_DESERIALIZER_CLASS);
    }

    try {
      final String deserializerClass = Configuration.getStringValue(Configuration.ENTITY_DESERIALIZER_CLASS);

      return (Deserializer<Entity>) Class.forName(deserializerClass).getConstructor().newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param entities the entities to check
   * @return true if any of the given entities has a modified primary key
   */
  public static boolean isPrimaryKeyModified(final Collection<Entity> entities) {
    if (entities == null || entities.isEmpty()) {
      return false;
    }
    for (final Entity entity : entities) {
      for (final Property.PrimaryKeyProperty property : Entities.getPrimaryKeyProperties(entity.getEntityID())) {
        if (entity.isModified(property.getPropertyID())){
          return true;
        }
      }
    }

    return false;
  }

  private static Object getRandomString(final Property property) {
    final int minLength = property.isNullable() ? 0 : 1;
    return Util.createRandomString(minLength, property.getMaxLength() < 0 ? 10 : property.getMaxLength());
  }

  private static Object getRandomInteger(final Property property) {
    final double min = property.getMin() == null ? -10000000 : property.getMin();
    final double max = property.getMax() == null ? 10000000 : property.getMax();

    return (int) (min + (RANDOM.nextDouble() * ((max - min) + 1)));
  }

  private static Object getRandomDouble(final Property property) {
    final double min = property.getMin() == null ? -10000000 : property.getMin();
    final double max = property.getMax() == null ? 10000000 : property.getMax();
    //Min + (int)(Math.random() * ((Max - Min) + 1))
    final double ret = min + (RANDOM.nextDouble() * ((max - min) + 1));
    if (property.getMaximumFractionDigits() > 0) {
      return Util.roundDouble(ret, property.getMaximumFractionDigits());
    }
    else {
      return ret;
    }
  }

  /**
   * A class for mapping between entities and corresponding bean classes
   */
  public static class EntityBeanMapper {

    private final Map<Class, String> entityIDMap = new HashMap<Class, String>();
    private final Map<Class, Map<String, String>> propertyMap = new HashMap<Class, Map<String, String>>();

    /**
     * Associates the given bean class with the given entityID
     * @param beanClass the bean class representing entities with the given entityID
     * @param entityID the ID of the entity represented by the given bean class
     */
    public final void setEntityID(final Class beanClass, final String entityID) {
      entityIDMap.put(beanClass, entityID);
    }

    /**
     * @param beanClass the bean class
     * @return the entityID of the entity represented by the given bean class, null if none is specified
     */
    public final String getEntityID(final Class beanClass) {
      return entityIDMap.get(beanClass);
    }

    /**
     * @param entityID the entityID
     * @return the class of the bean representing entities with the given entityID
     * @throws IllegalArgumentException in case no bean class has been defined for the given entityID
     */
    public final Class getBeanClass(final String entityID) {
      for (final Map.Entry<Class, String> entry : entityIDMap.entrySet()) {
        if (entry.getValue().equals(entityID)) {
          return entry.getKey();
        }
      }

      throw new IllegalArgumentException("No bean class defined for entityID: " + entityID);
    }

    /**
     * Links the given bean property name to the property identified by the given propertyID in the specified bean class
     * @param beanClass the bean class
     * @param propertyID the propertyID of the entity property
     * @param propertyName the name of the bean property
     */
    public final void setProperty(final Class beanClass, final String propertyID, final String propertyName) {
      Map<String, String> beanPropertyMap = propertyMap.get(beanClass);
      if (beanPropertyMap == null) {
        beanPropertyMap = new HashMap<String, String>();
        propertyMap.put(beanClass, beanPropertyMap);
      }
      beanPropertyMap.put(propertyID, propertyName);
    }

    /**
     * @param beanClass the bean class
     * @return a Map mapping bean property names to propertyIDs for the given bean class
     */
    public final Map<String, String> getPropertyMap(final Class beanClass) {
      return propertyMap.get(beanClass);
    }

    /**
     * Transforms the given bean into a Entity according to the information found in this EntityBeanMapper instance
     * @param bean the bean to transform
     * @return a Entity derived from the given bean
     * @throws NoSuchMethodException if a required getter method is not found in the bean class
     * @throws java.lang.reflect.InvocationTargetException in case an exception is thrown during a bean method call
     * @throws IllegalAccessException if a required method is not accessible
     */
    public Entity toEntity(final Object bean) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
      if (bean == null) {
        return null;
      }

      final Entity entity = Entities.entity(getEntityID(bean.getClass()));
      final Map<String, String> beanPropertyMap = getPropertyMap(bean.getClass());
      for (final Map.Entry<String, String> propertyEntry : beanPropertyMap.entrySet()) {
        final Property property = Entities.getProperty(entity.getEntityID(), propertyEntry.getKey());
        final Method getter = Util.getGetMethod(property.getTypeClass(), propertyEntry.getValue(), bean);
        entity.setValue(property, getter.invoke(bean));
      }

      return entity;
    }

    /**
     * Transforms the given entity into a bean according to the information found in this EntityBeanMapper instance
     * @param entity the entity to transform
     * @return a bean derived from the given entity
     * @throws NoSuchMethodException if a required setter method is not found in the bean class
     * @throws InvocationTargetException in case an exception is thrown during a bean method call
     * @throws IllegalAccessException if a required method is not accessible
     * @throws InstantiationException if the bean class can not be instantiated
     */
    public Object toBean(final Entity entity) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException, InstantiationException {
      if (entity == null) {
        return null;
      }

      final Class beanClass = getBeanClass(entity.getEntityID());
      final Object bean = beanClass.getConstructor().newInstance();
      final Map<String, String> beanPropertyMap = getPropertyMap(beanClass);
      for (final Map.Entry<String, String> propertyEntry : beanPropertyMap.entrySet()) {
        final Property property = Entities.getProperty(entity.getEntityID(), propertyEntry.getKey());
        final Method setter = Util.getSetMethod(property.getTypeClass(), propertyEntry.getValue(), bean);
        setter.invoke(bean, entity.getValue(property));
      }

      return bean;
    }
  }
}
