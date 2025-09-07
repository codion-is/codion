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
import is.codion.framework.domain.entity.DefaultEntityDefinition.DefaultAttributes;
import is.codion.framework.domain.entity.DefaultEntityDefinition.DefaultBuilder;
import is.codion.framework.domain.entity.DefaultEntityDefinition.DefaultColumns;
import is.codion.framework.domain.entity.DefaultEntityDefinition.DefaultForeignKeys;
import is.codion.framework.domain.entity.DefaultEntityDefinition.DefaultPrimaryKey;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.condition.ConditionString;
import is.codion.framework.domain.entity.condition.ConditionType;
import is.codion.framework.domain.entity.query.EntitySelectQuery;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static is.codion.common.Configuration.booleanValue;

/**
 * Specifies an entity definition, encapsulating the metadata for an entity type.
 * <p>
 * An EntityDefinition contains all information about an entity's structure including:
 * <ul>
 *   <li>The underlying table name and database mapping
 *   <li>All attributes (columns, foreign keys, derived attributes)
 *   <li>Primary key definition
 *   <li>Validation rules and behavior settings
 *   <li>Display properties (caption, description, ordering)
 * </ul>
 * <p>
 * Entity definitions are typically created using the builder pattern during domain initialization:
 * {@snippet :
 * public class Store extends DefaultDomain {
 *
 *     // Define entity types
 *     interface Customer {
 *         EntityType TYPE = DOMAIN.entityType("store.customer");
 *
 *         Column<Integer> ID = TYPE.integerColumn("id");
 *         Column<String> NAME = TYPE.stringColumn("name");
 *         Column<String> EMAIL = TYPE.stringColumn("email");
 *     }
 *
 *     // Define the entity structure
 *     void defineCustomer() {
 *         EntityDefinition definition = Customer.TYPE.define(
 *                 Customer.ID.define()
 *                     .primaryKey(),
 *                 Customer.NAME.define()
 *                     .column()
 *                     .caption("Name")
 *                     .nullable(false)
 *                     .maximumLength(100),
 *                 Customer.EMAIL.define()
 *                     .column()
 *                     .caption("Email")
 *                     .maximumLength(255))
 *             .table("customer")
 *             .caption("Customer")
 *             .orderBy(ascending(Customer.NAME))
 *             .smallDataset(true)
 *             .build();
 *     }
 * }
 *}
 * @see EntityType#define(AttributeDefinition.Builder[])
 * @see Builder
 */
public sealed interface EntityDefinition permits DefaultEntityDefinition {

	/**
	 * Specifies whether optimistic locking should be enabled by default for entities
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> OPTIMISTIC_LOCKING = booleanValue("codion.domain.optimisticLocking", true);

	/**
	 * @return the entity type
	 */
	EntityType type();

	/**
	 * @return the name of the underlying table, with schema prefix if applicable
	 */
	String table();

	/**
	 * Returns the {@link ConditionString} associated with the given type
	 * @param conditionType the condition type
	 * @return the ConditionString associated with the given condition
	 * @throws IllegalArgumentException in case no ConditionString is associated with the given conditionType
	 * @see EntityDefinition.Builder#condition(ConditionType, ConditionString)
	 */
	ConditionString condition(ConditionType conditionType);

	/**
	 * @return the validator for this entity type
	 */
	EntityValidator validator();

	/**
	 * The default exists predicate returns true if the entity has a non-null original primary key,
	 * which is a best-guess about an entity existing in a database.
	 * @return the predicate to use to check if an entity of this type exists in the database
	 */
	Predicate<Entity> exists();

	/**
	 * @return the caption to use when presenting entities of this type
	 */
	String caption();

	/**
	 * @return the entity description
	 */
	Optional<String> description();

	/**
	 * @return true if the underlying table is small enough for displaying the contents in a combo box
	 */
	boolean smallDataset();

	/**
	 * @return true if this entity type is read only
	 */
	boolean readOnly();

	/**
	 * @return true if optimistic locking should be used during updates
	 */
	boolean optimisticLocking();

	/**
	 * @return the default order by clause to use when querying entities of this type, an empty Optional if none is available
	 */
	Optional<OrderBy> orderBy();

	/**
	 * @return the name of the table to use when selecting entities of this type
	 */
	String selectTable();

	/**
	 * @return the select query to use when selecting entities of this type, an empty Optional if none is available
	 */
	Optional<EntitySelectQuery> selectQuery();

