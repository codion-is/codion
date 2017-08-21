/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

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
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final List<Entity> modifiedEntities = new ArrayList<>();
    for (final Entity entity : entities) {
      if (entity.isModified()) {
        modifiedEntities.add(entity);
      }
    }

    return modifiedEntities;
  }

  /**
   * Maps the given entities to their primary key
   * @param entities the entities to map
   * @return the mapped entities
   */
  public static Map<Entity.Key, Entity> mapToKey(final Collection<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final Map<Entity.Key, Entity> entityMap = new HashMap<>();
    for (final Entity entity : entities) {
      entityMap.put(entity.getKey(), entity);
    }

    return entityMap;
  }

  /**
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  public static List<Entity.Key> getKeys(final Collection<Entity> entities) {
    return getKeys(entities, false);
  }

  /**
   * @param entities the entities
   * @param originalValue if true then the original value of the primary key is used
   * @return a List containing the primary keys of the given entities
   */
  public static List<Entity.Key> getKeys(final Collection<Entity> entities, final boolean originalValue) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final List<Entity.Key> keys = new ArrayList<>(entities.size());
    for (final Entity entity : entities) {
      keys.add(originalValue ? entity.getOriginalKey() : entity.getKey());
    }

    return keys;
  }

  /**
   * Retrieves the values of the given keys, assuming they are single column keys.
   * @param <T> the value type
   * @param keys the keys
   * @return the actual property values of the given keys
   */
  public static <T> List<T> getValues(final List<Entity.Key> keys) {
    Objects.requireNonNull(keys, "keys");
    final List<T> list = new ArrayList<>(keys.size());
    for (int i = 0; i < keys.size(); i++) {
      final Entity.Key key = keys.get(i);
      list.add((T) key.get(key.getFirstProperty()));
    }

    return list;
  }

  /**
   * @param <T> the value type
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a Collection containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static <T> Collection<T> getValues(final String propertyID, final Collection<Entity> entities) {
    return getValues(propertyID, entities, true);
  }

  /**
   * @param <T> the value type
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a Collection containing the values of the property with the given ID from the given entities
   */
  public static <T> Collection<T> getValues(final String propertyID, final Collection<Entity> entities,
                                            final boolean includeNullValues) {
    return collectValues(new ArrayList<T>(entities == null ? 0 : entities.size()), propertyID, entities, includeNullValues);
  }

  /**
   * Returns a Collection containing the distinct values of {@code propertyID} from the given entities, excluding null values.
   * If the {@code entities} list is null an empty Collection is returned.
   * @param <T> the value type
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @return a Collection containing the distinct property values, excluding null values
   */
  public static <T> Collection<T> getDistinctValues(final String propertyID, final Collection<Entity> entities) {
    return getDistinctValues(propertyID, entities, false);
  }

  /**
   * Returns a Collection containing the distinct values of {@code propertyID} from the given entities.
   * If the {@code entities} list is null an empty Collection is returned.
   * @param <T> the value type
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @param includeNullValue if true then null is considered a value
   * @return a Collection containing the distinct property values
   */
  public static <T> Collection<T> getDistinctValues(final String propertyID, final Collection<Entity> entities,
                                                    final boolean includeNullValue) {
    return collectValues(new HashSet<T>(), propertyID, entities, includeNullValue);
  }

  /**
   * Creates a two dimensional array containing the values of the given properties for the given entities in string format.
   * @param properties the properties
   * @param entities the entities
   * @return the values of the given properties from the given entities in a two dimensional array
   */
  public static String[][] getStringValueArray(final List<? extends Property> properties, final List<Entity> entities) {
    final String[][] data = new String[entities.size()][];
    for (int i = 0; i < data.length; i++) {
      final List<String> line = new ArrayList<>();
      for (final Property property : properties) {
        line.add(entities.get(i).getAsString(property));
      }

      data[i] = line.toArray(new String[line.size()]);
    }

    return data;
  }

  /**
   * Sets the value of the property with ID {@code propertyID} to {@code value}
   * in the given entities
   * @param propertyID the ID of the property for which to set the value
   * @param value the value
   * @param entities the entities for which to set the value
   * @return the old property values mapped to their respective primary key
   */
  public static Map<Entity.Key, Object> put(final String propertyID, final Object value,
                                            final Collection<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final Map<Entity.Key, Object> oldValues = new HashMap<>(entities.size());
    for (final Entity entity : entities) {
      oldValues.put(entity.getKey(), entity.put(propertyID, value));
    }

    return oldValues;
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to the value of the property with ID {@code propertyID},
   * respecting the iteration order of the given collection
   * @param <K> the key type
   * @param propertyID the ID of the property which value should be used for mapping
   * @param entities the entities to map by property value
   * @return a Map of entities mapped to property value
   */
  public static <K> LinkedHashMap<K, Collection<Entity>> mapToValue(final String propertyID, final Collection<Entity> entities) {
    return Util.map(entities, value -> (K) value.get(propertyID));
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to their entityIDs,
   * respecting the iteration order of the given collection
   * @param entities the entities to map by entityID
   * @return a Map of entities mapped to entityID
   */
  public static LinkedHashMap<String, Collection<Entity>> mapToEntityID(final Collection<Entity> entities) {
    return Util.map(entities, Entity::getEntityID);
  }

  /**
   * Returns a LinkedHashMap containing the given entity keys mapped to their entityIDs,
   * respecting the iteration order of the given collection
   * @param keys the entity keys to map by entityID
   * @return a Map of entity keys mapped to entityID
   */
  public static LinkedHashMap<String, Collection<Entity.Key>> mapKeysToEntityID(final Collection<Entity.Key> keys) {
    return Util.map(keys, Entity.Key::getEntityID);
  }

  /**
   * @param entities the entities to copy
   * @return deep copies of the entities, in the same order as they are received
   */
  public static List<Entity> copyEntities(final List<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final List<Entity> copies = new ArrayList<>(entities.size());
    for (final Entity entity : entities) {
      copies.add((Entity) entity.getCopy());
    }

    return copies;
  }

  /**
   * Returns true if this entity has a null primary key or a null original primary key,
   * which is the best guess about an entity being new, as in, not existing in a database.
   * @param entity the entity
   * @return true if this entity has not been persisted
   */
  public static boolean isEntityNew(final Entity entity) {
    final Entity.Key key = entity.getKey();
    final Entity.Key originalKey = entity.getOriginalKey();

    return key.isNull() || originalKey.isNull();
  }

  /**
   * Maps the given entities and their updated counterparts to their original primary keys,
   * assumes a single copy of each entity in the given lists.
   * @param entitiesBeforeUpdate the entities before update
   * @param entitiesAfterUpdate the entities after update
   * @return the updated entities mapped to their respective original primary keys
   */
  public static Map<Entity.Key, Entity> mapToOriginalPrimaryKey(final List<Entity> entitiesBeforeUpdate,
                                                                final List<Entity> entitiesAfterUpdate) {
    final List<Entity> entitiesAfterUpdateCopy = new ArrayList<>(entitiesAfterUpdate);
    final Map<Entity.Key, Entity> keyMap = new HashMap<>(entitiesBeforeUpdate.size());
    for (final Entity entity : entitiesBeforeUpdate) {
      keyMap.put(entity.getOriginalKey(), findAndRemove(entity.getKey(), entitiesAfterUpdateCopy.listIterator()));
    }

    return keyMap;
  }

  private static Entity findAndRemove(final Entity.Key primaryKey, final ListIterator<Entity> iterator) {
    while (iterator.hasNext()) {
      final Entity current = iterator.next();
      if (current.getKey().equals(primaryKey)) {
        iterator.remove();

        return current;
      }
    }

    return null;
  }

  private static <T> Collection<T> collectValues(final Collection<T> collection, final String propertyID,
                                                 final Collection<Entity> entities, final boolean includeNullValues) {
    Objects.requireNonNull(collection);
    Objects.requireNonNull(propertyID);
    if (!Util.nullOrEmpty(entities)) {
      for (final Entity entity : entities) {
        final Object value = entity.get(propertyID);
        if (value != null || includeNullValues) {
          collection.add((T) value);
        }
      }
    }

    return collection;
  }

  /**
   * A class for mapping between entities and corresponding bean classes
   */
  public static class EntityBeanMapper {

    private static final String BEAN_CLASS_PARAM = "beanClass";
    private static final String ENTITY_ID_PARAM = "entityID";
    private static final String PROPERTY_ID_PARAM = "propertyID";
    private static final String PROPERTY_NAME_PARAM = "propertyName";

    private final Entities entities;

    private final Map<Class, String> entityIDMap = new HashMap<>();
    private final Map<Class, Map<String, GetterSetter>> propertyMap = new HashMap<>();

    public EntityBeanMapper(final Entities entities) {
      this.entities = entities;
    }

    /**
     * Associates the given bean class with the given entityID
     * @param beanClass the bean class representing entities with the given entityID
     * @param entityID the ID of the entity represented by the given bean class
     */
    public final void setEntityID(final Class beanClass, final String entityID) {
      Objects.requireNonNull(beanClass, BEAN_CLASS_PARAM);
      Objects.requireNonNull(entityID, ENTITY_ID_PARAM);
      entityIDMap.put(beanClass, entityID);
    }

    /**
     * @param beanClass the bean class
     * @return the entityID of the entity represented by the given bean class, null if none is specified
     */
    public final String getEntityID(final Class beanClass) {
      Objects.requireNonNull(beanClass, BEAN_CLASS_PARAM);
      return entityIDMap.get(beanClass);
    }

    /**
     * @param entityID the entityID
     * @return the class of the bean representing entities with the given entityID
     * @throws IllegalArgumentException in case no bean class has been defined for the given entityID
     */
    public final Class getBeanClass(final String entityID) {
      Objects.requireNonNull(entityID, ENTITY_ID_PARAM);
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
     * @throws NoSuchMethodException if the required setter/getter methods are not found
     */
    public final void setProperty(final Class beanClass, final String propertyID, final String propertyName) throws NoSuchMethodException {
      Objects.requireNonNull(beanClass, BEAN_CLASS_PARAM);
      Objects.requireNonNull(propertyID, PROPERTY_ID_PARAM);
      Objects.requireNonNull(propertyName, PROPERTY_NAME_PARAM);
      final Map<String, GetterSetter> beanPropertyMap = propertyMap.computeIfAbsent(beanClass, k -> new HashMap<>());
      final Property property = entities.getProperty(getEntityID(beanClass), propertyID);
      final Method getter = Util.getGetMethod(property.getTypeClass(), propertyName, beanClass);
      final Method setter = Util.getSetMethod(property.getTypeClass(), propertyName, beanClass);
      beanPropertyMap.put(propertyID, new GetterSetter(getter, setter));
    }

    /**
     * @param beanClass the bean class
     * @return a Map mapping bean property names to propertyIDs for the given bean class
     */
    public final Map<String, GetterSetter> getPropertyMap(final Class beanClass) {
      Objects.requireNonNull(beanClass, BEAN_CLASS_PARAM);
      return propertyMap.get(beanClass);
    }

    /**
     * Transforms the given bean into a Entity according to the information found in this EntityBeanMapper instance
     * @param bean the bean to transform
     * @return a Entity derived from the given bean
     * @throws java.lang.reflect.InvocationTargetException in case an exception is thrown during a bean method call
     * @throws IllegalAccessException if a required method is not accessible
     */
    public Entity toEntity(final Object bean) throws InvocationTargetException, IllegalAccessException {
      Objects.requireNonNull(bean, "bean");
      final Entity entity = entities.entity(getEntityID(bean.getClass()));
      final Map<String, GetterSetter> beanPropertyMap = getPropertyMap(bean.getClass());
      for (final Map.Entry<String, GetterSetter> propertyEntry : beanPropertyMap.entrySet()) {
        final Property property = entities.getProperty(entity.getEntityID(), propertyEntry.getKey());
        entity.put(property, propertyEntry.getValue().getter.invoke(bean));
      }

      return entity;
    }

    /**
     * Transforms the given beans into a Entities according to the information found in this EntityBeanMapper instance
     * @param beans the beans to transform
     * @return a List containing the Entities derived from the given beans, an empty List if {@code beans} is null or empty
     * @throws java.lang.reflect.InvocationTargetException in case an exception is thrown during a bean method call
     * @throws IllegalAccessException if a required method is not accessible
     */
    public List<Entity> toEntities(final List<?> beans) throws InvocationTargetException, IllegalAccessException {
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
     * @return a bean derived from the given entity
     * @throws NoSuchMethodException if a required setter method is not found in the bean class
     * @throws InvocationTargetException in case an exception is thrown during a bean method call
     * @throws IllegalAccessException if a required method is not accessible
     * @throws InstantiationException if the bean class can not be instantiated
     */
    public Object toBean(final Entity entity) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException, InstantiationException {
      Objects.requireNonNull(entity, "entity");
      final Class<?> beanClass = getBeanClass(entity.getEntityID());
      final Object bean = beanClass.getConstructor().newInstance();
      final Map<String, GetterSetter> beanPropertyMap = getPropertyMap(beanClass);
      for (final Map.Entry<String, GetterSetter> propertyEntry : beanPropertyMap.entrySet()) {
        final Property property = entities.getProperty(entity.getEntityID(), propertyEntry.getKey());
        propertyEntry.getValue().setter.invoke(bean, entity.get(property));
      }

      return bean;
    }

    /**
     * Transforms the given entities into beans according to the information found in this EntityBeanMapper instance
     * @param entities the entities to transform
     * @return a List containing the beans derived from the given entities, an empty List if {@code entities} is null or empty
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
