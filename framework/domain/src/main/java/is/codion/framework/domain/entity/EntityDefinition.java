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
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.query.SelectQuery;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Specifies an entity definition.
 */
public interface EntityDefinition {

  /**
   * Specifies whether it should be possible to define foreign keys referencing entities that have
   * not been defined, this can be disabled in cases where entities have circular references.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> STRICT_FOREIGN_KEYS = Configuration.booleanValue("codion.domain.strictForeignKeys", true);

  /**
   * @return the entity type
   */
  EntityType entityType();

  /**
   * @return the name of the underlying table, with schema prefix if applicable
   */
  String tableName();

  /**
   * Returns the {@link ConditionProvider} associated with the given type
   * @param conditionType the condition type
   * @return the condition provider associated with the given type
   * @throws IllegalArgumentException in case no ConditionProvider is associated with the given conditionType
   */
  ConditionProvider conditionProvider(ConditionType conditionType);

  /**
   * @return the validator for this entity type
   */
  EntityValidator validator();

  /**
   * @return the caption to use when presenting entities of this type
   */
  String caption();

  /**
   * @return the entity description
   */
  String description();

  /**
   * @return true if the underlying table is small enough for displaying the contents in a combo box
   */
  boolean isSmallDataset();

  /**
   * @return true if the data in the underlying table can be regarded as static
   */
  boolean isStaticData();

  /**
   * @return true if this entity type is read only
   */
  boolean isReadOnly();

  /**
   * @return true if optimistic locking should be used during updates
   */
  boolean isOptimisticLockingEnabled();

  /**
   * @return the object responsible for generating primary key values for entities of this type
   * @see Builder#keyGenerator(KeyGenerator)
   */
  KeyGenerator keyGenerator();

  /**
   * Returns true if the value for the primary key of this entity is generated with a {@link KeyGenerator}.
   * @return true if the value for the primary key is generated
   * @see Builder#keyGenerator(KeyGenerator)
   */
  boolean isKeyGenerated();

  /**
   * @return the default order by clause to use when querying entities of this type, null if none is available
   */
  OrderBy orderBy();

  /**
   * @return the name of the table to use when selecting entities of this type
   */
  String selectTableName();

  /**
   * @return the select query to use when selecting entities of this type, may be null
   */
  SelectQuery selectQuery();

  /**
   * @return the object responsible for providing toString values for this entity type
   */
  Function<Entity, String> stringFactory();

  /**
   * @return the comparator used when comparing this entity type to other entities
   */
  Comparator<Entity> comparator();

  /**
   * @return an unmodifiable list view of the attribute definitions
   */
  List<AttributeDefinition<?>> attributeDefinitions();

  /**
   * @return true if this entity has a defined primary key
   */
  boolean hasPrimaryKey();

  /**
   * Returns the attributes which values are derived from the value of the given attribute,
   * an empty collection if no such derived attributes exist
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a collection containing the attributes which are derived from the given attribute
   */
  <T> Collection<Attribute<?>> derivedAttributes(Attribute<T> attribute);

  /**
   * Returns a list containing all primary key columns associated with this entity type.
   * If the entity has no primary key columns defined, an empty list is returned.
   * @return the primary key columns of this entity type, sorted by primary key column index
   */
  List<Column<?>> primaryKeyColumns();

  /**
   * Returns a list containing the definitions of all primary key columns associated with this entity type.
   * If the entity has no primary key columns defined, an empty list is returned.
   * @return the primary key column definitions of this entity type, sorted by primary key column index
   */
  List<ColumnDefinition<?>> primaryKeyColumnDefinitions();

  /**
   * @return a list containing the definitions of the colums for this entity type
   */
  List<ColumnDefinition<?>> columnDefinitions();

  /**
   * @return a list containing the foreign key definitions for this entity type
   */
  Collection<ForeignKeyDefinition> foreignKeyDefinitions();

  /**
   * @return all foreign keys for this entity type
   */
  Collection<ForeignKey> foreignKeys();

  /**
   * Returns the {@link EntityDefinition} of the entity referenced by the given foreign key.
   * @param foreignKey the foreign key
   * @return the definition of the referenced entity
   */
  EntityDefinition referencedEntity(ForeignKey foreignKey);

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
  <T> Attribute<T> attribute(String attributeName);

  /**
   * Returns the solumns to search by when searching for entities of this type by a string value
   * @return the solumn to use when searching by string
   * @see ColumnDefinition.Builder#searchColumn(boolean)
   */
  Collection<Column<String>> searchColumns();

  /**
   * Returns the attributes selected by default for this entity type.
   * Contains the selectable columns and foreign keys, excluding lazy loaded blob columns.
   * @return the default select attributes
   */
  Collection<Attribute<?>> selectAttributes();

  /**
   * @param column the column
   * @param <T> the column type
   * @return the column definition associated with the column
   * @throws IllegalArgumentException in case the column does not represent a {@link ColumnDefinition}
   * @throws NullPointerException in case {@code column} is null
   */
  <T> ColumnDefinition<T> columnDefinition(Column<T> column);

  /**
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return the attribute definition associated with {@code attribute}.
   * @throws IllegalArgumentException in case no such attribute exists
   * @throws NullPointerException in case {@code attribute} is null
   */
  <T> AttributeDefinition<T> attributeDefinition(Attribute<T> attribute);

  /**
   * @return a Collection containing all updatable attributes associated with the given entityType
   */
  Collection<AttributeDefinition<?>> updatableAttributeDefinitions();

  /**
   * @param foreignKey the foreign key
   * @return true if all the underlying columns are updatable
   */
  boolean isUpdatable(ForeignKey foreignKey);