	/**
	 * Returns the function responsible for providing toString values for this entity type.
	 * {@snippet :
	 * // Define custom string representation
	 * Customer.TYPE.define(
	 *         Customer.ID.define()
	 *             .primaryKey(),
	 *         Customer.FIRST_NAME.define()
	 *             .column(),
	 *         Customer.LAST_NAME.define()
	 *             .column(),
	 *         Customer.EMAIL.define()
	 *             .column())
	 *     .stringFactory(customer ->
	 *         customer.get(Customer.LAST_NAME) + ", " +
	 *         customer.get(Customer.FIRST_NAME) +
	 *         " (" + customer.get(Customer.EMAIL) + ")")
	 *     .build();
	 *
	 * // Usage
	 * Entity customer = entities.entity(Customer.TYPE)
	 *     .with(Customer.FIRST_NAME, "John")
	 *     .with(Customer.LAST_NAME, "Doe")
	 *     .with(Customer.EMAIL, "john@example.com")
	 *     .build();
	 *
	 * System.out.println(customer); // "Doe, John (john@example.com)"
	 *}
	 * @return the function responsible for providing toString values for this entity type
	 */
	Function<Entity, String> stringFactory();

	/**
	 * True by default.
	 * @return true if the result of toString() is cached
	 */
	boolean cacheToString();

	/**
	 * @return the comparator used when comparing this entity type to other entities
	 */
	Comparator<Entity> comparator();

	/**
	 * @return the {@link Attributes} instance
	 */
	Attributes attributes();

	/**
	 * @return the {@link Columns} instance
	 */
	Columns columns();

	/**
	 * @return the {@link ForeignKeys} instance
	 */
	ForeignKeys foreignKeys();

	/**
	 * @return the {@link PrimaryKey} instance
	 */
	PrimaryKey primaryKey();

	/**
	 * Creates a new {@link Entity} instance based on this definition
	 * @return a new {@link Entity} instance
	 */
	Entity entity();

	/**
	 * Creates a new {@link Entity} instance based on this definition
	 * @param values the initial values, an empty map in case of no values
	 * @return a new {@link Entity} instance
	 * @throws IllegalArgumentException in case any of the value attributes are not part of the entity.
	 */
	Entity entity(Map<Attribute<?>, Object> values);

	/**
	 * Creates a new {@link Entity} instance based on this definition
	 * @param values the initial values, an empty map in case of no values
	 * @param originalValues the original values, an empty map in case of no original values
	 * @return a new {@link Entity} instance
	 * @throws IllegalArgumentException in case any of the value attributes are not part of the entity.
	 */
	Entity entity(Map<Attribute<?>, Object> values, Map<Attribute<?>, Object> originalValues);

	/**
	 * Creates a new {@link Entity.Key} instance based on this definition, initialised with the given value
	 * @param value the key value, assuming a single value key
	 * @param <T> the key value type
	 * @return a new {@link Entity.Key} instance
	 * @throws IllegalStateException in case the given primary key is a composite key
	 * @throws IllegalArgumentException in case the value is not of the correct type
	 */
	<T> Entity.Key primaryKey(T value);

	/**
	 * Builds a EntityDefinition
	 * @see EntityType#define(AttributeDefinition.Builder[])
	 * @see EntityType#define(List)
	 */
	sealed interface Builder permits DefaultBuilder {

		/**
		 * @param table the table name
		 * @return this {@link Builder} instance
		 */
		Builder table(String table);

		/**
		 * @param validator the validator for this entity type
		 * @return this {@link Builder} instance
		 */
		Builder validator(EntityValidator validator);

		/**
		 * Adds a {@link ConditionString} which provides a dynamic query condition string.
		 * The condition string should not include the WHERE keyword and use the ?
		 * substitution character where values should be inserted.
		 * @param conditionType the condition type
		 * @param conditionString the condition string provider
		 * @return this {@link Builder} instance
		 */
		Builder condition(ConditionType conditionType, ConditionString conditionString);

		/**
		 * Sets the caption for this entity type
		 * @param caption the caption
		 * @return this {@link Builder} instance
		 */
		Builder caption(String caption);

		/**
		 * Specifies the resource bundle key associated with the caption.
		 * @param captionResourceKey the name of the resource bundle key associated with the caption for this entity
		 * @return this {@link Builder} instance
		 * @see EntityType#resourceBundleName()
		 */
		Builder captionResourceKey(String captionResourceKey);

		/**
		 * Specifies a description for this entity.
		 * @param description the description
		 * @return this {@link Builder} instance
		 */
		Builder description(String description);

		/**
		 * Specifies whether this entity should be regarded as being based on a small dataset,
		 * which primarily means that combo box models can be based on this entity.
		 * @param smallDataset true if this entity is based on a small dataset
		 * @return this {@link Builder} instance
		 */
		Builder smallDataset(boolean smallDataset);

