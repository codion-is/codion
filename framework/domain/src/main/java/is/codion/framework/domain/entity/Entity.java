/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

/**
 * Represents a row in a table or query.
 * Helper class for working with Entity instances and related classes.
 * @see EntityDefinition#entity()
 * @see Entities#entity(EntityType)
 * @see Entities#builder(EntityType)
 * @see #entity(Key)
 * @see #builder(Key)
 * @see #copyBuilder()
 */
public interface Entity extends Comparable<Entity> {

  /**
   * @return the entity type
   */
  EntityType type();

  /**
   * @return the entity definition
   */
  EntityDefinition definition();

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
   * @param <T> the value type
   * @return a String representation of the value associated with {@code attribute}
   */
  <T> String toString(Attribute<T> attribute);

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
   * Returns true if this Entity contains a non-null value mapped to the given attribute
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
  Entity referencedEntity(ForeignKey foreignKey);

  /**
   * Returns the primary key of the entity referenced by the given {@link Attribute},
   * if the reference is null this method returns null.
   * @param foreignKey the foreign key for which to retrieve the underlying {@link Key}
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  Key referencedKey(ForeignKey foreignKey);

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
   * @throws IllegalArgumentException in case the entity is not of the same type
   */
  Map<Attribute<?>, Object> setAs(Entity entity);

  /**
   * Copies this entity.
   * @return a copy of this entity
   */
  Entity copy();

  /**
   * Returns a new {@link Builder} instance initialized with the values and original values from this entity.
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
   * @param entityClass the entity class to cast to
   * @param <T> the entity class type
   * @return a typed entity
   * @throws IllegalArgumentException in case the given class has not been associated with the underlying {@link EntityType}.
   */
  <T extends Entity> T castTo(Class<T> entityClass);

  /**
   * Returns true if the given foreign key references a non-null entity and that entity instance has been loaded
   * @param foreignKey the attribute
   * @return true if the referenced entity has been loaded
   */
  boolean isLoaded(ForeignKey foreignKey);

  /**
   * Returns the primary key of this entity.
   * If the entity has no primary key attribute defined, this key contains no values.
   * @return the primary key of this entity
   */
  Key primaryKey();

  /**
   * Returns the primary key of this entity, in its original state.
   * If the entity has no primary key attributes defined, this key contains no values.
   * @return the primary key of this entity in its original state
   */
  Key originalPrimaryKey();

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
   * <pre>
   * Store domain = new Store();
   *
   * Entities entities = domain.entities();
   *
   * Entity customer = entities.builder(Customer.TYPE)
   *     .with(Customer.FIRST_NAME, "John")
   *     .with(Customer.LAST_NAME, "Doe")
   *     .build();
   * </pre>
   * @see Entities#builder(EntityType)
   * @see Entity#builder(Key)
   * @see Entity#copyBuilder()
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
     * Sets the default value for all attributes.
     * @return this builder instance
     * @see Property#defaultValue()
     */
    Builder withDefaultValues();

