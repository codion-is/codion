/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static is.codion.common.Util.map;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Represents a row in a database table.
 * Helper class for working with Entity instances and related classes
 */
public interface Entity extends Comparable<Entity> {

  /**
   * @return the entity type
   */
  EntityType<?> getEntityType();

  /**
   * Sets the value of the given attribute, returning the old value if any
   * @param attribute the attribute
   * @param value the value
   * @param <T> the value type
   * @return the previous value
   */
  <T> T put(Attribute<T> attribute, T value);

  /**
   * Returns the value associated with {@code attribute}.
   * @param attribute the attribute for which to retrieve the value
   * @param <T> the value type
   * @return the value of the given attribute
   */
  <T> T get(Attribute<T> attribute);

  /**
   * Returns the value associated with {@code attribute}, wrapped in an {@link Optional}.
   * @param attribute the attribute for which to retrieve the value
   * @param <T> the value type
   * @return the value of the given attribute, wrapped in an {@link Optional}
   */
  <T> Optional<T> getOptional(Attribute<T> attribute);

  /**
   * Returns the original value associated with {@code attribute}.
   * If the value has not been modified the current value is returned.
   * @param attribute the attribute for which to retrieve the original value
   * @param <T> the value type
   * @return the original value of the given attribute
   */
  <T> T getOriginal(Attribute<T> attribute);

  /**
   * This method returns a String representation of the value associated with the given attribute,
   * if the associated property has a format it is used.
   * @param attribute the attribute for which to retrieve the value
   * @return a String representation of the value associated with {@code attribute}
   */
  String getAsString(Attribute<?> attribute);

  /**
   * This method returns a String representation of the value associated with the given property,
   * if the associated property has a format it is used.
   * @param property the property for which to retrieve the value
   * @return a String representation of the value associated with {@code property}
   */
  String getAsString(Property<?> property);

  /**
   * Reverts the value associated with the given attribute to its original value.
   * If the value has not been modified then calling this method has no effect.
   * @param attribute the attribute for which to revert the value
   */
  void revert(Attribute<?> attribute);

  /**
   * Reverts all value modifications that have been made.
   * This entity will be unmodified after a call to this method.
   * If no modifications have been made then calling this method has no effect.
   */
  void revertAll();

  /**
   * Saves the value associated with the given attribute, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param attribute the attribute for which to save the value
   */
  void save(Attribute<?> attribute);

  /**
   * Saves all the value modifications that have been made.
   * This entity will be unmodified after a call to this method.
   * @see #isModified()
   */
  void saveAll();

  /**
   * Removes the given value from this Entity along with the original value if any.
   * If no value is mapped to the given attribute, this method has no effect.
   * @param attribute the attribute to remove
   * @param <T> the value type
   * @return the previous value mapped to the given attribute
   */
  <T> T remove(Attribute<T> attribute);

  /**
   * Returns true if a null value is mapped to the given attribute or if no mapping is found.
   * In case of foreign key attributes the value of the underlying reference attribute is checked.
   * @param attribute the attribute
   * @return true if the value mapped to the given attribute is null or no value is mapped
   */
  boolean isNull(Attribute<?> attribute);

  /**
   * Returns true if a this Entity contains a non-null value mapped to the given attribute
   * In case of foreign key attributes the value of the underlying reference attribute is checked.
   * @param attribute the attribute
   * @return true if a non-null value is mapped to the given attribute
   */
  boolean isNotNull(Attribute<?> attribute);

  /**
   * Returns true if this Entity contains a value for the given attribute, that value can be null.
   * @param attribute the attribute
   * @return true if a value is mapped to this attribute
   */
  boolean contains(Attribute<?> attribute);

  /**
   * Returns the Entity instance referenced by the given foreign key attribute.
   * If the underlying reference attribute contains a value, that is,
   * a foreign key value exists but the actual referenced entity has not
   * been loaded, an "empty" entity is returned, containing only the primary
   * key value. Null is returned only if the actual reference attribute is null.
   * @param foreignKey the foreign key for which to retrieve the value
   * @return the value of {@code foreignKey},
   * assuming it is an Entity
   * @throws IllegalArgumentException if the attribute is not a foreign key attribute
   * @see #isLoaded(ForeignKey)
   */
  Entity getForeignKey(ForeignKey foreignKey);

