/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Serializer;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * A static utility class containing helper methods for working with Entity instances.
 */
public final class EntityUtil {

  private static final String ENTITIES_PARAM = "entities";

  private EntityUtil() {}

  /**
   * @param entities the entities
   * @return a List of entities that have been modified
   */
  public static List<Entity> getModifiedEntities(final Collection<Entity> entities) {
    Util.rejectNullValue(entities, ENTITIES_PARAM);
    final List<Entity> modifiedEntities = new ArrayList<>();
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
    final Map<Entity.Key, Entity> entityMap = new HashMap<>();
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
    final List<Entity.Key> keys = new ArrayList<>(entities.size());
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
  public static <T> Collection<T> getPropertyValues(final Collection<Entity.Key> keys) {
    Util.rejectNullValue(keys, "keys");
    final List<T> list = new ArrayList<>(keys.size());
    for (final Entity.Key key : keys) {
      list.add((T) key.getValue(key.getFirstKeyProperty().getPropertyID()));
    }

    return list;
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a Collection containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static <T> Collection<T> getPropertyValues(final String propertyID, final Collection<Entity> entities) {
    return getPropertyValues(propertyID, entities, true);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a Collection containing the values of the property with the given ID from the given entities
   */
  public static <T> Collection<T> getPropertyValues(final String propertyID, final Collection<Entity> entities,
                                                    final boolean includeNullValues) {
    if (Util.nullOrEmpty(entities)) {
      return new ArrayList<>(0);
    }

    return getPropertyValues(entities.iterator().next().getProperty(propertyID), entities, includeNullValues);
  }

  /**
   * @param property the the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a Collection containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static <T> Collection<T> getPropertyValues(final Property property, final Collection<Entity> entities) {
    return getPropertyValues(property, entities, true);
  }

  /**
   * @param property the the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a Collection containing the values of the property with the given ID from the given entities
   */
  public static <T> Collection<T> getPropertyValues(final Property property, final Collection<Entity> entities,
                                                    final boolean includeNullValues) {
    Util.rejectNullValue(entities, ENTITIES_PARAM);
    final List<T> values = new ArrayList<>(entities.size());
    for (final Entity entity : entities) {
      if (includeNullValues) {
        values.add((T) entity.getValue(property));
      }
      else if (!entity.isValueNull(property)) {
        values.add((T) entity.getValue(property));
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
  public static <T> Collection<T> getDistinctPropertyValues(final String propertyID, final Collection<Entity> entities) {
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
  public static <T> Collection<T> getDistinctPropertyValues(final String propertyID, final Collection<Entity> entities,
                                                            final boolean includeNullValue) {
    final Set<T> values = new HashSet<>();
    if (entities == null) {
      return values;
    }
    for (final Entity entity : entities) {
      final Object value = entity.getValue(propertyID);
      if (value != null || includeNullValue) {
        values.add((T) value);
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
    final Map<Entity.Key, Object> oldValues = new HashMap<>(entities.size());
    for (final Entity entity : entities) {
      oldValues.put(entity.getPrimaryKey(), entity.setValue(propertyID, value));
    }

    return oldValues;
  }

  /**
   * Returns a LinkedHashMap containing the given entities hashed by the value of the property with ID <code>propertyID</code>,
   * respecting the iteration order of the given collection
   * @param propertyID the ID of the property which value should be used for mapping
   * @param entities the entities to map by property value
   * @return a Map of entities hashed by property value
   */
  public static LinkedHashMap<Object, Collection<Entity>> hashByPropertyValue(final String propertyID, final Collection<Entity> entities) {
    return Util.map(entities, new Util.HashKeyProvider<Object, Entity>() {
      @Override
      public Object getKey(final Entity value) {
        return value.getValue(propertyID);
      }
    });
  }

  /**
   * Returns a LinkedHashMap containing the given entities hashed by their entityIDs,
   * respecting the iteration order of the given collection
   * @param entities the entities to map by entityID
   * @return a Map of entities hashed by entityID
   */
  public static LinkedHashMap<String, Collection<Entity>> hashByEntityID(final Collection<Entity> entities) {
    return Util.map(entities, new Util.HashKeyProvider<String, Entity>() {
      @Override
      public String getKey(final Entity value) {
        return value.getEntityID();
      }
    });
  }

  /**
   * Returns a LinkedHashMap containing the given entity keys hashed by their entityIDs,
   * respecting the iteration order of the given collection
   * @param keys the entity keys to map by entityID
   * @return a Map of entity keys hashed by entityID
   */
  public static LinkedHashMap<String, Collection<Entity.Key>> hashKeysByEntityID(final Collection<Entity.Key> keys) {
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
    if (Util.nullOrEmpty(propertyIDs)) {
      return new ArrayList<>(0);
    }

    final List<Property> properties = new ArrayList<>(propertyIDs.size());
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
    final List<Property> properties = new ArrayList<>(getProperties(entityID, propertyIDs));
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
            Entities.getKeyGenerator(entityID).isManual(), false, false);
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
   * @return deep copies of the entities, in the same order as they are received
   */
  public static List<Entity> copyEntities(final List<Entity> entities) {
    Util.rejectNullValue(entities, ENTITIES_PARAM);
    final List<Entity> copies = new ArrayList<>(entities.size());
    for (final Entity entity : entities) {
      copies.add((Entity) entity.getCopy());
    }

    return copies;
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
   * @param entities the entities to check
   * @return true if any of the given entities has a modified primary key
   */
  public static boolean isPrimaryKeyModified(final Collection<Entity> entities) {
    if (Util.nullOrEmpty(entities)) {
      return false;
    }
    for (final Entity entity : entities) {
      if (entity != null) {
        for (final Property.ColumnProperty property : Entities.getPrimaryKeyProperties(entity.getEntityID())) {
          if (entity.isModified(property.getPropertyID())) {
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Creates an empty Entity instance returning the given string on a call to toString(), all other
   * method calls are routed to an empty Entity instance.
   * @param entityID the entityID
   * @param toStringValue the string to return by a call to toString() on the resulting entity
   * @return an empty entity wrapping a string
   */
  public static Entity createToStringEntity(final String entityID, final String toStringValue) {
    final Entity entity = Entities.entity(entityID);
    return Util.initializeProxy(Entity.class, new InvocationHandler() {
      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
        switch (method.getName()) {
          case "toString": return toStringValue;
          default: return method.invoke(entity, args);
        }
      }
    });
  }

  /**
   * Sets all property values to null
   * @param entity the entity
   * @return the same entity instance
   */
  public static Entity setNull(final Entity entity) {
    Util.rejectNullValue(entity, "entity");
    for (final Property property : Entities.getProperties(entity.getEntityID(), true)) {
      entity.setValue(property, null);
    }

    return entity;
  }

  /**
   * A class for mapping between entities and corresponding bean classes
   */
  public static class EntityBeanMapper {

    private static final String BEAN_CLASS_PARAM = "beanClass";
    private static final String ENTITY_ID_PARAM = "entityID";
    private static final String PROPERTY_ID_PARAM = "propertyID";
    private static final String PROPERTY_NAME_PARAM = "propertyName";

    private final Map<Class, String> entityIDMap = new HashMap<>();
    private final Map<Class, Map<String, GetterSetter>> propertyMap = new HashMap<>();

    /**
     * Associates the given bean class with the given entityID
     * @param beanClass the bean class representing entities with the given entityID
     * @param entityID the ID of the entity represented by the given bean class
     */
    public final void setEntityID(final Class beanClass, final String entityID) {
      Util.rejectNullValue(beanClass, BEAN_CLASS_PARAM);
      Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
      entityIDMap.put(beanClass, entityID);
    }

    /**
     * @param beanClass the bean class
     * @return the entityID of the entity represented by the given bean class, null if none is specified
     */
    public final String getEntityID(final Class beanClass) {
      Util.rejectNullValue(beanClass, BEAN_CLASS_PARAM);
      return entityIDMap.get(beanClass);
    }

    /**
     * @param entityID the entityID
     * @return the class of the bean representing entities with the given entityID
     * @throws IllegalArgumentException in case no bean class has been defined for the given entityID
     */
    public final Class getBeanClass(final String entityID) {
      Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
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
    public final void setProperty(final Class beanClass, final String propertyID, final String propertyName) throws NoSuchMethodException {
      Util.rejectNullValue(beanClass, BEAN_CLASS_PARAM);
      Util.rejectNullValue(propertyID, PROPERTY_ID_PARAM);
      Util.rejectNullValue(propertyName, PROPERTY_NAME_PARAM);
      Map<String, GetterSetter> beanPropertyMap = propertyMap.get(beanClass);
      if (beanPropertyMap == null) {
        beanPropertyMap = new HashMap<>();
        propertyMap.put(beanClass, beanPropertyMap);
      }
      final Property property = Entities.getProperty(getEntityID(beanClass), propertyID);
      final Method getter = Util.getGetMethod(property.getTypeClass(), propertyName, beanClass);
      final Method setter = Util.getSetMethod(property.getTypeClass(), propertyName, beanClass);
      beanPropertyMap.put(propertyID, new GetterSetter(getter, setter));
    }

    /**
     * @param beanClass the bean class
     * @return a Map mapping bean property names to propertyIDs for the given bean class
     */
    public final Map<String, GetterSetter> getPropertyMap(final Class beanClass) {
      Util.rejectNullValue(beanClass, BEAN_CLASS_PARAM);
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
      final Map<String, GetterSetter> beanPropertyMap = getPropertyMap(bean.getClass());
      for (final Map.Entry<String, GetterSetter> propertyEntry : beanPropertyMap.entrySet()) {
        final Property property = Entities.getProperty(entity.getEntityID(), propertyEntry.getKey());
        entity.setValue(property, propertyEntry.getValue().getter.invoke(bean));
      }

      return entity;
    }

    /**
     * Transforms the given beans into a Entities according to the information found in this EntityBeanMapper instance
     * @param beans the beans to transform
     * @return a List containing the Entities derived from the given beans, an empty List if <code>beans</code> is null or empty
     * @throws NoSuchMethodException if a required getter method is not found in the bean class
     * @throws java.lang.reflect.InvocationTargetException in case an exception is thrown during a bean method call
     * @throws IllegalAccessException if a required method is not accessible
     */
    public List<Entity> toEntities(final List<?> beans) throws InvocationTargetException,
            NoSuchMethodException, IllegalAccessException {
      if (Util.nullOrEmpty(beans)) {
        return Collections.emptyList();
      }
      final List<Entity> entities = new ArrayList<>(beans.size());
      for (final Object bean : beans) {
        entities.add(toEntity(bean));
      }

      return entities;
    }

    /**
     * Transforms the given entity into a bean according to the information found in this EntityBeanMapper instance
     * @param entity the entity to transform
     * @return a bean derived from the given entity, null if <code>entity</code> is null
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

      final Class<?> beanClass = getBeanClass(entity.getEntityID());
      final Object bean = beanClass.getConstructor().newInstance();
      final Map<String, GetterSetter> beanPropertyMap = getPropertyMap(beanClass);
      for (final Map.Entry<String, GetterSetter> propertyEntry : beanPropertyMap.entrySet()) {
        final Property property = Entities.getProperty(entity.getEntityID(), propertyEntry.getKey());
        propertyEntry.getValue().setter.invoke(bean, entity.getValue(property));
      }

      return bean;
    }

    /**
     * Transforms the given entities into beans according to the information found in this EntityBeanMapper instance
     * @param entities the entities to transform
     * @return a List containing the beans derived from the given entities, an empty List if <code>entities</code> is null or empty
     * @throws NoSuchMethodException if a required setter method is not found in the bean class
     * @throws InvocationTargetException in case an exception is thrown during a bean method call
     * @throws IllegalAccessException if a required method is not accessible
     * @throws InstantiationException if the bean class can not be instantiated
     */
    public List<Object> toBeans(final List<Entity> entities) throws InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
      if (Util.nullOrEmpty(entities)) {
        return Collections.emptyList();
      }
      final List<Object> beans = new ArrayList<>(entities.size());
      for (final Entity entity : entities) {
        beans.add(toBean(entity));
      }

      return beans;
    }

    private static final class GetterSetter {
      private final Method getter;
      private final Method setter;

      private GetterSetter(final Method getter, final Method setter) {
        this.getter = getter;
        this.setter = setter;
      }
    }
  }
}
