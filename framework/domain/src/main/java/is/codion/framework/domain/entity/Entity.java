/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

/**
 * Represents a row in a table or query result, encapsulating the data and state of a domain entity.
 * <p>
 * An Entity instance maintains both current and original values for all attributes, tracks modifications,
 * and provides type-safe access to column values and foreign key relationships.
 * <p>
 * Entity instances can be mutable or immutable. Mutable entities track value modifications and
 * support operations like {@link #set(Attribute, Object)}, {@link #revert()}, and {@link #save()}.
 * Immutable entities throw {@link UnsupportedOperationException} for modification operations.
 *
 * <h2>Thread Safety</h2>
 * <b>Mutable entities are NOT thread-safe.</b> They should not be shared between threads without
 * external synchronization. Concurrent modifications may result in data corruption or inconsistent state.
 * <p>
 * <b>Immutable entities ARE thread-safe.</b> Created via {@link #immutable()}, they can be safely
 * shared between threads. All referenced entities are also made immutable to ensure complete thread safety.
 * <p>
 * <b>Recommended patterns for concurrent use:</b>
 * <ul>
 *   <li>Use {@link #immutable()} to create thread-safe snapshots for sharing</li>
 *   <li>Implement external synchronization when sharing mutable entities</li>
 *   <li>Consider using immutable entities for read-only operations across threads</li>
 *   <li>Create defensive copies with {@link #copy()} when passing entities between threads</li>
 * </ul>
 * <p>
 * {@snippet :
 * // Creating and working with entities
 * Entity customer = entities.builder(Customer.TYPE)
 *     .with(Customer.ID, 42)
 *     .with(Customer.NAME, "John Doe")
 *     .with(Customer.EMAIL, "john@example.com")
 *     .build();
 *
 * // Accessing values
 * String name = customer.get(Customer.NAME);
 * Optional<String> email = customer.optional(Customer.EMAIL);
 *
 * // Modifying values (if mutable)
 * customer.set(Customer.EMAIL, "newemail@example.com");
 * boolean isModified = customer.modified(Customer.EMAIL); // true
 *
 * // Reverting changes
 * customer.revert(Customer.EMAIL); // back to "john@example.com"
 *}
 * <p>
 * {@snippet :
 * // Working with foreign keys
 * Entity invoice = connection.selectSingle(Invoice.ID.equalTo(123));
 *
 * // Access the referenced entity (automatically loaded if configured)
 * Entity customer = invoice.get(Invoice.CUSTOMER_FK);
 *
 * // Access foreign key attributes directly
 * String customerName = invoice.get(Invoice.CUSTOMER_FK).get(Customer.NAME);
 *
 * // Get the foreign key value
 * Key customerKey = invoice.key(Invoice.CUSTOMER_FK);
 *}
 * @see EntityDefinition#entity()
 * @see Entities#entity(EntityType)
 * @see Entities#builder(EntityType)
 * @see #entity(Key)
 * @see #builder(Key)
 * @see #copy()
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
	 * Sets the value of the given attribute, returning the previous value if any
	 * @param attribute the attribute
	 * @param value the value
	 * @param <T> the value type
	 * @return the previous value
	 * @throws UnsupportedOperationException in case this entity is immutable
	 */
	@Nullable
	<T> T set(Attribute<T> attribute, @Nullable T value);

	/**
	 * Returns the value associated with {@code attribute}.
	 * @param attribute the attribute for which to retrieve the value
	 * @param <T> the value type
	 * @return the value of the given attribute
	 */
	@Nullable <T> T get(Attribute<T> attribute);

	/**
	 * Returns the value associated with {@code attribute}, wrapped in an {@link Optional}.
	 * @param attribute the attribute which value to retrieve
	 * @param <T> the value type
	 * @return the value of the given attribute, wrapped in an {@link Optional}
	 */
	<T> Optional<T> optional(Attribute<T> attribute);

	/**
	 * Returns the original value associated with {@code attribute}, or the current one if it is unmodified.
	 * @param attribute the attribute for which to retrieve the original value
	 * @param <T> the value type
	 * @return the original value of the given attribute
	 */
	@Nullable <T> T original(Attribute<T> attribute);

	/**
	 * This method returns a String representation of the value associated with the given attribute,
	 * if the associated attribute has a format it is used.
	 * @param attribute the attribute which value to retrieve
	 * @param <T> the value type
	 * @return a String representation of the value associated with {@code attribute}
	 */
	<T> String string(Attribute<T> attribute);

	/**
	 * Reverts the value associated with the given attribute to its original value.
	 * If the value has not been modified then calling this method has no effect.
	 * @param attribute the attribute for which to revert the value
	 * @throws UnsupportedOperationException in case this entity is immutable
	 */
	void revert(Attribute<?> attribute);

	/**
	 * Reverts all value modifications that have been made.
	 * This entity will be unmodified after a call to this method.
	 * If no modifications have been made then calling this method has no effect.
	 * @throws UnsupportedOperationException in case this entity is immutable
	 */
	void revert();

	/**
	 * Saves the value associated with the given attribute, that is, removes the original value.
	 * If the value has not been modified calling this method has no effect.
	 * @param attribute the attribute for which to save the value
	 * @throws UnsupportedOperationException in case this entity is immutable
	 */
	void save(Attribute<?> attribute);

	/**
	 * Saves all the value modifications that have been made, that is, removes all original values.
	 * This entity will be unmodified after a call to this method.
	 * @throws UnsupportedOperationException in case this entity is immutable
	 * @see #modified()
	 */
	void save();

	/**
	 * Removes the given value from this Entity along with the original value if any.
	 * If no value is mapped to the given attribute, this method has no effect.
	 * @param attribute the attribute to remove
	 * @param <T> the value type
	 * @return the previous value mapped to the given attribute
	 * @throws UnsupportedOperationException in case this entity is immutable
	 */
	@Nullable <T> T remove(Attribute<T> attribute);

	/**
	 * Returns true if a null value is mapped to the given attribute or if no mapping is found.
	 * In case of foreign keys the value of the underlying reference column(s) is checked.
	 * @param attribute the attribute
	 * @return true if the value mapped to the given attribute is null or no value is mapped
	 */
	boolean isNull(Attribute<?> attribute);

	/**
	 * Returns true if this Entity contains a value for the given attribute, that value can be null.
	 * @param attribute the attribute
	 * @return true if a value is mapped to this attribute
	 */
	boolean contains(Attribute<?> attribute);

	/**
	 * Returns the Entity instance referenced by the given {@link ForeignKey}.
	 * If the underlying reference contains a value, that is,
	 * a foreign key value exists but the actual referenced entity has not
	 * been loaded, an "empty" entity is returned, containing only the referenced
	 * key value(s). Null is returned only if the actual foreign key is null.
	 * {@snippet :
	 * // Assuming Invoice has a foreign key to Customer
	 * Entity invoice = connection.selectSingle(Invoice.ID.equalTo(42));
	 *
	 * // Get the customer entity - may be fully loaded or just contain the key
	 * Entity customer = invoice.entity(Invoice.CUSTOMER_FK);
	 *
	 * if (customer != null) {
	 *     // This is always available - the foreign key value
	 *     Integer customerId = customer.get(Customer.ID);
	 *
	 *     // This may return null if customer wasn't loaded
	 *     // and the foreign key definition doesn't include Customer.NAME
	 *     String customerName = customer.get(Customer.NAME);
	 * }
	 *}
	 * @param foreignKey the foreign key for which to retrieve the referenced entity
	 * @return the entity associated with {@code foreignKey}
	 */
	@Nullable Entity entity(ForeignKey foreignKey);

	/**
	 * Returns the key referenced by the given {@link ForeignKey},
	 * if the reference is null this method returns null.
	 * @param foreignKey the foreign key for which to retrieve the underlying {@link Key}
	 * @return the key for the underlying entity, null if no entity is referenced
	 */
	@Nullable Key key(ForeignKey foreignKey);

	/**
	 * Returns true if the value associated with the given attribute has been modified since first set,
	 * note that this does not apply to attributes based on derived values.
	 * {@snippet :
	 * Entity customer = entities.builder(Customer.TYPE)
	 *     .with(Customer.NAME, "John")
	 *     .with(Customer.EMAIL, "john@example.com")
	 *     .build();
	 *
	 * customer.modified(Customer.NAME); // false
	 *
	 * customer.set(Customer.NAME, "Jane");
	 * customer.modified(Customer.NAME); // true
	 *
	 * customer.save();
	 * customer.modified(Customer.NAME); // false
	 *}
	 * @param attribute the attribute
	 * @return true if the value associated with the given attribute has been modified
	 */
	boolean modified(Attribute<?> attribute);

	/**
	 * Returns true if one or more writable attributes have been modified from their initial value,
	 * non-insertable and non-updatable attributes are excluded unless they are transient and modify the entity.
	 * @return true if one or more writable attributes have been modified since they were first set
	 * @see TransientAttributeDefinition#modifies()
	 */
	boolean modified();

	/**
	 * @return true if this entity has been persisted
	 * @see EntityDefinition.Builder#exists(Predicate)
	 */
	boolean exists();

	/**
	 * Compares the values of all attributes in the given entity to the values in this entity instance.
	 * Returns true if all attribute values available in this entity are available and equal in the comparison entity
	 * {@snippet :
	 * Entity customer1 = entities.builder(Customer.TYPE)
	 *     .with(Customer.ID, 42)
	 *     .with(Customer.NAME, "John Doe")
	 *     .with(Customer.EMAIL, "john@example.com")
	 *     .build();
	 *
	 * Entity customer2 = entities.builder(Customer.TYPE)
	 *     .with(Customer.ID, 42)
	 *     .with(Customer.NAME, "John Doe")
	 *     .with(Customer.EMAIL, "john@example.com")
	 *     .with(Customer.PHONE, "555-1234") // Extra attribute
	 *     .build();
	 *
	 * customer1.equalValues(customer2); // true - all values in customer1 exist and are equal in customer2
	 * customer2.equalValues(customer1); // false - customer2 has PHONE which customer1 doesn't have
	 *}
	 * @param entity the entity to compare to
	 * @return true if all values in this entity instance are present and equal to the values in the given entity
	 * @throws IllegalArgumentException in case the entity is not of the same type
	 */
	boolean equalValues(Entity entity);

	/**
	 * Compares the values of the given attributes in the given entity to the values in this entity instance.
	 * Returns true if these two entities contain values for the given attributes and all the values are equal.
	 * {@snippet :
	 * Entity customer1 = entities.builder(Customer.TYPE)
	 *     .with(Customer.ID, 42)
	 *     .with(Customer.NAME, "John Doe")
	 *     .with(Customer.EMAIL, "john@example.com")
	 *     .build();
	 *
	 * Entity customer2 = entities.builder(Customer.TYPE)
	 *     .with(Customer.ID, 42)
	 *     .with(Customer.NAME, "John Doe")
	 *     .with(Customer.EMAIL, "different@example.com")
	 *     .build();
	 *
	 * // Compare only specific attributes
	 * Set<Attribute<?>> nameAttributes = Set.of(Customer.ID, Customer.NAME);
	 * customer1.equalValues(customer2, nameAttributes); // true - ID and NAME are equal
	 *
	 * Set<Attribute<?>> allAttributes = Set.of(Customer.ID, Customer.NAME, Customer.EMAIL);
	 * customer1.equalValues(customer2, allAttributes); // false - EMAIL differs
	 *}
	 * @param entity the entity to compare to
	 * @param attributes the attributes to compare
	 * @return true if all the given values in this entity instance are present and equal to the values in the given entity
	 * @throws IllegalArgumentException in case the entity is not of the same type
	 */
	boolean equalValues(Entity entity, Collection<? extends Attribute<?>> attributes);

	/**
	 * After a call to this method this Entity contains the same values and original values as the source entity.
	 * A null argument to this method clears this entity instance of all values and original values.
	 * @param entity the entity to copy or null for clearing all values in this instance
	 * @return the affected attributes and their previous values, that is, attributes which values changed
	 * @throws IllegalArgumentException in case the entity is not of the same type
	 * @throws UnsupportedOperationException in case this entity is immutable
	 */
	Map<Attribute<?>, Object> set(@Nullable Entity entity);

	/**
	 * Returns a {@link Copy} instance providing ways to create copies of this entity.
	 * {@snippet :
	 * Entity customer = entities.builder(Customer.TYPE)
	 *     .with(Customer.ID, 42)
	 *     .with(Customer.NAME, "John Doe")
	 *     .with(Customer.EMAIL, "john@example.com")
	 *     .build();
	 *
	 * // Create a mutable copy
	 * Entity mutableCopy = customer.copy().mutable();
	 * mutableCopy.set(Customer.EMAIL, "new@example.com");
	 *
	 * // Original remains unchanged
	 * customer.get(Customer.EMAIL); // "john@example.com"
	 * mutableCopy.get(Customer.EMAIL); // "new@example.com"
	 *
	 * // Create a builder initialized with entity values
	 * Entity newCustomer = customer.copy().builder()
	 *     .with(Customer.ID, 43) // Different ID
	 *     .with(Customer.PHONE, "555-1234") // Additional field
	 *     .build();
	 *}
	 * @return a {@link Copy} instance for this entity
	 */
	Copy copy();

	/**
	 * Returns an immutable version of this entity, all foreign key entities are also immutable.
	 * Note that this may be the same instance in case this instance is already immutable.
	 * <p>
	 * <b>Thread Safety:</b> The returned immutable entity is thread-safe and can be safely
	 * shared between threads without external synchronization. All referenced entities
	 * are also made immutable to ensure complete thread safety throughout the object graph.
	 * <p>
	 * This method is particularly useful for:
	 * <ul>
	 *   <li>Creating thread-safe snapshots for sharing between threads</li>
	 *   <li>Ensuring data integrity in concurrent read operations</li>
	 *   <li>Caching entities safely in multi-threaded environments</li>
	 * </ul>
	 * @return an immutable, thread-safe version of this entity
	 */
	Entity immutable();

	/**
	 * Returns whether this entity instance is mutable (can be modified).
	 * <p>
	 * <b>Thread Safety:</b> Mutable entities (returning {@code true}) are NOT thread-safe
	 * and should not be shared between threads without external synchronization.
	 * Immutable entities (returning {@code false}) are thread-safe and can be safely
	 * shared between threads.
	 * @return true if this is a mutable instance, false if immutable
	 * @see #immutable()
	 */
	boolean mutable();

	/**
	 * Returns the primary key of this entity.
	 * If the entity has no defined primary key, this key contains
	 * all column values and {@link Key#primary()} returns false.
	 * @return the primary key of this entity
	 * @see Key#primary()
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
	 * Provides ways to create copies of an entity instance.
	 * <ul>
	 *   <li>{@link #mutable()} returns a mutable copy
	 *   <li>{@link #builder()} returns a {@link Builder} instance initialized with the values of the entity being copied
	 * </ul>
	 */
	interface Copy {

		/**
		 * Returns a mutable copy of this entity.
		 * @return a mutable copy of this entity
		 */
		Entity mutable();

		/**
		 * Returns a new {@link Builder} instance initialized with the values and original values from this entity.
		 * @return a {@link Builder} instance.
		 */
		Builder builder();
	}

	/**
	 * A builder for {@link Entity} instances.
	 * {@snippet :
	 * Store domain = new Store();
	 *
	 * Entities entities = domain.entities();
	 *
	 * Entity customer = entities.builder(Customer.TYPE)
	 *     .with(Customer.FIRST_NAME, "John")
	 *     .with(Customer.LAST_NAME, "Doe")
	 *     .build();
	 *}
	 * @see Entities#builder(EntityType)
	 * @see Entity#builder(Key)
	 * @see Copy#builder()
	 */
	interface Builder {

		/**
		 * Adds the given attribute value to this builder
		 * @param attribute the attribute
		 * @param value the value
		 * @param <T> the value type
		 * @return this builder instance
		 */
		<T> Builder with(Attribute<T> attribute, @Nullable T value);

		/**
		 * Sets the default value for all attributes which have a default value.
		 * @return this builder instance
		 * @see AttributeDefinition#defaultValue()
		 * @see AttributeDefinition#hasDefaultValue()
		 */
		Builder withDefaults();

		/**
		 * Clears the primary key values from this builder,
		 * current as well as original values if any
		 * @return this builder instance
		 */
		Builder clearPrimaryKey();

		/**
		 * Resets the primary to its original state,
		 * in case the original value is available
		 * @return this builder instance
		 */
		Builder originalPrimaryKey();

		/**
		 * Creates a new {@link Entity.Key.Builder} instance, initialized with
		 * any primary key values found in this builder.
		 * @return a new {@link Key.Builder}
		 */
		Key.Builder key();

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
	 * Returns the primary keys of the given entities.
	 * @param entities the entities
	 * @return a {@link Collection} containing the primary keys of the given entities
	 */
	static Collection<Key> primaryKeys(Collection<Entity> entities) {
		return requireNonNull(entities).stream()
						.map(Entity::primaryKey)
						.collect(toList());
	}

	/**
	 * Returns the non-null keys referenced by the given {@link ForeignKey}
	 * @param foreignKey the foreign key
	 * @param entities the entities
	 * @return a {@link Collection} containing the non-null keys referenced by the given {@link ForeignKey}
	 */
	static Collection<Key> keys(ForeignKey foreignKey, Collection<Entity> entities) {
		return requireNonNull(entities).stream()
						.map(entity -> entity.key(foreignKey))
						.filter(Objects::nonNull)
						.collect(toSet());
	}

	/**
	 * Returns the primary keys of the given entities with their original values.
	 * @param entities the entities
	 * @return a {@link Collection} containing the primary keys of the given entities with their original values
	 */
	static Collection<Key> originalPrimaryKeys(Collection<Entity> entities) {
		return requireNonNull(entities).stream()
						.map(Entity::originalPrimaryKey)
						.collect(toList());
	}

	/**
	 * Retrieves the values of the given keys, assuming they are single column keys.
	 * @param <T> the value type
	 * @param keys the keys
	 * @return the attribute values of the given keys
	 * @throws IllegalStateException in case of a composite key
	 */
	static <T> Collection<T> values(Collection<Key> keys) {
		return requireNonNull(keys).stream()
						.map(key -> (T) key.value())
						.collect(toList());
	}

	/**
	 * Returns the non-null values associated with {@code attribute} from the given entities.
	 * @param <T> the value type
	 * @param attribute the attribute which values to retrieve
	 * @param entities the entities from which to retrieve the attribute value
	 * @return the non-null values of the given attribute from the given entities.
	 */
	static <T> Collection<T> values(Attribute<T> attribute, Collection<Entity> entities) {
		requireNonNull(attribute);
		return requireNonNull(entities).stream()
						.map(entity -> entity.get(attribute))
						.filter(Objects::nonNull)
						.collect(toList());
	}

	/**
	 * Returns the distinct non-null values of {@code attribute} from the given entities.
	 * @param <T> the value type
	 * @param attribute the attribute which values to retrieve
	 * @param entities the entities from which to retrieve the values
	 * @return the distinct non-null values of the given attribute from the given entities.
	 */
	static <T> Collection<T> distinct(Attribute<T> attribute, Collection<Entity> entities) {
		requireNonNull(attribute);
		return requireNonNull(entities).stream()
						.map(entity -> entity.get(attribute))
						.filter(Objects::nonNull)
						.collect(toSet());
	}

	/**
	 * Maps the given entities to their primary key, assuming each entity appears only once in the given collection.
	 * {@snippet :
	 * List<Entity> customers = connection.select(Customer.ID.in(1, 2, 3));
	 *
	 * // Create a map for fast lookup by primary key
	 * Map<Key, Entity> customerMap = Entity.primaryKeyMap(customers);
	 *
	 * // Later, quick lookup by key
	 * Key customerKey = entities.primaryKey(Customer.TYPE, 2);
	 * Entity customer = customerMap.get(customerKey);
	 *}
	 * @param entities the entities to map
	 * @return the mapped entities
	 * @throws IllegalArgumentException in case a non-unique primary key is encountered
	 */
	static Map<Key, Entity> primaryKeyMap(Collection<Entity> entities) {
		return requireNonNull(entities).stream()
						.collect(toMap(Entity::primaryKey, Function.identity(), ThrowIfNonUnique.INSTANCE));
	}

	/**
	 * Returns a {@link LinkedHashMap} containing the given entities mapped to the value of {@code attribute},
	 * respecting the iteration order of the given collection
	 * {@snippet :
	 * List<Entity> orders = connection.select(all(Order.TYPE));
	 *
	 * // Group orders by status
	 * LinkedHashMap<String, List<Entity>> ordersByStatus =
	 *     Entity.groupByValue(Order.STATUS, orders);
	 *
	 * // Process orders by status
	 * ordersByStatus.forEach((status, statusOrders) -> {
	 *     System.out.println("Status: " + status + ", Count: " + statusOrders.size());
	 * });
	 *
	 * // Groups can contain entities with null values
	 * List<Entity> pendingOrders = ordersByStatus.get("PENDING");
	 * List<Entity> nullStatusOrders = ordersByStatus.get(null);
	 *}
	 * @param <T> the key type
	 * @param attribute the attribute which value should be used for mapping
	 * @param entities the entities to map by attribute value
	 * @return a {@link LinkedHashMap} of the given entities mapped to the attribute value
	 */
	static <T> LinkedHashMap<T, List<Entity>> groupByValue(Attribute<T> attribute, Collection<Entity> entities) {
		LinkedHashMap<T, List<Entity>> result = new LinkedHashMap<>();
		requireNonNull(entities).forEach(entity -> result.computeIfAbsent(entity.get(attribute), k -> new ArrayList<>()).add(entity));

		return result;
	}

	/**
	 * Returns a {@link LinkedHashMap} containing the given entities mapped to their entityTypes,
	 * respecting the iteration order of the given collection
	 * @param entities the entities to map by entityType
	 * @return a {@link LinkedHashMap} of the given entities mapped to their {@link EntityType}
	 */
	static LinkedHashMap<EntityType, List<Entity>> groupByType(Collection<Entity> entities) {
		return requireNonNull(entities).stream()
						.collect(groupingBy(Entity::type, LinkedHashMap::new, toList()));
	}

	/**
	 * Represents a unique column combination for a given entity.
	 */
	interface Key {

		/**
		 * @return the entity type
		 */
		EntityType type();

		/**
		 * @return the entity definition
		 */
		EntityDefinition definition();

		/**
		 * Returns the columns comprising this key, in an unspecified order.
		 * For the primary key columns in the correct (indexed) order use
		 * {@link EntityDefinition.PrimaryKey#columns()} via {@link EntityDefinition#primaryKey()}.
		 * @return the columns comprising this key in no particular order
		 */
		Collection<Column<?>> columns();

		/**
		 * Note that this method returns true for empty keys representing entities without a defined primary key
		 * @return true if this key represents a primary key
		 */
		boolean primary();

		/**
		 * @return true if this key contains no values or if it contains a null value for a non-nullable key attribute
		 */
		boolean isNull();

		/**
		 * Returns true if a null value is mapped to the given column or no mapping exists.
		 * @param column the column
		 * @return true if the value mapped to the given column is null or none exists
		 */
		boolean isNull(Column<?> column);

		/**
		 * Returns this keys column. Note that this method throws an exception if this key is a composite key.
		 * @param <T> the column type
		 * @return the key column, useful for single column keys
		 * @throws IllegalStateException in case this is a composite key
		 * @throws NoSuchElementException in case this key contains no values
		 */
		<T> Column<T> column();

		/**
		 * Returns the value of this key. Note that this method throws an exception if this key is a composite key.
		 * @param <T> the value type
		 * @return the first value contained in this key, useful for single attribute keys
		 * @throws IllegalStateException in case this is a composite key
		 * @throws NoSuchElementException in case this key contains no values
		 */
		@Nullable <T> T value();

		/**
		 * Returns the value of this key, wrapped in an {@link Optional}. Note that this method throws an exception if this key is a composite key.
		 * @param <T> the value type
		 * @return the first value contained in this key, wrapped in an {@link Optional}, useful for single attribute keys
		 * @throws IllegalStateException in case this is a composite key
		 * @throws NoSuchElementException in case this key contains no values
		 */
		<T> Optional<T> optional();

		/**
		 * @param column the column
		 * @param <T> the value type
		 * @return the value associated with the given column
		 * @throws IllegalArgumentException in case this column is not part of this key
		 */
		@Nullable <T> T get(Column<T> column);

		/**
		 * @param column the column
		 * @param <T> the value type
		 * @return the value associated with the given column, wrapped in an {@link Optional}
		 * @throws IllegalArgumentException in case this column is not part of this key
		 */
		<T> Optional<T> optional(Column<T> column);

		/**
		 * Creates a new {@link Builder} instance, initialized with the values in this key.
		 * @return a new builder based on this key
		 */
		Builder copy();

		/**
		 * Returns a LinkedHashMap containing the given entity keys mapped to their entityTypes,
		 * respecting the iteration order of the given collection
		 * @param keys the entity keys to map by entityType
		 * @return a Map of entity keys mapped to entityType
		 */
		static LinkedHashMap<EntityType, List<Key>> groupByType(Collection<Key> keys) {
			return requireNonNull(keys).stream()
							.collect(groupingBy(Key::type, LinkedHashMap::new, toList()));
		}

		/**
		 * A builder for {@link Key} instances.
		 * Note that the resulting key is assumed to be a primary key
		 * if any of the values is associated with a primary key column.
		 */
		interface Builder {

			/**
			 * Adds the given column value to this builder
			 * @param column the column
			 * @param value the value
			 * @param <T> the value type
			 * @return this builder instance
			 * @throws IllegalArgumentException in case this column is not part of the entity
			 */
			<T> Builder with(Column<T> column, @Nullable T value);

			/**
			 * Builds the key instance
			 * @return a new Key instance
			 */
			Key build();
		}
	}
}