  /**
   * Returns the primary key of the entity referenced by the given {@link Attribute},
   * if the reference is null this method returns null.
   * @param foreignKey the foreign key for which to retrieve the underlying {@link Key}
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  Key getReferencedKey(ForeignKey foreignKey);

  /**
   * Returns true if the value of the given foreign key is null, in case of composite
   * foreign keys a single null value of a non-null attribute is enough.
   * @param foreignKey the foreign key
   * @return true if the foreign key value is null
   */
  boolean isForeignKeyNull(ForeignKey foreignKey);

  /**
   * Returns true if the value associated with the given attribute has been modified since first set,
   * note that this does not apply to attributes based on derived values.
   * @param attribute the attribute
   * @return true if the value associated with the given attribute has been modified
   */
  boolean isModified(Attribute<?> attribute);

  /**
   * Returns true if one or more writable attributes have been modified from their initial value,
   * read only and non-updatable attributes are excluded unless they are transient.
   * @return true if one or more attributes have been modified since the entity was initialized
   */
  boolean isModified();

  /**
   * Returns true if this entity has a null primary key or a null original primary key,
   * which is the best guess about an entity being new, as in, not existing in a database.
   * @return true if this entity has not been persisted
   */
  boolean isNew();

  /**
   * Clears the primary key values from this entity,
   * current as well as original values if any
   * @return this Entity instance
   */
  Entity clearPrimaryKey();

  /**
   * Compares all column based values in the given entity to the values in this entity instance.
   * Returns true if these two entities contain values for the same columns and all the values are equal.
   * @param entity the entity to compare to
   * @return true if all the column based values in this entity instance are present and equal to the values in the given entity
   * @throws IllegalArgumentException in case the entity is not of the same type
   */
  boolean columnValuesEqual(Entity entity);

  /**
   * After a call to this method this Entity contains the same values and original values as the source entity.
   * A null argument to this method clears this entity instance of all values and original values.
   * @param entity the entity to copy or null for clearing all values in this instance
   * @return the affected attributes and their previous values, that is, attributes which values changed
   */
  Map<Attribute<?>, Object> setAs(Entity entity);

  /**
   * Copies this entity.
   * @return a copy of this entity
   */
  Entity copy();

  /**
   * Returns a new {@link Builder} instance initialized with the values from this entity.
   * @return a {@link Builder} instance.
   */
  Builder copyBuilder();

  /**
   * Copies this entity, with new copied instances of all foreign key value entities.
   * @return a deep copy of this entity
   */
  Entity deepCopy();

  /**
   * Casts this entity to the given type.
   * @param entityType the entity type
   * @param <T> the type
   * @return a typed entity
   * @throws IllegalArgumentException in case this entity is not of the given type
   */
  <T extends Entity> T castTo(final EntityType<T> entityType);

  /**
   * Returns true if the entity referenced via the given foreign key attribute has been loaded
   * @param foreignKey the attribute
   * @return true if the reference entity has been loaded
   */
  boolean isLoaded(ForeignKey foreignKey);

  /**
   * Returns the primary key of this entity.
   * If the entity has no primary key attribute defined, this key contains no values.
   * @return the primary key of this entity
   */
  Key getPrimaryKey();

  /**
   * Returns the primary key of this entity, in its original state.
   * If the entity has no primary key attributes defined, this key contains no values.
   * @return the primary key of this entity in its original state
   */
  Key getOriginalPrimaryKey();

  /**
   * Returns an unmodifiable view of the entries in this Entity, note that
   * attributes based on derived values are not included.
   * @return an unmodifiable view of the entries in this Entity
   */
  Set<Map.Entry<Attribute<?>, Object>> entrySet();

  /**
   * @return an unmodifiable view of the original entries values in this Entity, that is,
   * the original values of attributes that have been modified
   */
  Set<Map.Entry<Attribute<?>, Object>> originalEntrySet();

  /**
   * A builder for {@link Entity} instances.
   */
  interface Builder {

    /**
     * Adds the given attribute value to this builder
     * @param attribute the attribute
     * @param value the value
     * @param <T> the value type
     * @return this builder instance
     */
    <T> Builder with(Attribute<T> attribute, T value);