		/**
		 * Specifies whether this entity should be read-only, that it should not be possible to
		 * insert, update or delete entities of this type
		 * @param readOnly true if this entity is read-only
		 * @return this {@link Builder} instance
		 */
		Builder readOnly(boolean readOnly);

		/**
		 * Use this to disable optimistic locking for this entity type
		 * @param optimisticLocking true if optimistic locking should be used during updates, false to disable
		 * @return this {@link Builder} instance
		 */
		Builder optimisticLocking(boolean optimisticLocking);

		/**
		 * Sets the primary key generator
		 * @param keyGenerator the primary key generator
		 * @return this {@link Builder} instance
		 * @see PrimaryKey#generated()
		 */
		Builder keyGenerator(KeyGenerator keyGenerator);

		/**
		 * Sets the order by clause for this entity type.
		 * @param orderBy the order by clause
		 * @return this {@link Builder} instance
		 */
		Builder orderBy(OrderBy orderBy);

		/**
		 * Sets the name of the table to use when selecting entities of this type,
		 * when it differs from the one used to update/insert, such as a view.
		 * @param selectTable the name of the table
		 * @return this {@link Builder} instance
		 */
		Builder selectTable(String selectTable);

		/**
		 * Sets the select query to use when selecting entities of this type,
		 * use with care. If the query contains a columns clause, the order
		 * of the attributes when defining the entity must match the column order in the given query.
		 * @param selectQuery the select query to use for this entity type
		 * @return this {@link Builder} instance
		 */
		Builder selectQuery(EntitySelectQuery selectQuery);

		/**
		 * Sets the string factory, using the value of the given attribute. Shortcut for:
		 * {@snippet :
		 * stringFactory(EntityFormatter.builder()
		 *           .value(attribute)
		 *           .build())
		 *}
		 * @param attribute the attribute which value to use
		 * @return this {@link Builder} instance
		 */
		Builder stringFactory(Attribute<?> attribute);

		/**
		 * Sets the string factory, that is, the function responsible for creating toString() values for this entity type.
		 * Note that if for some reason this function returns null, the default string factory is used as fallback,
		 * which simply returns the entity type name and primary key value.
		 * @param stringFactory the string factory function
		 * @return this {@link Builder} instance
		 * @see #cacheToString(boolean)
		 */
		Builder stringFactory(Function<Entity, String> stringFactory);

		/**
		 * @param cacheToString true if the result of toString() should be cached
		 * @return this {@link Builder} instance
		 */
		Builder cacheToString(boolean cacheToString);

		/**
		 * Sets the comparator to use when comparing entities of this type
		 * @param comparator the comparator
		 * @return this {@link Builder} instance
		 */
		Builder comparator(Comparator<Entity> comparator);

		/**
		 * Sets the predicate to use when checking if an entity of this type exists in the database.
		 * The default predicate returns true if the entity has a non-null original primary key,
		 * which is a best-guess about an entity existing in a database.
		 * @param exists the entity exists predicate
		 * @return this {@link Builder} instance
		 */
		Builder exists(Predicate<Entity> exists);

		/**
		 * @return a new {@link EntityDefinition} instance based on this builder
		 */
		EntityDefinition build();
	}

	/**
	 * Holds the attribute definitions for an entity type
	 */
	sealed interface Attributes permits DefaultAttributes {

		/**
		 * @return all attributes for this entity type
		 */
		Collection<Attribute<?>> get();

		/**
		 * @return an unmodifiable view of the attribute definitions
		 */
		Collection<AttributeDefinition<?>> definitions();

		/**
		 * Returns the attributes which values are derived from the value of the given attribute,
		 * an empty collection if no such derived attributes exist
		 * @param attribute the attribute
		 * @param <T> the attribute type
		 * @return a collection containing the attributes which are derived from the given attribute
		 */
		<T> Collection<Attribute<?>> derivedFrom(Attribute<T> attribute);

		/**
		 * @param attribute the attribute
		 * @return true if this entity definition contains the given attribute
		 */
		boolean contains(Attribute<?> attribute);

		/**
		 * Returns the attribute with the given name, null if none is found.
		 * @param attributeName the name of the attribute to fetch
		 * @param <T> the attribute type
		 * @return the attribute with the given name, null if none is found
		 */
		<T> @Nullable Attribute<T> get(String attributeName);

