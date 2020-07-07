/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;

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
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A repository specifying the {@link EntityDefinition}s for a given domain.
 * Factory for {@link Entity} and {@link Key} instances.
 * Helper class for working with Entity instances and related classes
 */
public interface Entities {

  /**
   * @return the {@link DomainType} this {@link Entities} instance is associated with
   */
  DomainType getDomainType();

  /**
   * Returns the {@link EntityDefinition} for the given entityType
   * @param entityType the entityType
   * @return the entity definition
   * @throws IllegalArgumentException in case the definition is not found
   */
  EntityDefinition getDefinition(EntityType<?> entityType);

  /**
   * Returns all {@link EntityDefinition}s available
   * @return all entity definitions
   */
  Collection<EntityDefinition> getDefinitions();

  /**
   * Creates a new {@link Entity} instance with the given entityType
   * @param entityType the entityType
   * @return a new {@link Entity} instance
   */
  Entity entity(EntityType<?> entityType);

  /**
   * Creates a new {@link Entity} instance with the given primary key
   * @param key the primary key
   * @return a new {@link Entity} instance
   */
  Entity entity(Key key);

  /**
   * Creates a new {@link Key} instance with the given entityType
   * @param entityType the entityType
   * @return a new {@link Key} instance
   */
  Key key(EntityType<?> entityType);

  /**
   * Creates a new {@link Key} instance with the given entityType, initialised with the given value
   * @param entityType the entityType
   * @param value the key value, assumes a single integer key
   * @return a new {@link Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityType or value is null
   */
  Key key(EntityType<?> entityType, Integer value);

  /**
   * Creates a new {@link Key} instance with the given entityType, initialised with the given value
   * @param entityType the entityType
   * @param value the key value, assumes a single long key
   * @return a new {@link Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityType or value is null
   */
  Key key(EntityType<?> entityType, Long value);

  /**
   * Creates new {@link Key} instances with the given entityType, initialised with the given values
   * @param entityType the entityType
   * @param values the key values, assumes a single integer key
   * @return new {@link Key} instances
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityType or values is null
   */
  List<Key> keys(EntityType<?> entityType, Integer... values);

  /**
   * Creates new {@link Key} instances with the given entityType, initialised with the given values
   * @param entityType the entityType
   * @param values the key values, assumes a single integer key
   * @return new {@link Key} instances
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityType or values is null
   */
  List<Key> keys(EntityType<?> entityType, Long... values);

  /**
   * Copies the given entities, with new copied instances of all foreign key value entities.
   * @param entities the entities to copy
   * @return deep copies of the entities, in the same order as they are received
   */
  List<Entity> deepCopyEntities(List<? extends Entity> entities);

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
   * Casts the given entities to the given type.
   * @param type the type
   * @param entities the entities
   * @param <T> the entity type
   * @return typed entities
   * @throws IllegalArgumentException in case any of the entities is not of the given type
   */
  <T extends Entity> List<T> castTo(EntityType<T> type, List<Entity> entities);

  /**
   * Casts the given entity to the given type.
   * @param type the type
   * @param entity the entity
   * @param <T> the entity type
   * @return a typed entity
   * @throws IllegalArgumentException in case the entity is not of the given type
   */
  <T extends Entity> T castTo(EntityType<T> type, Entity entity);

  /**
   * Returns true if the entity has a null primary key or a null original primary key,
   * which is the best guess about an entity being new, as in, not existing in a database.
   * @param entity the entity
   * @return true if this entity has not been persisted
   */
  static boolean isEntityNew(final Entity entity) {
    requireNonNull(entity);
    final Key key = entity.getKey();
    final Key originalKey = entity.getOriginalKey();

    return key.isNull() || originalKey.isNull();
  }