  /**
   * @param column the column
   * @return true if the given column is part of a foreign key
   */
  boolean isForeignKeyColumn(Column<?> column);

  /**
   * Returns the foreign keys referencing entities of the given type
   * @param referencedEntityType the referenced entity type
   * @return the foreign keys referencing the given entity type, an empty collection is returned in case no foreign keys are found
   */
  Collection<ForeignKey> foreignKeys(EntityType referencedEntityType);

  /**
   * @param foreignKey the foreign key
   * @return the ForeignKeyDefinition for the given foreign key
   * @throws IllegalArgumentException in case no such foreign key exists
   */
  ForeignKeyDefinition foreignKeyDefinition(ForeignKey foreignKey);

  /**
   * @param column the column
   * @param <T> the attribute type
   * @return the ForeignKeyDefinitions associated with the given column, an empty collection in case none are found
   */
  <T> Collection<ForeignKeyDefinition> foreignKeyDefinitions(Column<T> column);

  /**
   * Returns the background color provider, never null
   * @return the background color provider
   */
  ColorProvider backgroundColorProvider();

  /**
   * Returns the foreground color provider, never null
   * @return the foreground color provider
   */
  ColorProvider foregroundColorProvider();

  /**
   * Creates a new {@link Entity} instance based on this definition
   * @return a new {@link Entity} instance
   */
  Entity entity();

  /**
   * Creates a new {@link Entity} instance based on this definition
   * @param values the values
   * @return a new {@link Entity} instance
   * @throws IllegalArgumentException in case any of the value attributes are not part of the entity.
   */
  Entity entity(Map<Attribute<?>, Object> values);

  /**
   * Creates a new {@link Entity} instance based on this definition
   * @param values the values
   * @param originalValues the original values
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
   * Returns the Attribute for the getter this method represents in the {@link EntityType#entityClass()},
   * null if none exists.
   * @param method the method
   * @return the getter attribute
   */
  Attribute<?> getterAttribute(Method method);

  /**
   * Returns the Attribute for the setter this method represents in the {@link EntityType#entityClass()},
   * null if none exists.
   * @param method the method
   * @return the setter attribute
   */
  Attribute<?> setterAttribute(Method method);

  /**
   * Returns the MethodHandle for the given default method in the {@link EntityType#entityClass()}.
   * @param method the method
   * @return a MethodHandle based on the method
   */
  MethodHandle defaultMethodHandle(Method method);

  /**
   * Builds a EntityDefinition
   * @see EntityType#define(AttributeDefinition.Builder[])
   * @see EntityType#define(List)
   */
  interface Builder {

    /**
     * @param tableName the table name
     * @return this {@link Builder} instance
     */
    Builder tableName(String tableName);

    /**
     * @param validator the validator for this entity type
     * @return this {@link Builder} instance
     */
    Builder validator(EntityValidator validator);

    /**
     * Adds a {@link ConditionProvider} which provides a dynamic query condition string.
     * The condition string should not include the WHERE keyword and use the ?
     * substitution character where values should be inserted.
     * @param conditionType the condition type
     * @param conditionProvider the condition provider
     * @return this {@link Builder} instance
     */
    Builder conditionProvider(ConditionType conditionType, ConditionProvider conditionProvider);

    /**
     * @param backgroundColorProvider the background color provider
     * @return this {@link Builder} instance
     */
    Builder backgroundColorProvider(ColorProvider backgroundColorProvider);

    /**
     * @param foregroundColorProvider the foreground color provider
     * @return this {@link Builder} instance
     */
    Builder foregroundColorProvider(ColorProvider foregroundColorProvider);

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
     * Specifies whether this entity should be regarded as based on a static dataset, that is, one that rarely changes.
     * This is useful in deciding how often to refresh, say, a combo box based on the entity.
     * @param staticData true if the underlying data should be regarded as static
     * @return this {@link Builder} instance
     */
    Builder staticData(boolean staticData);

    /**
     * Specifies whether this entity should be read-only, that it should not be possible to
     * insert, update or delete entities of this type
     * @param readOnly true if this entity is read-only
     * @return this {@link Builder} instance
     */
    Builder readOnly(boolean readOnly);

    /**
     * Use this to disable optimistic locking for this entity type
     * @param optimisticLockingEnabled true if optimistic locking should be used during updates, false to disable
     * @return this {@link Builder} instance
     */
    Builder optimisticLockingEnabled(boolean optimisticLockingEnabled);

    /**
     * Sets the primary key generator
     * @param keyGenerator the primary key generator
     * @return this {@link Builder} instance
     * @see #isKeyGenerated()
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
     * @param selectTableName the name of the table
     * @return this {@link Builder} instance
     */
    Builder selectTableName(String selectTableName);

    /**
     * Sets the select query to use when selecting entities of this type,
     * use with care. If the query contains a columns clause, the order
     * of the attributes when defining the entity must match the column order in the given query.
     * @param selectQuery the select query to use for this entity type
     * @return this {@link Builder} instance
     */
    Builder selectQuery(SelectQuery selectQuery);

    /**
     * Sets the string factory, using the value of the given attribute. Shortcut for:
     * <pre>
     * stringFactory(StringFactory.builder()
     *           .value(attribute)
     *           .build())
     * </pre>
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
     */
    Builder stringFactory(Function<Entity, String> stringFactory);

    /**
     * Sets the comparator to use when comparing entities of this type
     * @param comparator the comparator
     * @return this {@link Builder} instance
     */
    Builder comparator(Comparator<Entity> comparator);

    /**
     * @return a new {@link EntityDefinition} instance based on this builder
     */
    EntityDefinition build();
  }
}