		/**
		 * Returns the attribute with the given name.
		 * @param attributeName the name of the attribute to fetch
		 * @param <T> the attribute type
		 * @return the attribute with the given name
		 * @throws IllegalArgumentException in case the attribute was not found
		 */
		<T> Attribute<T> getOrThrow(String attributeName);

		/**
		 * Returns the attributes selected by default for this entity type.
		 * Contains the selectable columns and foreign keys, excluding lazy loaded columns
		 * and foreign key values based on lazy loaded columns.
		 * @return the default select attributes
		 */
		Collection<Attribute<?>> selected();

		/**
		 * @param attribute the attribute
		 * @param <T> the attribute type
		 * @return the attribute definition associated with {@code attribute}.
		 * @throws IllegalArgumentException in case no such attribute exists
		 * @throws NullPointerException in case {@code attribute} is null
		 */
		<T> AttributeDefinition<T> definition(Attribute<T> attribute);

		/**
		 * @return a Collection containing all updatable attributes associated with the given entityType
		 */
		Collection<AttributeDefinition<?>> updatable();
	}

	/**
	 * Holds the column definitions for an entity type
	 */
	sealed interface Columns permits DefaultColumns {

		/**
		 * @return all columns for this entity type
		 */
		Collection<Column<?>> get();

		/**
		 * @return all the column definitions for this entity type
		 */
		Collection<ColumnDefinition<?>> definitions();

		/**
		 * Returns the columns to search by when searching for entities of this type by a string value
		 * @return the columns to use when searching by string
		 * @see ColumnDefinition.Builder#searchable(boolean)
		 */
		Collection<Column<String>> searchable();

		/**
		 * @param column the column
		 * @param <T> the column type
		 * @return the column definition associated with the column
		 * @throws NullPointerException in case {@code column} is null
		 */
		<T> ColumnDefinition<T> definition(Column<T> column);
	}

	/**
	 * Holds the foreign key definitions for an entity type
	 */
	sealed interface ForeignKeys permits DefaultForeignKeys {

		/**
		 * @return a list containing the foreign key definitions for this entity type
		 */
		Collection<ForeignKeyDefinition> definitions();

		/**
		 * @return all foreign keys for this entity type
		 */
		Collection<ForeignKey> get();

		/**
		 * Returns the {@link EntityDefinition} of the entity referenced by the given foreign key.
		 * @param foreignKey the foreign key
		 * @return the definition of the referenced entity
		 */
		EntityDefinition referencedBy(ForeignKey foreignKey);

		/**
		 * @param foreignKey the foreign key
		 * @return true if all the underlying columns are updatable
		 */
		boolean updatable(ForeignKey foreignKey);

		/**
		 * @param column the column
		 * @return true if the given column is part of a foreign key
		 */
		boolean foreignKeyColumn(Column<?> column);

		/**
		 * Returns the foreign keys referencing entities of the given type
		 * @param referencedEntityType the referenced entity type
		 * @return the foreign keys referencing the given entity type, an empty collection is returned in case no foreign keys are found
		 */
		Collection<ForeignKey> get(EntityType referencedEntityType);

		/**
		 * @param foreignKey the foreign key
		 * @return the ForeignKeyDefinition for the given foreign key
		 * @throws IllegalArgumentException in case no such foreign key exists
		 */
		ForeignKeyDefinition definition(ForeignKey foreignKey);

		/**
		 * @param column the column
		 * @param <T> the attribute type
		 * @return the ForeignKeyDefinitions associated with the given column, an empty collection in case none are found
		 */
		<T> Collection<ForeignKeyDefinition> definitions(Column<T> column);
	}

	/**
	 * Holds the primary key definition for an entity type
	 */
	sealed interface PrimaryKey permits DefaultPrimaryKey {

		/**
		 * Returns a list containing all primary key columns associated with this entity type.
		 * If the entity has no primary key columns defined, an empty list is returned.
		 * @return the primary key columns of this entity type, sorted by primary key column index
		 */
		List<Column<?>> columns();

		/**
		 * Returns a list containing the definitions of all primary key columns associated with this entity type.
		 * If the entity has no primary key columns defined, an empty list is returned.
		 * @return the primary key column definitions of this entity type, sorted by primary key column index
		 */
		List<ColumnDefinition<?>> definitions();

		/**
		 * @return the object responsible for generating primary key values for entities of this type
		 * @see Builder#keyGenerator(KeyGenerator)
		 */
		KeyGenerator generator();

		/**
		 * Returns true if the value for the primary key of this entity is generated with a {@link KeyGenerator}.
		 * @return true if the value for the primary key is generated
		 * @see Builder#keyGenerator(KeyGenerator)
		 */
		boolean generated();
	}
}