    /**
     * Builds the entity instance
     * @return a new Entity instance
     */
    Entity build();
  }

  /**
   * Checks if the primary key of any of the given entities is modified
   * @param entities the entities to check
   * @return true if any of the given entities has a modified primary key
   */
  static boolean isKeyModified(final Collection<Entity> entities) {
    return requireNonNull(entities).stream().anyMatch(entity ->
            entity.getPrimaryKey().getAttributes().stream().anyMatch(entity::isModified));
  }

  /**
   * Returns all of the given entities which have been modified
   * @param entities the entities
   * @param <T> the entity type
   * @return a List of entities that have been modified
   * @see Entity#isModified()
   */
  static <T extends Entity> List<T> getModified(final Collection<T> entities) {
    return requireNonNull(entities, "entities").stream().filter(Entity::isModified).collect(toList());
  }

  /**
   * Returns all updatable {@link Attribute}s which value is missing or the original value differs from the one in the comparison
   * entity, returns an empty Collection if all of {@code entity}s original values match the values found in {@code comparison}.
   * Note that only eagerly loaded blob values are included in this comparison.
   * @param definition the entity definition
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @return the updatable column attributes which values differ from the ones in the comparison entity
   * @see BlobProperty#isEagerlyLoaded()
   */
  static List<Attribute<?>> getModifiedColumnAttributes(final EntityDefinition definition, final Entity entity,
                                                        final Entity comparison) {
    requireNonNull(definition);
    requireNonNull(entity);
    requireNonNull(comparison);
    return comparison.entrySet().stream().map(entry -> definition.getProperty(entry.getKey())).filter(property -> {
      final boolean updatableColumnProperty = property instanceof ColumnProperty && ((ColumnProperty<?>) property).isUpdatable();
      final boolean lazilyLoadedBlobProperty = property instanceof BlobProperty && !((BlobProperty) property).isEagerlyLoaded();

      return updatableColumnProperty && !lazilyLoadedBlobProperty && isValueMissingOrModified(entity, comparison, property.getAttribute());
    }).map(Property::getAttribute).collect(toList());
  }