  /**
   * Checks if the primary key of any of the given entities is modified
   * @param entities the entities to check
   * @return true if any of the given entities has a modified primary key
   */
  static boolean isKeyModified(final Collection<Entity> entities) {
    return requireNonNull(entities).stream().anyMatch(entity ->
            entity.getKey().getAttributes().stream().anyMatch(entity::isModified));
  }

  /**
   * Returns all of the given entities which have been modified
   * @param entities the entities
   * @param <T> the entity type
   * @return a List of entities that have been modified
   * @see Entity#isModified()
   */
  static <T extends Entity> List<T> getModifiedEntities(final Collection<T> entities) {
    return requireNonNull(entities, "entities").stream().filter(Entity::isModified).collect(toList());
  }

  /**
   * Returns all updatable {@link ColumnProperty}s which value is missing or the original value differs from the one in the comparison
   * entity, returns an empty Collection if all of {@code entity}s original values match the values found in {@code comparison}.
   * Note that only eagerly loaded blob values are included in this comparison.
   * @param definition the entity definition
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @return the updatable column properties which values differ from the ones in the comparison entity
   * @see BlobProperty#isEagerlyLoaded()
   */
  static List<ColumnProperty<?>> getModifiedColumnProperties(final EntityDefinition definition, final Entity entity, final Entity comparison) {
    requireNonNull(definition);
    requireNonNull(entity);
    requireNonNull(comparison);
    return comparison.entrySet().stream().map(entry -> definition.getProperty(entry.getKey())).filter(property -> {
      final boolean updatableColumnProperty = property instanceof ColumnProperty && ((ColumnProperty<?>) property).isUpdatable();
      final boolean lazilyLoadedBlobProperty = property instanceof BlobProperty && !((BlobProperty) property).isEagerlyLoaded();

      return updatableColumnProperty && !lazilyLoadedBlobProperty && isValueMissingOrModified(entity, comparison, property.getAttribute());
    }).map(property -> (ColumnProperty<?>) property).collect(toList());
  }

