/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static is.codion.common.Util.map;
import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A repository specifying the {@link EntityDefinition}s for a given domain.
 * Factory for {@link Entity} and {@link Entity.Key} instances.
 * Helper class for working with Entity instances and related classes
 */
public interface Entities extends EntityDefinition.Provider, Serializable {

  /**
   * @return the domain id
   */
  String getDomainId();

  /**
   * Creates a new {@link Entity} instance with the given entityId
   * @param entityId the  entityId
   * @return a new {@link Entity} instance
   */
  Entity entity(String entityId);

  /**
   * Creates a new {@link Entity} instance with the given primary key
   * @param key the primary key
   * @return a new {@link Entity} instance
   */
  Entity entity(Entity.Key key);

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId
   * @param entityId the  entityId
   * @return a new {@link Entity.Key} instance
   */
  Entity.Key key(String entityId);

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId, initialised with the given value
   * @param entityId the  entityId
   * @param value the key value, assumes a single integer key
   * @return a new {@link Entity.Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or value is null
   */
  Entity.Key key(String entityId, Integer value);

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId, initialised with the given value
   * @param entityId the  entityId
   * @param value the key value, assumes a single long key
   * @return a new {@link Entity.Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or value is null
   */
  Entity.Key key(String entityId, Long value);

  /**
   * Creates new {@link Entity.Key} instances with the given entityId, initialised with the given values
   * @param entityId the  entityId
   * @param values the key values, assumes a single integer key
   * @return new {@link Entity.Key} instances
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or values is null
   */
  List<Entity.Key> keys(String entityId, Integer... values);

  /**
   * Creates new {@link Entity.Key} instances with the given entityId, initialised with the given values
   * @param entityId the  entityId
   * @param values the key values, assumes a single integer key
   * @return new {@link Entity.Key} instances
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or values is null
   */
  List<Entity.Key> keys(String entityId, Long... values);

  /**
   * Copies the given entities, with new copied instances of all foreign key value entities.
   * @param entities the entities to copy
   * @return deep copies of the entities, in the same order as they are received
   */
  List<Entity> deepCopyEntities(List<Entity> entities);

  /**
   * Copies the given entity.
   * @param entity the entity to copy
   * @return copy of the given entity
   */
  Entity copyEntity(Entity entity);

  /**
   * Copies the given entity, with new copied instances of all foreign key value entities.
   * @param entity the entity to copy
   * @return a deep copy of the given entity
   */
  Entity deepCopyEntity(Entity entity);

  /**
   * Copies the given key.
   * @param key the key to copy
   * @return a copy of the given key
   */
  Entity.Key copyKey(Entity.Key key);

  /**
   * Creates an empty Entity instance returning the given string on a call to toString(), all other
   * method calls are routed to an empty Entity instance.
   * @param entityId the entityId
   * @param toStringValue the string to return by a call to toString() on the resulting entity
   * @return an empty entity wrapping a string
   */
  Entity createToStringEntity(String entityId, String toStringValue);

  /**
   * Transforms the given entities into beans according to the information found in this Entities instance
   * @param <V> the bean type
   * @param entities the entities to transform
   * @return a List containing the beans derived from the given entities, an empty List if {@code entities} is null or empty
   * @see EntityDefinition.Builder#beanClass(Class)
   * @see Property.Builder#beanProperty(String)
   */
  <V> List<V> toBeans(List<Entity> entities);

  /**
   * Transforms the given entity into a bean according to the information found in this Entities instance
   * @param <V> the bean type
   * @param entity the entity to transform
   * @return a bean derived from the given entity
   * @see EntityDefinition.Builder#beanClass(Class)
   * @see Property.Builder#beanProperty(String)
   */
  <V> V toBean(Entity entity);

  /**
   * Transforms the given beans into a entities according to the information found in this Entities instance
   * @param beans the beans to transform
   * @return a List containing the entities derived from the given beans, an empty List if {@code beans} is null or empty
   * @see EntityDefinition.Builder#beanClass(Class)
   * @see Property.Builder#beanProperty(String)
   */
  List<Entity> fromBeans(List beans);

  /**
   * Creates an Entity from the given bean object.
   * @param bean the bean to convert to an Entity
   * @param <V> the bean type
   * @return a Entity based on the given bean
   * @see EntityDefinition.Builder#beanClass(Class)
   * @see Property.Builder#beanProperty(String)
   */
  <V> Entity fromBean(V bean);

  /**
   * Registers this instance, required for serialization of entities.
   * @return this Entities instance
   * @see #getDomainId()
   */
  Entities register();

  /**
   * Returns true if the entity has a null primary key or a null original primary key,
   * which is the best guess about an entity being new, as in, not existing in a database.
   * @param entity the entity
   * @return true if this entity has not been persisted
   */
  static boolean isEntityNew(final Entity entity) {
    requireNonNull(entity);
    final Entity.Key key = entity.getKey();
    final Entity.Key originalKey = entity.getOriginalKey();

    return key.isNull() || originalKey.isNull();
  }

  /**
   * Checks if the primary key of any of the given entities is modified
   * @param definition the definition of the entity
   * @param entities the entities to check
   * @return true if any of the given entities has a modified primary key
   */
  static boolean isKeyModified(final EntityDefinition definition, final Collection<Entity> entities) {
    requireNonNull(definition);
    if (nullOrEmpty(entities)) {
      return false;
    }

    return entities.stream().anyMatch(entity ->
            definition.getPrimaryKeyProperties().stream().anyMatch(entity::isModified));
  }

  /**
   * Returns all of the given entities which have been modified
   * @param entities the entities
   * @return a List of entities that have been modified
   * @see Entity#isModified()
   */
  static List<Entity> getModifiedEntities(final Collection<Entity> entities) {
    requireNonNull(entities, "entities");

    return entities.stream().filter(Entity::isModified).collect(toList());
  }

  /**
   * Returns all updatable {@link ColumnProperty}s which value is missing or the original value differs from the one in the comparison
   * entity, returns an empty Collection if all of {@code entity}s original values match the values found in {@code comparison}.
   * Note that only eagerly loaded blob values are included in this comparison.
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @return the updatable column properties which values differ from the ones in the comparison entity
   * @see BlobProperty#isEagerlyLoaded()
   */
  static List<ColumnProperty<?>> getModifiedColumnProperties(final Entity entity, final Entity comparison) {
    requireNonNull(entity);
    requireNonNull(comparison);
    return comparison.keySet().stream().filter(property -> {
      final boolean updatableColumnProperty = property instanceof ColumnProperty && ((ColumnProperty<?>) property).isUpdatable();
      final boolean lazilyLoadedBlobProperty = property instanceof BlobProperty && !((BlobProperty) property).isEagerlyLoaded();

      return updatableColumnProperty && !lazilyLoadedBlobProperty && isValueMissingOrModified(entity, comparison, property);
    }).map(property -> (ColumnProperty<?>) property).collect(toList());
  }

  /**
   * Returns the primary keys of the given entities.
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  static List<Entity.Key> getKeys(final List<Entity> entities) {
    requireNonNull(entities, "entities");
    final List<Entity.Key> keys = new ArrayList<>(entities.size());
    for (int i = 0; i < entities.size(); i++) {
      keys.add(entities.get(i).getKey());
    }

    return keys;
  }

  /**
   * Returns the primary keys of the entities referenced by the given entities via the given foreign key
   * @param entities the entities
   * @param foreignKeyProperty the foreign key
   * @return the primary keys of the referenced entities
   */
  static Set<Entity.Key> getReferencedKeys(final List<Entity> entities, final ForeignKeyProperty foreignKeyProperty) {
    final Set<Entity.Key> keySet = new HashSet<>();
    for (int i = 0; i < entities.size(); i++) {
      final Entity.Key key = entities.get(i).getReferencedKey(foreignKeyProperty);
      if (key != null) {
        keySet.add(key);
      }
    }

    return keySet;
  }

  /**
   * Returns the primary keys of the given entities with their original values.
   * @param entities the entities
   * @return a List containing the primary keys of the given entities with their original values
   */
  static List<Entity.Key> getOriginalKeys(final List<Entity> entities) {
    requireNonNull(entities, "entities");
    final List<Entity.Key> keys = new ArrayList<>(entities.size());
    for (int i = 0; i < entities.size(); i++) {
      keys.add(entities.get(i).getOriginalKey());
    }

    return keys;
  }

  /**
   * Retrieves the values of the given keys, assuming they are single column keys.
   * @param <T> the value type
   * @param keys the keys
   * @return the actual property values of the given keys
   */
  static <T> List<T> getValues(final List<Entity.Key> keys) {
    requireNonNull(keys, "keys");
    final List<T> list = new ArrayList<>(keys.size());
    for (int i = 0; i < keys.size(); i++) {
      final Entity.Key key = keys.get(i);
      list.add((T) key.get(key.getFirstProperty()));
    }

    return list;
  }

  /**
   * Returns the values associated with the given property from the given entities.
   * @param <T> the value type
   * @param attribute the attribute for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a List containing the non-null values of the property with the given id from the given entities
   */
  static <T> List<T> getValues(final Attribute<T> attribute, final Collection<Entity> entities) {
    requireNonNull(attribute, "attribute");
    requireNonNull(entities, "entities");
    return entities.stream().map(entity -> (T) entity.get(attribute)).collect(toList());
  }

  /**
   * Returns a Collection containing the distinct non-null values of {@code attribute} from the given entities.
   * @param <T> the value type
   * @param attribute the attribute for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @return a List containing the distinct non-null property values
   */
  static <T> List<T> getDistinctValues(final Attribute<T> attribute, final Collection<Entity> entities) {
    requireNonNull(attribute, "attribute");
    requireNonNull(entities, "entities");
    return entities.stream().map(entity -> (T) entity.get(attribute)).distinct().filter(Objects::nonNull).collect(toList());
  }

  /**
   * Returns a Collection containing the distinct values of {@code attribute} from the given entities.
   * @param <T> the value type
   * @param attribute the attribute for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @return a List containing the distinct property values
   */
  static <T> List<T> getDistinctValuesIncludingNull(final Attribute<T> attribute, final Collection<Entity> entities) {
    requireNonNull(attribute, "attribute");
    requireNonNull(entities, "entities");
    return entities.stream().map(entity -> (T) entity.get(attribute)).distinct().collect(toList());
  }

  /**
   * Sets the value of the property with id {@code attribute} to {@code value}
   * in the given entities
   * @param attribute the attribute for which to set the value
   * @param value the value
   * @param entities the entities for which to set the value
   * @param <T> the value type
   * @return the previous property values mapped to the primary key of the entity
   */
  static <T> Map<Entity.Key, T> put(final Attribute<T> attribute, final T value, final Collection<Entity> entities) {
    requireNonNull(entities, "entities");
    final Map<Entity.Key, T> previousValues = new HashMap<>(entities.size());
    for (final Entity entity : entities) {
      previousValues.put(entity.getKey(), entity.put(attribute, value));
    }

    return previousValues;
  }

  /**
   * Maps the given entities to their primary key
   * @param entities the entities to map
   * @return the mapped entities
   */
  static Map<Entity.Key, Entity> mapToKey(final Collection<Entity> entities) {
    requireNonNull(entities, "entities");
    final Map<Entity.Key, Entity> entityMap = new HashMap<>();
    for (final Entity entity : entities) {
      entityMap.put(entity.getKey(), entity);
    }

    return entityMap;
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to the value of the property with id {@code attribute},
   * respecting the iteration order of the given collection
   * @param <K> the key type
   * @param attribute the attribute which value should be used for mapping
   * @param entities the entities to map by property value
   * @return a Map of entities mapped to property value
   */
  static <K> LinkedHashMap<K, List<Entity>> mapToValue(final Attribute<K> attribute, final Collection<Entity> entities) {
    return map(entities, value -> (K) value.get(attribute));
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to their entityIds,
   * respecting the iteration order of the given collection
   * @param entities the entities to map by entityId
   * @return a Map of entities mapped to entityId
   */
  static LinkedHashMap<String, List<Entity>> mapToEntityId(final Collection<Entity> entities) {
    return map(entities, Entity::getEntityId);
  }

  /**
   * Returns a LinkedHashMap containing the given entity keys mapped to their entityIds,
   * respecting the iteration order of the given collection
   * @param keys the entity keys to map by entityId
   * @return a Map of entity keys mapped to entityId
   */
  static LinkedHashMap<String, List<Entity.Key>> mapKeysToEntityId(final Collection<Entity.Key> keys) {
    return map(keys, Entity.Key::getEntityId);
  }

  /**
   * Creates a two dimensional list containing the values of the given properties for the given entities in string format.
   * @param properties the properties
   * @param entities the entities
   * @return the values of the given properties from the given entities in a two dimensional list
   */
  static List<List<String>> getStringValueList(final List<? extends Property> properties, final List<Entity> entities) {
    requireNonNull(properties);
    requireNonNull(entities);
    final List<List<String>> data = new ArrayList<>();
    for (final Entity entity : entities) {
      final List<String> line = new ArrayList<>(properties.size());
      for (final Property property : properties) {
        line.add(entity.getAsString(property));
      }
      data.add(line);
    }

    return data;
  }

  /**
   * Finds entities according to the values of values
   * @param entities the entities to search
   * @param values the property values to use as condition mapped to their respective attributes
   * @return the entities having the exact same property values as in the given value map
   */
  static List<Entity> getEntitiesByValue(final Collection<Entity> entities, final Map<Attribute<?>, Object> values) {
    requireNonNull(entities);
    requireNonNull(values);
    final List<Entity> result = new ArrayList<>();
    for (final Entity entity : requireNonNull(entities, "entities")) {
      boolean equal = true;
      for (final Map.Entry<Attribute<?>, Object> entries : values.entrySet()) {
        final Attribute<?> attribute = entries.getKey();
        if (!entity.get(attribute).equals(entries.getValue())) {
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
   * Returns true if the values of the given properties are equal in the given entities.
   * @param entityOne the first entity
   * @param entityTwo the second entity
   * @param attributes the attributes to use
   * @return true if the values of the given attributes are equal in the given entities
   */
  static boolean valuesEqual(final Entity entityOne, final Entity entityTwo, final Attribute<?>... attributes) {
    requireNonNull(entityOne);
    requireNonNull(entityTwo);
    requireNonNull(attributes);
    if (attributes.length == 0) {
      throw new IllegalArgumentException("No properties provided for equality check");
    }
    for (final Attribute<?> attribute : attributes) {
      if (!Objects.equals(entityOne.get(attribute), entityTwo.get(attribute))) {
        return false;
      }
    }

    return true;
  }

  /**
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @param property the property to check
   * @return true if the value is missing or the original value differs from the one in the comparison entity
   */
  static boolean isValueMissingOrModified(final Entity entity, final Entity comparison, final Property property) {
    requireNonNull(entity);
    requireNonNull(comparison);
    requireNonNull(property);
    if (!entity.containsKey(property)) {
      return true;
    }

    final Object originalValue = entity.getOriginal(property);
    final Object comparisonValue = comparison.get(property);
    if (property.getAttribute().isBlob()) {
      return !Arrays.equals((byte[]) originalValue, (byte[]) comparisonValue);
    }

    return !Objects.equals(originalValue, comparisonValue);
  }
}