  /**
   * Returns the primary keys of the given entities.
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  static List<Key> getPrimaryKeys(final List<? extends Entity> entities) {
    requireNonNull(entities, "entities");
    final List<Key> keys = new ArrayList<>(entities.size());
    for (int i = 0; i < entities.size(); i++) {
      keys.add(entities.get(i).getPrimaryKey());
    }

    return keys;
  }

  /**
   * Returns the keys referenced by the given foreign key
   * @param entities the entities
   * @param foreignKey the foreign key
   * @return the keys referenced by the given foreign key
   */
  static Set<Key> getReferencedKeys(final List<? extends Entity> entities, final ForeignKey foreignKey) {
    final Set<Key> keySet = new HashSet<>();
    for (int i = 0; i < entities.size(); i++) {
      final Key key = entities.get(i).getReferencedKey(foreignKey);
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
  static List<Key> getOriginalPrimaryKeys(final List<? extends Entity> entities) {
    requireNonNull(entities, "entities");
    final List<Key> keys = new ArrayList<>(entities.size());
    for (int i = 0; i < entities.size(); i++) {
      keys.add(entities.get(i).getOriginalPrimaryKey());
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
  static <T> List<T> get(final Attribute<T> attribute, final Collection<Entity> entities) {
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
  static <T> List<T> getDistinct(final Attribute<T> attribute, final Collection<Entity> entities) {
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
  static <T> List<T> getDistinctIncludingNull(final Attribute<T> attribute, final Collection<Entity> entities) {
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
      previousValues.put(entity.getPrimaryKey(), entity.put(attribute, value));
    }

    return previousValues;
  }

  /**
   * Puts all the values from 'source' into 'destination'.
   * @param destination the destination
   * @param source the source
   * @throws IllegalArgumentException in case the entities are not of the same type
   */
  static void put(final Entity destination, final Entity source) {
    if (!requireNonNull(destination).getEntityType().equals(requireNonNull(source).getEntityType())) {
      throw new IllegalArgumentException("Entities of same type expected");
    }
    source.entrySet().forEach(entry -> destination.put((Attribute<Object>) entry.getKey(), entry.getValue()));
  }

  /**
   * Deep copies the given entities, with new copied instances of all foreign key value entities.
   * @param entities the entities to copy
   * @return a deep copy of the given entities
   */
  static List<Entity> deepCopy(final List<? extends Entity> entities) {
    return requireNonNull(entities, "entities").stream().map(Entity::deepCopy).collect(toList());
  }

  /**
   * Copies the given entities.
   * @param entities the entities to copy
   * @return copies of the given entities, in the same order as they are received
   */
  static List<Entity> copy(final List<? extends Entity> entities) {
    return requireNonNull(entities, "entities").stream().map(Entity::copy).collect(toList());
  }

  /**
   * Casts the given entities to the given type.
   * @param entityType the entity type
   * @param entities the entities
   * @param <T> the type to cast to
   * @return typed entities
   * @throws IllegalArgumentException in case any of the entities is not of the given entity type
   */
  static <T extends Entity> List<T> castTo(final EntityType<T> entityType, final List<Entity> entities) {
    return requireNonNull(entities, "entities").stream().map(entity -> entity.castTo(entityType)).collect(toList());
  }

  /**
   * Maps the given entities to their primary key
   * @param entities the entities to map
   * @return the mapped entities
   */
  static Map<Key, Entity> mapToPrimaryKey(final List<Entity> entities) {
    requireNonNull(entities, "entities");
    final Map<Key, Entity> entityMap = new HashMap<>();
    for (int i = 0; i < entities.size(); i++) {
      final Entity entity = entities.get(i);
      entityMap.put(entity.getPrimaryKey(), entity);
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
  static LinkedHashMap<EntityType<?>, List<Entity>> mapToType(final Collection<? extends Entity> entities) {
    return map(entities, Entity::getEntityType);
  }

  /**
   * Returns a LinkedHashMap containing the given entity keys mapped to their entityTypes,
   * respecting the iteration order of the given collection
   * @param keys the entity keys to map by entityType
   * @return a Map of entity keys mapped to entityType
   */
  static LinkedHashMap<EntityType<?>, List<Key>> mapKeysToType(final Collection<Key> keys) {
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
   * Finds entities according to attribute values
   * @param entities the entities to search
   * @param values the attribute values to use as condition mapped to their respective attributes
   * @return the entities having the exact same attribute values as in the given value map
   */
  static List<Entity> getByValue(final Collection<Entity> entities, final Map<Attribute<?>, Object> values) {
    requireNonNull(values);
    return requireNonNull(entities).stream().filter(entity ->
            values.entrySet().stream().allMatch(entry ->
                    Objects.equals(entity.get(entry.getKey()), entry.getValue()))).collect(toList());
  }

  /**
   * Returns true if all attribute values are equal in the given entities.
   * @param entityOne the first entity
   * @param entityTwo the second entity
   * @return true if the values of the given attributes are equal in the given entities
   */
  static boolean valuesEqual(final Entity entityOne, final Entity entityTwo) {
    if (entityOne.entrySet().size() != entityTwo.entrySet().size()) {
      return false;
    }

    return valuesEqual(entityOne, entityTwo, entityOne.entrySet().stream().map(Map.Entry::getKey).toArray(Attribute[]::new));
  }

  /**
   * Returns true if the values of the given attributes are equal in the given entities.
   * @param entityOne the first entity
   * @param entityTwo the second entity
   * @param attributes the attributes which values to compare
   * @return true if the values of the given attributes are equal in the given entities
   */
  static boolean valuesEqual(final Entity entityOne, final Entity entityTwo, final Attribute<?>... attributes) {
    if (!requireNonNull(entityOne).getEntityType().equals(requireNonNull(entityTwo).getEntityType())) {
      throw new IllegalArgumentException("Type mismatch: " + entityOne.getEntityType() + " - " + entityTwo.getEntityType());
    }
    if (requireNonNull(attributes).length == 0) {
      throw new IllegalArgumentException("No attributes provided for equality check");
    }

    return Arrays.stream(attributes).allMatch(attribute -> {
      if (attribute.isByteArray()) {
        return Arrays.equals((byte[]) entityOne.get(attribute), (byte[]) entityTwo.get(attribute));
      }

      return Objects.equals(entityOne.get(attribute), entityTwo.get(attribute));
    });
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
    if (!entity.contains(attribute)) {
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
