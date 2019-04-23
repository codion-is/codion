/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Util;
import org.jminor.common.db.valuemap.ValueMap;

import java.sql.Types;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Helper class for working with Entity and related classes
 */
public final class Entities {

  private static final String ENTITIES_PARAM = "entities";

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
   * @param entities the entities
   * @return a List of entities that have been modified
   */
  public static List<Entity> getModifiedEntities(final Collection<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);

    return entities.stream().filter(ValueMap::isModified).collect(Collectors.toList());
  }

  /**
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @return all {@link Property.ColumnProperty}s which value is missing or the original value differs from the one in the comparison
   * entity, returns an empty Collection if all of {@code entity}s original values match the values found in {@code comparison}
   */
  public static final Collection<Property.ColumnProperty> getModifiedColumnProperties(final Entity entity, final Entity comparison) {
    //BLOB property values are not loaded, so we can't compare those
    return comparison.keySet().stream().filter(property ->
            property instanceof Property.ColumnProperty && !property.isType(Types.BLOB)
                    && isValueMissingOrModified(entity, comparison, property.getPropertyId()))
            .map(property -> (Property.ColumnProperty) property).collect(Collectors.toList());
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

    return entities.stream().map(entity -> originalValue ? entity.getOriginalKey() : entity.getKey()).collect(Collectors.toList());
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
   * @param propertyId the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a Collection containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static <T> Collection<T> getValues(final String propertyId, final Collection<Entity> entities) {
    return getValues(propertyId, entities, true);
  }

  /**
   * @param <T> the value type
   * @param propertyId the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a Collection containing the values of the property with the given ID from the given entities
   */
  public static <T> Collection<T> getValues(final String propertyId, final Collection<Entity> entities,
                                            final boolean includeNullValues) {
    return collectValues(new ArrayList<T>(entities == null ? 0 : entities.size()), propertyId, entities, includeNullValues);
  }

  /**
   * Returns a Collection containing the distinct values of {@code propertyId} from the given entities, excluding null values.
   * If the {@code entities} list is null an empty Collection is returned.
   * @param <T> the value type
   * @param propertyId the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @return a Collection containing the distinct property values, excluding null values
   */
  public static <T> Collection<T> getDistinctValues(final String propertyId, final Collection<Entity> entities) {
    return getDistinctValues(propertyId, entities, false);
  }

  /**
   * Returns a Collection containing the distinct values of {@code propertyId} from the given entities.
   * If the {@code entities} list is null an empty Collection is returned.
   * @param <T> the value type
   * @param propertyId the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @param includeNullValue if true then null is considered a value
   * @return a Collection containing the distinct property values
   */
  public static <T> Collection<T> getDistinctValues(final String propertyId, final Collection<Entity> entities,
                                                    final boolean includeNullValue) {
    return collectValues(new HashSet<T>(), propertyId, entities, includeNullValue);
  }

  /**
   * Sets the value of the property with ID {@code propertyId} to {@code value}
   * in the given entities
   * @param propertyId the ID of the property for which to set the value
   * @param value the value
   * @param entities the entities for which to set the value
   * @return the old property values mapped to their respective primary key
   */
  public static Map<Entity.Key, Object> put(final String propertyId, final Object value,
                                            final Collection<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final Map<Entity.Key, Object> oldValues = new HashMap<>(entities.size());
    for (final Entity entity : entities) {
      oldValues.put(entity.getKey(), entity.put(propertyId, value));
    }

    return oldValues;
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
   * Returns a LinkedHashMap containing the given entities mapped to the value of the property with ID {@code propertyId},
   * respecting the iteration order of the given collection
   * @param <K> the key type
   * @param propertyId the ID of the property which value should be used for mapping
   * @param entities the entities to map by property value
   * @return a Map of entities mapped to property value
   */
  public static <K> LinkedHashMap<K, List<Entity>> mapToValue(final String propertyId, final Collection<Entity> entities) {
    return Util.map(entities, value -> (K) value.get(propertyId));
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to their entityIds,
   * respecting the iteration order of the given collection
   * @param entities the entities to map by entityId
   * @return a Map of entities mapped to entityId
   */
  public static LinkedHashMap<String, List<Entity>> mapToEntityId(final Collection<Entity> entities) {
    return Util.map(entities, Entity::getEntityId);
  }

  /**
   * Returns a LinkedHashMap containing the given entity keys mapped to their entityIds,
   * respecting the iteration order of the given collection
   * @param keys the entity keys to map by entityId
   * @return a Map of entity keys mapped to entityId
   */
  public static LinkedHashMap<String, List<Entity.Key>> mapKeysToEntityID(final Collection<Entity.Key> keys) {
    return Util.map(keys, Entity.Key::getEntityId);
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

      data[i] = line.toArray(new String[0]);
    }

    return data;
  }

  /**
   * @param entities the entities to copy
   * @return deep copies of the entities, in the same order as they are received
   */
  public static List<Entity> copyEntities(final List<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);

    return entities.stream().map(entity -> (Entity) entity.getCopy()).collect(Collectors.toList());
  }

  /**
   * Sorts the given properties by caption, or if that is not available, property ID, ignoring case
   * @param properties the properties to sort
   */
  public static void sort(final List<? extends Property> properties) {
    Objects.requireNonNull(properties, "properties");
    final Collator collator = Collator.getInstance();
    properties.sort((o1, o2) -> collator.compare(o1.toString().toLowerCase(), o2.toString().toLowerCase()));
  }

  /**
   * Finds entities according to the values of values
   * @param entities the entities to search
   * @param values the property values to use as condition mapped to their respective propertyIds
   * @return the entities having the exact same property values as in the given value map
   */
  public static List<Entity> getEntitiesByValue(final Collection<Entity> entities, final Map<String, Object> values) {
    final List<Entity> result = new ArrayList<>();
    for (final Entity entity : Objects.requireNonNull(entities, ENTITIES_PARAM)) {
      boolean equal = true;
      for (final Map.Entry<String, Object> entries : values.entrySet()) {
        final String propertyId = entries.getKey();
        if (!entity.get(propertyId).equals(entries.getValue())) {
          equal = false;
          break;
        }
      }
      if (equal) {
        result.add(entity);
      }
    }

    return result;
  }

  /**
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @param propertyId the property to check
   * @return true if the value is missing or the original value differs from the one in the comparison entity
   */
  static boolean isValueMissingOrModified(final Entity entity, final Entity comparison, final String propertyId) {
    return !entity.containsKey(propertyId) || !Objects.equals(comparison.get(propertyId), entity.getOriginal(propertyId));
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

  private static <T> Collection<T> collectValues(final Collection<T> collection, final String propertyId,
                                                 final Collection<Entity> entities, final boolean includeNullValues) {
    Objects.requireNonNull(collection);
    Objects.requireNonNull(propertyId);
    if (!Util.nullOrEmpty(entities)) {
      for (final Entity entity : entities) {
        final Object value = entity.get(propertyId);
        if (value != null || includeNullValues) {
          collection.add((T) value);
        }
      }
    }

    return collection;
  }
}
