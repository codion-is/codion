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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.domain.DomainType;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * A repository containing the {@link EntityDefinition}s for a given domain.
 * Factory for {@link Entity} and {@link Entity.Key} instances.
 * <p>
 * The Entities instance serves as the central registry for all entity types within a domain,
 * providing access to entity definitions and factory methods for creating entity instances and keys.
 * <p>
 * Typically accessed through a domain instance:
 * {@snippet class = "is.codion.demos.chinook.javadoc.EntitiesSnippets" region = "usage" :
 * Domain domain = new ChinookImpl(); // @start region=usage
 * Entities entities = domain.entities();
 *
 * // Create entity instances
 * Entity customer = entities.entity(Customer.TYPE)
 *         .with(Customer.FIRSTNAME, "John")
 *         .with(Customer.LASTNAME, "Doe")
 *         .with(Customer.EMAIL, "john@example.com")
 *         .build();
 *
 * // Create primary keys
 * Entity.Key customerKey = entities.primaryKey(Customer.TYPE, 42L); // @end}
 * <p>
 * Or via an EntityConnection:
 * {@snippet class = "is.codion.demos.chinook.javadoc.EntitiesSnippets" region = "connection" :
 * // @start region=connection
 * Entities entities = connection.entities(); // @end}
 * @see #entity(EntityType)
 * @see #key(EntityType)
 * @see #primaryKey(EntityType, Object)
 * @see #primaryKeys(EntityType, Object[])
 * @see #configurable(DomainType)
 */
public sealed interface Entities permits DefaultEntities {

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
	 * {@snippet class = "is.codion.demos.chinook.javadoc.EntitiesSnippets" region = "entity" :
	 * // Build an entity with initial values // @start region=entity
	 * Entity customer = entities.entity(Customer.TYPE)
	 *         .with(Customer.FIRSTNAME, "John")
	 *         .with(Customer.LASTNAME, "Doe")
	 *         .with(Customer.EMAIL, "john@example.com")
	 *         .build();
	 *
	 * // Build with a foreign key reference
	 * Entity invoice = entities.entity(Invoice.TYPE)
	 *         .with(Invoice.CUSTOMER_FK, customer)
	 *         .with(Invoice.DATE, LocalDate.now())
	 *         .build(); // @end}
	 * @param entityType the entityType
	 * @return a new {@link Entity.Builder}
	 */
	Entity.Builder entity(EntityType entityType);

	/**
	 * Creates a new {@link Entity.Key.Builder} instance for the given entityType
	 * {@snippet class = "is.codion.demos.world.javadoc.EntitiesSnippets" region = "key" :
	 * // A composite primary key // @start region=key
	 * Entity.Key languageKey = entities.key(CountryLanguage.TYPE)
	 *         .with(CountryLanguage.COUNTRY_CODE, "ISL")
	 *         .with(CountryLanguage.LANGUAGE, "Icelandic")
	 *         .build(); // @end}
	 * @param entityType the entityType
	 * @return a new {@link Entity.Key.Builder}
	 */
	Entity.Key.Builder key(EntityType entityType);

	/**
	 * Creates a new {@link Entity.Key} instance of the given entityType, initialised with the given value
	 * {@snippet class = "is.codion.demos.chinook.javadoc.EntitiesSnippets" region = "primaryKey" :
	 * // Create a single-value primary key // @start region=primaryKey
	 * Entity.Key customerKey = entities.primaryKey(Customer.TYPE, 42L);
	 *
	 * // Use the key to fetch an entity
	 * Entity customer = connection.select(customerKey);
	 *
	 * // Keys can be compared
	 * Entity.Key anotherKey = entities.primaryKey(Customer.TYPE, 42L);
	 * customerKey.equals(anotherKey); // true
	 *
	 * // Null values are allowed
	 * Entity.Key nullKey = entities.primaryKey(Customer.TYPE, null); // @end}
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
	 * {@snippet class = "is.codion.demos.chinook.javadoc.EntitiesSnippets" region = "primaryKeys" :
	 * // Create multiple keys at once // @start region=primaryKeys
	 * List<Entity.Key> customerKeys = entities.primaryKeys(Customer.TYPE, 1L, 2L, 3L, 4L, 5L);
	 *
	 * // Fetch multiple entities
	 * Collection<Entity> customers = connection.select(customerKeys); // @end}
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
	 * {@snippet class = "is.codion.demos.chinook.javadoc.EntitiesSnippets" region = "primaryKeysCollection" :
	 * // Create multiple keys at once // @start region=primaryKeysCollection
	 * List<Entity.Key> customerKeys = entities.primaryKeys(Customer.TYPE, List.of(1L, 2L, 3L, 4L, 5L)); // @end}
	 * @param entityType the entityType
	 * @param values the key values, assumes a single value key
	 * @param <T> the key value type
	 * @return new {@link Entity.Key} instances
	 * @throws IllegalStateException in case the given primary key is a composite key
	 * @throws IllegalArgumentException in case any of the values is not of the correct type
	 * @throws NullPointerException in case entityType or values is null
	 */
	<T> List<Entity.Key> primaryKeys(EntityType entityType, Collection<T> values);

	/**
	 * Creates a new {@link Entities.Configurable} instance for the given domain type.
	 * @param domainType the domain type
	 * @return a new {@link Configurable} {@link Entities} instance
	 */
	static Entities.Configurable configurable(DomainType domainType) {
		return new ConfigurableEntities(new DefaultEntities(domainType));
	}

	/**
	 * Provides a {@link Configurable} {@link Entities} instance
	 */
	interface Configurable {

		/**
		 * @return the {@link Entities} instance being configured
		 */
		Entities entities();

		/**
		 * Adds an entity definition.
		 * @param definition the entity definition to add
		 * @throws IllegalArgumentException in case the entities instance already contains the given definition
		 */
		void add(EntityDefinition definition);

		/**
		 * Adds all entity definitions from the given instance.
		 * @param entities the entities instance from which to add
		 */
		void add(Entities entities);

		/**
		 * Adds the given entity definitions, including dependencies, from the given instance.
		 * @param entities the entities
		 * @param entityTypes the types to add
		 */
		void add(Entities entities, Collection<EntityType> entityTypes);

		/**
		 * Specifies whether to validate foreign keys when created, asserting that
		 * the referenced entity has been defined. Disable in case of cyclical dependencies.
		 * @param validateForeignKeys true if foreign keys should be validated
		 */
		void validateForeignKeys(boolean validateForeignKeys);
	}
}
