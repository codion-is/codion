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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.DomainType;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static is.codion.common.Configuration.booleanValue;

/**
 * A repository containing the {@link EntityDefinition}s for a given domain.
 * Factory for {@link Entity} and {@link Entity.Key} instances.
 * <p>
 * The Entities instance serves as the central registry for all entity types within a domain,
 * providing access to entity definitions and factory methods for creating entity instances and keys.
 * <p>
 * Typically accessed through a domain instance:
 * {@snippet :
 * // Define a domain
 * public class Store extends DefaultDomain {
 *     public static final DomainType DOMAIN = domainType(Store.class);
 *
 *     public Store() {
 *         super(DOMAIN);
 *         // Define entities...
 *     }
 * }
 *
 * // Access entities
 * Store store = new Store();
 * Entities entities = store.entities();
 *
 * // Create entity instances
 * Entity customer = entities.entity(Customer.TYPE)
 *     .with(Customer.NAME, "John Doe")
 *     .with(Customer.EMAIL, "john@example.com")
 *     .build();
 *
 * // Create primary keys
 * Entity.Key customerKey = entities.primaryKey(Customer.TYPE, 42);
 *}
 * <p>
 * Or via an EntityConnection or EntityConnectionProvider:
 * {@snippet :
 * EntityConnectionProvider connectionProvider = connectionProvider();
 *
 * Entities entities = connection.entities();
 *
 * EntityConnection connection = connectionProvider.connection();
 *
 * entities = connection.entities();
 *}
 * @see #entity(EntityType)
 * @see #key(EntityType)
 * @see #primaryKey(EntityType, Object)
 * @see #primaryKeys(EntityType, Object[])
 */
public interface Entities {

	/**
	 * Specifies whether foreign keys are validated when defined by asserting that the referenced entity has been defined.
	 * This can be disabled in cases where entities have circular references
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> VALIDATE_FOREIGN_KEYS =
					booleanValue("codion.domain.validateForeignKeys", true);

	/**
	 * Specifies whether strict deserialization should be used. This means that when an unknown attribute<br>
	 * is encountered during deserialization, an exception is thrown, instead of silently dropping the associated value.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> STRICT_DESERIALIZATION =
					booleanValue("codion.domain.strictDeserialization", true);

	/**
	 * @return the {@link DomainType} this {@link Entities} instance is associated with
	 */
	DomainType domainType();

	/**
	 * Returns the {@link EntityDefinition} for the given entityType
	 * @param entityType the entityType
	 * @return the entity definition
	 * @throws IllegalArgumentException in case the definition is not found
	 */
	EntityDefinition definition(EntityType entityType);

	/**
	 * Returns the {@link EntityDefinition} for the given entityType name
	 * @param entityTypeName the name of the entityType
	 * @return the entity definition
	 * @throws IllegalArgumentException in case the definition is not found
	 */
	EntityDefinition definition(String entityTypeName);

	/**
	 * @param entityType the entityType
	 * @return true if this domain contains a definition for the given type
	 */
	boolean contains(EntityType entityType);

	/**
	 * Returns all {@link EntityDefinition}s found in this Entities instance
	 * @return all entity definitions
	 */
	Collection<EntityDefinition> definitions();

	/**
	 * Creates a new {@link Entity.Builder} instance for the given entityType
	 * {@snippet :
	 * Entities entities = domain.entities();
	 *
	 * // Build an entity with initial values
	 * Entity customer = entities.entity(Customer.TYPE)
	 *     .with(Customer.NAME, "John Doe")
	 *     .with(Customer.EMAIL, "john@example.com")
	 *     .with(Customer.ACTIVE, true)
	 *     .build();
	 *
	 * // Build with a foreign key reference
	 * Entity order = entities.entity(Order.TYPE)
	 *     .with(Order.CUSTOMER_FK, customer)
	 *     .with(Order.DATE, LocalDate.now())
	 *     .with(Order.TOTAL, 100.50)
	 *     .build();
	 *}
	 * @param entityType the entityType
	 * @return a new {@link Entity.Builder}
	 */
	Entity.Builder entity(EntityType entityType);

	/**
	 * {@snippet :
	 * Entity.Key customerKey = entities.key(Customer.TYPE)
	 *     .with(Customer.ID, 42)
	 *     .build();
	 *}
	 * @param entityType the entityType
	 * @return a new {@link Entity.Key.Builder}
	 */
	Entity.Key.Builder key(EntityType entityType);

	/**
	 * Creates a new {@link Entity.Key} instance of the given entityType, initialised with the given value
	 * {@snippet :
	 * Entities entities = domain.entities();
	 *
	 * // Create a single-value primary key
	 * Entity.Key customerKey = entities.primaryKey(Customer.TYPE, 42);
	 *
	 * // Use the key to fetch an entity
	 * Entity customer = connection.selectSingle(customerKey);
	 *
	 * // Keys can be compared
	 * Entity.Key anotherKey = entities.primaryKey(Customer.TYPE, 42);
	 * customerKey.equals(anotherKey); // true
	 *
	 * // Null values are allowed
	 * Entity.Key nullKey = entities.primaryKey(Customer.TYPE, null);
	 *}
	 * @param entityType the entityType
	 * @param value the key value, assumes a single value key
	 * @param <T> the key value type
	 * @return a new {@link Entity.Key} instance
	 * @throws IllegalStateException in case the given primary key is a composite key
	 * @throws IllegalArgumentException in case the value is not of the correct type
	 * @throws NullPointerException in case entityType is null
	 */
	<T> Entity.Key primaryKey(EntityType entityType, @Nullable T value);

	/**
	 * Creates new {@link Entity.Key} instances of the given entityType, initialised with the given values
	 * {@snippet :
	 * Entities entities = domain.entities();
	 *
	 * // Create multiple keys at once
	 * List<Entity.Key> customerKeys = entities.primaryKeys(Customer.TYPE, 1, 2, 3, 4, 5);
	 *
	 * // Fetch multiple entities
	 * List<Entity> customers = connection.select(customerKeys);
	 *
	 * // Varargs syntax allows flexible usage
	 * List<Entity.Key> keys = entities.primaryKeys(Order.TYPE,
	 *     "ORD-001", "ORD-002", "ORD-003");
	 *}
	 * @param entityType the entityType
	 * @param values the key values, assumes a single value key
	 * @param <T> the key value type
	 * @return new {@link Entity.Key} instances
	 * @throws IllegalStateException in case the given primary key is a composite key
	 * @throws IllegalArgumentException in case any of the values is not of the correct type
	 * @throws NullPointerException in case entityType or values is null
	 */
	<T> List<Entity.Key> primaryKeys(EntityType entityType, T... values);

	/**
	 * Creates new {@link Entity.Key} instances of the given entityType, initialised with the given values
	 * {@snippet :
	 * Entities entities = domain.entities();
	 *
	 * // Create multiple keys at once
	 * List<Entity.Key> customerKeys = entities.primaryKeys(Customer.TYPE, List.of(1, 2, 3, 4, 5));
	 *}
	 * @param entityType the entityType
	 * @param values the key values, assumes a single value key
	 * @param <T> the key value type
	 * @return new {@link Entity.Key} instances
	 * @throws IllegalStateException in case the given primary key is a composite key
	 * @throws IllegalArgumentException in case any of the values is not of the correct type
	 * @throws NullPointerException in case entityType or values is null
	 */
	<T> List<Entity.Key> primaryKeys(EntityType entityType, Collection<T> values);
}