    /**
     * Builds the Entity instance
     * @return a new Entity instance
     */
    Entity build();
  }

  /**
   * @param key the key
   * @return an Entity instance based on the given key
   */
  static Entity entity(Key key) {
    return new DefaultEntity(key);
  }

  /**
   * @param key the key
   * @return a builder instance based on the given key
   */
  static Builder builder(Key key) {
    return new DefaultEntityBuilder(key);
  }

  /**
   * Checks if any of the primary keys of the given entities is modified
   * @param entities the entities to check
   * @return true if any of the given entities has a modified primary key
   */
  static boolean isKeyModified(Collection<Entity> entities) {
    return requireNonNull(entities).stream()
            .anyMatch(entity -> entity.primaryKey().attributes().stream()
                    .anyMatch(entity::isModified));
  }

  /**
   * Returns the entities which have been modified
   * @param entities the entities
   * @param <T> the entity type
   * @return a List of entities that have been modified
   * @see Entity#isModified()
   */
  static <T extends Entity> List<T> getModified(Collection<T> entities) {
    return requireNonNull(entities).stream()
            .filter(Entity::isModified)
            .collect(toList());
  }

  /**
   * Returns all updatable {@link Attribute}s which value is missing or the original value differs from the one in the comparison
   * entity, returns an empty Collection if all of {@code entity}s original values match the values found in {@code comparison}.
   * Note that only eagerly loaded blob values are included in this comparison.
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @return the updatable column attributes which values differ from the ones in the comparison entity
   * @see BlobProperty#isEagerlyLoaded()
   */
  static Collection<Attribute<?>> getModifiedColumnAttributes(Entity entity, Entity comparison) {
    requireNonNull(entity);
    requireNonNull(comparison);
    return comparison.entrySet().stream()
            .map(entry -> entity.definition().property(entry.getKey()))
            .filter(property -> {
              boolean updatableColumnProperty = property instanceof ColumnProperty && ((ColumnProperty<?>) property).isUpdatable();
              boolean lazilyLoadedBlobProperty = property instanceof BlobProperty && !((BlobProperty) property).isEagerlyLoaded();

              return updatableColumnProperty && !lazilyLoadedBlobProperty && isValueMissingOrModified(entity, comparison, property.attribute());
            })
            .map(Property::attribute)
            .collect(toList());
  }

  /**
   * Returns the primary keys of the given entities.
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  static List<Key> getPrimaryKeys(Collection<? extends Entity> entities) {
    return requireNonNull(entities).stream()
            .map(Entity::primaryKey)
            .collect(toList());
  }

  /**
   * Returns the keys referenced by the given foreign key
   * @param entities the entities
   * @param foreignKey the foreign key
   * @return the keys referenced by the given foreign key
   */
  static Set<Key> getReferencedKeys(Collection<? extends Entity> entities, ForeignKey foreignKey) {
    return requireNonNull(entities).stream()
            .map(entity -> entity.referencedKey(foreignKey))
            .filter(Objects::nonNull)
            .collect(toSet());
  }

  /**
   * Returns the primary keys of the given entities with their original values.
   * @param entities the entities
   * @return a Collection containing the primary keys of the given entities with their original values
   */
  static Collection<Key> getOriginalPrimaryKeys(Collection<? extends Entity> entities) {
    return requireNonNull(entities).stream()
            .map(Entity::originalPrimaryKey)
            .collect(toList());
  }

  /**
   * Retrieves the values of the given keys, assuming they are single column keys.
   * @param <T> the value type
   * @param keys the keys
   * @return the attribute values of the given keys
   */
  static <T> Collection<T> getValues(Collection<Key> keys) {
    return requireNonNull(keys).stream()
            .map(key -> (T) key.get())
            .collect(toList());
  }

  /**
   * Returns the non-null values associated with the given attribute from the given entities.
   * @param <T> the value type
   * @param attribute the attribute which values to retrieve
   * @param entities the entities from which to retrieve the attribute value
   * @return the non-null values of the given attribute from the given entities.
   */
  static <T> Collection<T> get(Attribute<T> attribute, Collection<Entity> entities) {
    requireNonNull(attribute, "attribute");
    return requireNonNull(entities).stream()
            .map(entity -> entity.get(attribute))
            .filter(Objects::nonNull)
            .collect(toList());
  }

  /**
   * Returns the values associated with the given attribute from the given entities.
   * @param <T> the value type
   * @param attribute the attribute which values to retrieve
   * @param entities the entities from which to retrieve the attribute value
   * @return the values of the given attributes from the given entities, including null values.
   */
  static <T> Collection<T> getIncludingNull(Attribute<T> attribute, Collection<Entity> entities) {
    requireNonNull(attribute, "attribute");
    return requireNonNull(entities).stream()
            .map(entity -> entity.get(attribute))
            .collect(toList());
  }

  /**
   * Returns the distinct non-null values of {@code attribute} from the given entities.
   * @param <T> the value type
   * @param attribute the attribute which values to retrieve
   * @param entities the entities from which to retrieve the values
   * @return the distinct non-null values of the given attribute from the given entities.
   */
  static <T> Collection<T> getDistinct(Attribute<T> attribute, Collection<Entity> entities) {
    requireNonNull(attribute, "attribute");
    return requireNonNull(entities).stream()
            .map(entity -> entity.get(attribute))
            .filter(Objects::nonNull)
            .collect(toSet());
  }

  /**
   * Returns the distinct values of {@code attribute} from the given entities, including null.
   * @param <T> the value type
   * @param attribute the attribute which values to retrieve
   * @param entities the entities from which to retrieve the values
   * @return the distinct values of the given attribute from the given entities, may contain null.
   */
  static <T> Collection<T> getDistinctIncludingNull(Attribute<T> attribute, Collection<Entity> entities) {
    requireNonNull(attribute, "attribute");
    return requireNonNull(entities).stream()
            .map(entity -> entity.get(attribute))
            .collect(toSet());
  }

  /**
   * Sets the value of the given attribute to the given value in the given entities
   * @param attribute the attribute for which to set the value
   * @param value the value
   * @param entities the entities for which to set the value
   * @param <T> the value type
   * @return the previous attribute values mapped to the primary key of the entity
   */
  static <T> Map<Key, T> put(Attribute<T> attribute, T value, Collection<Entity> entities) {
    requireNonNull(attribute, "attribute");
    Map<Key, T> previousValues = new HashMap<>(requireNonNull(entities).size());
    for (Entity entity : entities) {
      previousValues.put(entity.primaryKey(), entity.put(attribute, value));
    }

    return previousValues;
  }

  /**
   * Puts all the values from the source entity into the destination entity.
   * @param destination the destination
   * @param source the source
   * @throws IllegalArgumentException in case the entities are not of the same type
   */
  static void put(Entity destination, Entity source) {
    if (!requireNonNull(destination).type().equals(requireNonNull(source).type())) {
      throw new IllegalArgumentException("Entities of same type expected");
    }
    source.entrySet().forEach(entry -> destination.put((Attribute<Object>) entry.getKey(), entry.getValue()));
  }

  /**
   * Deep copies the given entities, with new copied instances of all foreign key value entities.
   * @param entities the entities to copy
   * @return a deep copy of the given entities
   */
  static List<Entity> deepCopy(List<? extends Entity> entities) {
    return requireNonNull(entities).stream()
            .map(Entity::deepCopy)
            .collect(toList());
  }

  /**
   * Copies the given entities.
   * @param entities the entities to copy
   * @return copies of the given entities, in the same order as they are received
   */
  static List<Entity> copy(List<? extends Entity> entities) {
    return requireNonNull(entities).stream()
            .map(Entity::copy)
            .collect(toList());
  }

  /**
   * Casts the given entities to the given type.
   * @param entityClass the entity class to cast to
   * @param entities the entities
   * @param <T> the entity class type
   * @return typed entities
   * @throws IllegalArgumentException in case any of the entities is not of the given entity type
   */
  static <T extends Entity> List<T> castTo(Class<T> entityClass, List<Entity> entities) {
    return requireNonNull(entities).stream()
            .map(entity -> entity.castTo(entityClass))
            .collect(toList());
  }

  /**
   * Maps the given entities to their primary key
   * @param entities the entities to map
   * @return the mapped entities
   */
  static Map<Key, Entity> mapToPrimaryKey(Collection<Entity> entities) {
    return requireNonNull(entities).stream()
            .collect(toMap(Entity::primaryKey, Function.identity()));
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to the value of {@code attribute},
   * respecting the iteration order of the given collection
   * @param <T> the key type
   * @param attribute the attribute which value should be used for mapping
   * @param entities the entities to map by attribute value
   * @return a Map of entities mapped to attribute value
   */
  static <T> LinkedHashMap<T, List<Entity>> mapToValue(Attribute<T> attribute, Collection<Entity> entities) {
    return requireNonNull(entities).stream()
            .collect(groupingBy(entity -> entity.get(attribute), LinkedHashMap::new, toList()));
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to their entityTypes,
   * respecting the iteration order of the given collection
   * @param entities the entities to map by entityType
   * @return a Map of entities mapped to entityType
   */
  static LinkedHashMap<EntityType, List<Entity>> mapToType(Collection<? extends Entity> entities) {
    return requireNonNull(entities).stream()
            .collect(groupingBy(Entity::type, LinkedHashMap::new, toList()));
  }

  /**
   * Returns a LinkedHashMap containing the given entity keys mapped to their entityTypes,
   * respecting the iteration order of the given collection
   * @param keys the entity keys to map by entityType
   * @return a Map of entity keys mapped to entityType
   */
  static LinkedHashMap<EntityType, List<Key>> mapKeysToType(Collection<Key> keys) {
    return requireNonNull(keys).stream()
            .collect(groupingBy(Key::type, LinkedHashMap::new, toList()));
  }

  /**
   * Creates a two-dimensional list containing the values of the given attributes for the given entities in string format.
   * @param attributes the attributes
   * @param entities the entities
   * @return the values of the given attributes from the given entities in a two-dimensional list
   */
  static List<List<String>> getValuesAsString(List<Attribute<?>> attributes, List<Entity> entities) {
    requireNonNull(attributes);
    return requireNonNull(entities).stream()
            .map(entity -> attributes.stream()
                    .map(entity::toString)
                    .collect(toList()))
            .collect(toList());
  }

  /**
   * Finds entities according to attribute values
   * @param values the attribute values to use as condition mapped to their respective attributes
   * @param entities the entities to search
   * @return the entities having the exact same attribute values as in the given value map
   */
  static Collection<Entity> getByValue(Map<Attribute<?>, Object> values, Collection<Entity> entities) {
    requireNonNull(values);
    return requireNonNull(entities).stream()
            .filter(entity -> values.entrySet().stream()
                    .allMatch(entry -> Objects.equals(entity.get(entry.getKey()), entry.getValue())))
            .collect(toList());
  }

  /**
   * Returns true if all attribute values are equal in the given entities.
   * @param entityOne the first entity
   * @param entityTwo the second entity
   * @return true if the values of the given attributes are equal in the given entities
   */
  static boolean valuesEqual(Entity entityOne, Entity entityTwo) {
    if (requireNonNull(entityOne).entrySet().size() != requireNonNull(entityTwo).entrySet().size()) {
      return false;
    }

    return valuesEqual(entityOne, entityTwo, entityOne.entrySet().stream()
            .map(Map.Entry::getKey)
            .toArray(Attribute[]::new));
  }

  /**
   * Returns true if the values of the given attributes are equal in the given entities.
   * @param entityOne the first entity
   * @param entityTwo the second entity
   * @param attributes the attributes which values to compare
   * @return true if the values of the given attributes are equal in the given entities
   */
  static boolean valuesEqual(Entity entityOne, Entity entityTwo, Attribute<?>... attributes) {
    if (!requireNonNull(entityOne).type().equals(requireNonNull(entityTwo).type())) {
      throw new IllegalArgumentException("Type mismatch: " + entityOne.type() + " - " + entityTwo.type());
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
  static <T> boolean isValueMissingOrModified(Entity entity, Entity comparison, Attribute<T> attribute) {
    requireNonNull(entity);
    requireNonNull(comparison);
    requireNonNull(attribute);
    if (!entity.contains(attribute)) {
      return true;
    }

    T originalValue = entity.getOriginal(attribute);
    T comparisonValue = comparison.get(attribute);
    if (attribute.isByteArray()) {
      return !Arrays.equals((byte[]) originalValue, (byte[]) comparisonValue);
    }

    return !Objects.equals(originalValue, comparisonValue);
  }
}