  /**
   * Returns the primary keys of the given entities.
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  static List<Key> getKeys(final List<? extends Entity> entities) {
    requireNonNull(entities, "entities");
    final List<Key> keys = new ArrayList<>(entities.size());
    for (int i = 0; i < entities.size(); i++) {
      keys.add(entities.get(i).getKey());
    }

    return keys;
  }

  /**
   * Returns the primary keys of the entities referenced by the given entities via the given foreign key
   * @param entities the entities
   * @param foreignKeyAttribute the foreign key attribute
   * @return the primary keys of the referenced entities
   */
  static Set<Key> getReferencedKeys(final List<? extends Entity> entities, final Attribute<Entity> foreignKeyAttribute) {
    final Set<Key> keySet = new HashSet<>();
    for (int i = 0; i < entities.size(); i++) {
      final Key key = entities.get(i).getReferencedKey(foreignKeyAttribute);
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
  static List<Key> getOriginalKeys(final List<? extends Entity> entities) {
    requireNonNull(entities, "entities");
    final List<Key> keys = new ArrayList<>(entities.size());
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
  static <T> List<T> getValues(final List<Key> keys) {
    requireNonNull(keys, "keys");
    final List<T> list = new ArrayList<>(keys.size());
    for (int i = 0; i < keys.size(); i++) {
      list.add(keys.get(i).get());
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
    return entities.stream().map(entity -> entity.get(attribute)).collect(toList());
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
    return entities.stream().map(entity -> entity.get(attribute)).distinct().filter(Objects::nonNull).collect(toList());
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
    return entities.stream().map(entity -> entity.get(attribute)).distinct().collect(toList());
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
  static <T> Map<Key, T> put(final Attribute<T> attribute, final T value, final Collection<Entity> entities) {
    requireNonNull(entities, "entities");
    final Map<Key, T> previousValues = new HashMap<>(entities.size());
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
  static Map<Key, Entity> mapToKey(final List<Entity> entities) {
    requireNonNull(entities, "entities");
    final Map<Key, Entity> entityMap = new HashMap<>();
    for (int i = 0; i < entities.size(); i++) {
      final Entity entity = entities.get(i);
      entityMap.put(entity.getKey(), entity);
    }

    return entityMap;
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to the value of {@code attribute},
   * respecting the iteration order of the given collection
   * @param <T> the key type
   * @param attribute the attribute which value should be used for mapping
   * @param entities the entities to map by property value
   * @return a Map of entities mapped to property value
   */
  static <T> LinkedHashMap<T, List<Entity>> mapToValue(final Attribute<T> attribute, final Collection<Entity> entities) {
    return map(entities, entity -> entity.get(attribute));
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to their entityTypes,
   * respecting the iteration order of the given collection
   * @param entities the entities to map by entityType
   * @return a Map of entities mapped to entityType
   */
  static LinkedHashMap<EntityType<Entity>, List<Entity>> mapToType(final Collection<? extends Entity> entities) {
    return map(entities, Entity::getEntityType);
  }

  /**
   * Returns a LinkedHashMap containing the given entity keys mapped to their entityTypes,
   * respecting the iteration order of the given collection
   * @param keys the entity keys to map by entityType
   * @return a Map of entity keys mapped to entityType
   */
  static LinkedHashMap<EntityType<Entity>, List<Key>> mapKeysToType(final Collection<Key> keys) {
    return map(keys, Key::getEntityType);
  }

  /**
   * Creates a two dimensional list containing the values of the given attributes for the given entities in string format.
   * @param attributes the attributes
   * @param entities the entities
   * @return the values of the given attributes from the given entities in a two dimensional list
   */
  static List<List<String>> getStringValueList(final List<Attribute<?>> attributes, final List<Entity> entities) {
    requireNonNull(attributes);
    return requireNonNull(entities).stream().map(entity ->
            attributes.stream().map(entity::getAsString).collect(toList())).collect(toList());
  }

  /**
   * Finds entities according to the values of values
   * @param entities the entities to search
   * @param values the property values to use as condition mapped to their respective attributes
   * @return the entities having the exact same property values as in the given value map
   */
  static List<Entity> getEntitiesByValue(final Collection<Entity> entities, final Map<Attribute<?>, Object> values) {
    requireNonNull(values);
    return requireNonNull(entities).stream().filter(entity ->
            values.entrySet().stream().allMatch(entry ->
                    Objects.equals(entity.get(entry.getKey()), entry.getValue()))).collect(toList());
  }

  /**
   * Returns true if the values of the given attributes are equal in the given entities.
   * @param entityOne the first entity
   * @param entityTwo the second entity
   * @param attributes the attributes which values to compare
   * @return true if the values of the given attributes are equal in the given entities
   */
  static boolean valuesEqual(final Entity entityOne, final Entity entityTwo, final Attribute<?>... attributes) {
    requireNonNull(entityOne);
    requireNonNull(entityTwo);
    if (requireNonNull(attributes).length == 0) {
      throw new IllegalArgumentException("No attributes provided for equality check");
    }

    return Arrays.stream(attributes).allMatch(attribute ->
            Objects.equals(entityOne.get(attribute), entityTwo.get(attribute)));
  }

  /**
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @param attribute the attribute to check
   * @param <T> the attribute type
   * @return true if the value is missing or the original value differs from the one in the comparison entity
   */
  static <T> boolean isValueMissingOrModified(final Entity entity, final Entity comparison, final Attribute<T> attribute) {
    requireNonNull(entity);
    requireNonNull(comparison);
    requireNonNull(attribute);
    if (!entity.containsKey(attribute)) {
      return true;
    }

    final T originalValue = entity.getOriginal(attribute);
    final T comparisonValue = comparison.get(attribute);
    if (attribute.isByteArray()) {
      return !Arrays.equals((byte[]) originalValue, (byte[]) comparisonValue);
    }

    return !Objects.equals(originalValue, comparisonValue);
  }
}
