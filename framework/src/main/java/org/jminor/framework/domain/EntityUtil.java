/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Serializer;
import org.jminor.common.Util;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.framework.Configuration;
import org.jminor.framework.i18n.FrameworkMessages;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Types;
import java.text.Collator;
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
import java.util.Set;

/**
 * A static utility class containing helper methods for working with Entity instances.
 */
public final class EntityUtil {

  private static final String ENTITIES_PARAM = "entities";

  private EntityUtil() {}

  /**
   * Populates an entity of the given type using the values provided by the given valueProvider,
   * only non-derived, non-denormalized and values that are not part of a foreign key are fetched from the value provider
   * @param entityID the entity ID
   * @param valueProvider the value provider
   * @return the populated entity
   */
  public static Entity getEntity(final String entityID, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = Entities.entity(entityID);
    final Collection<Property.ColumnProperty> columnProperties = Entities.getColumnProperties(entityID);
    for (final Property.ColumnProperty property : columnProperties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()) {//these are set via their respective parent properties
        entity.put(property, valueProvider.get(property));
      }
    }
    final Collection<Property.TransientProperty> transientProperties = Entities.getTransientProperties(entityID);
    for (final Property.TransientProperty transientProperty : transientProperties) {
      if (!(transientProperty instanceof Property.DerivedProperty) && !(transientProperty instanceof Property.DenormalizedViewProperty)) {
        entity.put(transientProperty, valueProvider.get(transientProperty));
      }
    }
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(entityID);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      entity.put(foreignKeyProperty, valueProvider.get(foreignKeyProperty));
    }
    entity.saveAll();

    return entity;
  }

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
  public static <T> Collection<T> getValues(final Collection<Entity.Key> keys) {
    Objects.requireNonNull(keys, "keys");
    final List<T> list = new ArrayList<>(keys.size());
    for (final Entity.Key key : keys) {
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
    if (Util.nullOrEmpty(entities)) {
      return new ArrayList<>(0);
    }

    return getValues(entities.iterator().next().getProperty(propertyID), entities, includeNullValues);
  }

  /**
   * @param <T> the value type
   * @param property the the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a Collection containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static <T> Collection<T> getValues(final Property property, final Collection<Entity> entities) {
    return getValues(property, entities, true);
  }

  /**
   * @param <T> the value type
   * @param property the the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a Collection containing the values of the property with the given ID from the given entities
   */
  public static <T> Collection<T> getValues(final Property property, final Collection<Entity> entities,
                                            final boolean includeNullValues) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final List<T> values = new ArrayList<>(entities.size());
    for (final Entity entity : entities) {
      if (includeNullValues) {
        values.add((T) entity.get(property));
      }
      else if (!entity.isValueNull(property)) {
        values.add((T) entity.get(property));
      }
    }

    return values;
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
    final Set<T> values = new HashSet<>();
    if (Util.nullOrEmpty(entities)) {
      return values;
    }
    for (final Entity entity : entities) {
      final Object value = entity.get(propertyID);
      if (value != null || includeNullValue) {
        values.add((T) value);
      }
    }

    return values;
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
    Objects.requireNonNull(properties, "properties");
    final Collator collator = Collator.getInstance();
    Collections.sort(properties, (o1, o2) -> collator.compare(o1.toString().toLowerCase(), o2.toString().toLowerCase()));
  }

  /**
   * @param entityID the entity ID
   * @return a list containing all updatable properties associated with the given entity ID
   */
  public static List<Property> getUpdatableProperties(final String entityID) {
    final List<Property.ColumnProperty> columnProperties = Entities.getColumnProperties(entityID,
            Entities.getKeyGeneratorType(entityID).isManual(), false, false);
    columnProperties.removeIf(property -> property.isForeignKeyProperty() || property.isDenormalized());
    final List<Property> updatable = new ArrayList<>(columnProperties);
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
    Objects.requireNonNull(entities, ENTITIES_PARAM);
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
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param entities the entities to check
   * @return true if any of the given entities has a modified primary key
   */
  public static boolean isKeyModified(final Collection<Entity> entities) {
    if (Util.nullOrEmpty(entities)) {
      return false;
    }
    for (final Entity entity : entities) {
      if (entity != null) {
        for (final Property.ColumnProperty property : Entities.getPrimaryKeyProperties(entity.getEntityID())) {
          if (entity.isModified(property)) {
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
    return Util.initializeProxy(Entity.class, (proxy, method, args) -> {
      if ("toString".equals(method.getName())) {
        return toStringValue;
      }

      return method.invoke(entity, args);
    });
  }

  /**
   * Sets all property values to null
   * @param entity the entity
   * @return the same entity instance
   */
  public static Entity putNull(final Entity entity) {
    Objects.requireNonNull(entity, "entity");
    for (final Property property : Entities.getProperties(entity.getEntityID(), true)) {
      entity.put(property, null);
    }

    return entity;
  }

  /**
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @return the first property which value is missing or the original value differs from the one in the comparison
   * entity, returns null if all of {@code entity}s original values match the values found in {@code comparison}
   */
  public static Property getModifiedProperty(final Entity entity, final Entity comparison) {
    for (final Property property : comparison.keySet()) {
      //BLOB property values are not loaded, so we can't compare those
      if (!property.isType(Types.BLOB) && isValueMissingOrModified(entity, comparison, property.getPropertyID())) {
        return property;
      }
    }

    return null;
  }

  /**
   * @param exception the record modified exception
   * @return a String describing the modification
   */
  public static String getModifiedExceptionMessage(final RecordModifiedException exception) {
    final Entity entity = (Entity) exception.getRow();
    final Entity modified = (Entity) exception.getModifiedRow();
    if (modified == null) {//record has been deleted
      return entity + " " + FrameworkMessages.get(FrameworkMessages.HAS_BEEN_DELETED);
    }
    final Property modifiedProperty = getModifiedProperty(entity, modified);

    return Entities.getCaption(entity.getEntityID()) + ", " + modifiedProperty + ": " +
            entity.getOriginal(modifiedProperty) + " -> " + modified.get(modifiedProperty);
  }

  /**
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @param propertyID the property to check
   * @return true if the value is missing or the original value differs from the one in the comparison entity
   */
  static boolean isValueMissingOrModified(final Entity entity, final Entity comparison, final String propertyID) {
    return !entity.containsKey(propertyID) || !Objects.equals(comparison.get(propertyID), entity.getOriginal(propertyID));
  }

  /**
   * Returns true if this entity has a null primary key or a null original primary key,
   * which indicates the best guess about an entity being new.
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
      Objects.requireNonNull(beanClass, BEAN_CLASS_PARAM);
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
        entity.put(property, propertyEntry.getValue().getter.invoke(bean));
      }

      return entity;
    }

    /**
     * Transforms the given beans into a Entities according to the information found in this EntityBeanMapper instance
     * @param beans the beans to transform
     * @return a List containing the Entities derived from the given beans, an empty List if {@code beans} is null or empty
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
     * @return a bean derived from the given entity, null if {@code entity} is null
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
