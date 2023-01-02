/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.framework.domain.entity.query.SelectQuery;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.TransientProperty;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

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
  EntityType type();

  /**
   * @return the name of the underlying table, with schema prefix if applicable
   */
  String tableName();

  /**
   * Returns the {@link ConditionProvider} associated with the given type
   * @param conditionType the condition type
   * @return the condition provider associated with the given id
   * @throws IllegalArgumentException in case no ConditionProvider is associated with the given conditionType
   */
  ConditionProvider conditionProvider(ConditionType conditionType);

  /**
   * @return the name of the domain this entity type belongs to
   */
  String domainName();

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
   * @return the default order by clause to use when querying entities of this type
   */
  OrderBy orderBy();

  /**
   * @return the group by clause to use when querying entities of this type,
   * without the "group by" keywords
   */
  String groupByClause();

  /**
   * @return the name of the table to use when selecting entities of this type
   */
  String selectTableName();

  /**
   * @return the select query to use when selecting entities of this type
   */
  SelectQuery setSelectQuery();

  /**
   * @return the object responsible for providing toString values for this entity type
   */
  Function<Entity, String> stringFactory();

  /**
   * @return the comparator used when comparing this entity type to other entities
   */
  Comparator<Entity> comparator();

  /**
   * @return an unmodifiable list view of the properties
   */
  List<Property<?>> properties();

  /**
   * @return true if this entity has a defined primary key
   */
  boolean hasPrimaryKey();

  /**
   * @return true if this entity contains any attribute which values are derived from other attribute
   */
  boolean hasDerivedAttributes();

  /**
   * Returns true if this entity contains attributes which values are derived from the value of the given attribute
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return true if any attribute values are derived from the given attribute
   */
  <T> boolean hasDerivedAttributes(Attribute<T> attribute);

  /**
   * Returns the attributes which values are derived from the value of the given attribute,
   * an empty collection if no such derived attributes exist
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a collection containing the attributes which are derived from the given attribute
   */
  <T> Collection<Attribute<?>> derivedAttributes(Attribute<T> attribute);

  /**
   * Returns a list containing all primary key attributes associated with this entity type.
   * If the entity has no primary key attributes defined, an empty list is returned.
   * @return the primary key attributes of this entity type, sorted by primary key column index
   */
  List<Attribute<?>> primaryKeyAttributes();

  /**
   * Returns a list containing all primary key properties associated with this entity type.
   * If the entity has no primary key properties defined, an empty list is returned.
   * @return the primary key properties of this entity type, sorted by primary key column index
   */
  List<ColumnProperty<?>> primaryKeyProperties();

  /**
   * @return a list containing the visible properties for this entity type
   */
  List<Property<?>> visibleProperties();

  /**
   * @return a list containing the column-based properties for this entity type
   */
  List<ColumnProperty<?>> columnProperties();

  /**
   * @return a list containing all lazy loaded blob properties for this entity type
   */
  List<ColumnProperty<?>> lazyLoadedBlobProperties();

  /**
   * @return a list containing the non-column-based properties for this entity type
   */
  List<TransientProperty<?>> transientProperties();

  /**
   * @return a list containing the foreign key properties for this entity type
   */
  List<ForeignKeyProperty> foreignKeyProperties();

  /**
   * @return all foreign keys for this entity type
   */
  Collection<ForeignKey> foreignKeys();

  /**
   * Returns the {@link EntityDefinition} of the entity referenced by the given foreign key property.
   * @param foreignKey the foreign key
   * @return the definition of the referenced entity
   */
  EntityDefinition referencedEntityDefinition(ForeignKey foreignKey);

  /**
   * @return true if this entity type has any denormalized properties
   */
  boolean hasDenormalizedProperties();

  /**
   * @param entityAttribute the entity attribute
   * @return true if this entity type has any denormalized properties associated with the give entity attribute
   */
  boolean hasDenormalizedProperties(Attribute<Entity> entityAttribute);

  /**
   * Retrieves the denormalized properties which values originate from the entity referenced by the given entity attribute
   * @param entityAttribute the entity attribute
   * @return a list containing the denormalized properties which values originate from the entity
   * referenced by the given foreign key property
   */
  List<DenormalizedProperty<?>> denormalizedProperties(Attribute<Entity> entityAttribute);

  /**
   * @param attribute the attribute
   * @return true if this entity definition contains the given attribute
   */
  boolean containsAttribute(Attribute<?> attribute);

  /**
   * Returns the attribute with the given name, null if none is found.
   * @param attributeName the name of the attribute to fetch
   * @param <T> the attribute type
   * @return the attribute with the given name, null if none is found
   */
  <T> Attribute<T> attribute(String attributeName);

  /**
   * Returns the attributes to search by when searching for entities of this type by a string value
   * @return the attributes to use when searching by string
   * @see ColumnProperty.Builder#searchProperty(boolean)
   */
  Collection<Attribute<String>> searchAttributes();

  /**
   * Returns the attributes selected by default for this entity type.
   * Contains the selectable column property attributes and foreign keys, excluding lazy loaded blob attributes.
   * @return the default select attributes
   */
  Collection<Attribute<?>> defaultSelectAttributes();

  /**
   * @param attribute the attribute
   * @return the column property associated with the attribute
   * @param <T> the attribute type
   * @throws IllegalArgumentException in case the attribute does not represent a {@link ColumnProperty}
   */
  <T> ColumnProperty<T> columnProperty(Attribute<T> attribute);

  /**
   * @param attribute the attribute
   * @return the property associated with {@code attribute}.
   * @param <T> the attribute type
   * @throws IllegalArgumentException in case no such property exists
   */
  <T> Property<T> property(Attribute<T> attribute);

  /**
   * @param attribute the attribute
   * @return the primary key property associated with {@code attribute}.
   * @param <T> the attribute type
   * @throws IllegalArgumentException in case no such property exists
   */
  <T> ColumnProperty<T> primaryKeyProperty(Attribute<T> attribute);

  /**
   * Returns the {@link Property}s based on the given attributes
   * @param attributes the attributes which properties to retrieve
   * @return a list containing the properties based on the given attributes
   */
  List<Property<?>> properties(Collection<Attribute<?>> attributes);

  /**
   * Returns the {@link ColumnProperty}s based on the given attributes
   * @param attributes the attributes which properties to retrieve
   * @return a list of column properties
   */
  List<ColumnProperty<?>> columnProperties(List<Attribute<?>> attributes);

  /**
   * Retrieves the writable (non-read-only) column properties comprising this entity type
   * @param includePrimaryKeyProperties if true primary key properties are included, non-updatable primary key properties
   * are only included if {@code includeNonUpdatable} is true
   * @param includeNonUpdatable if true then non-updatable properties are included
   * @return a list containing the writable column properties (properties that map to database columns) comprising
   * the entity of type {@code entityType}
   */
  List<ColumnProperty<?>> writableColumnProperties(boolean includePrimaryKeyProperties, boolean includeNonUpdatable);

  /**
   * @return a list containing all updatable properties associated with the given entityType
   */
  List<Property<?>> updatableProperties();

  /**
   * @param foreignKey the foreign key
   * @return true if all the underlying properties are updatable
   */
  boolean isUpdatable(ForeignKey foreignKey);

  /**
   * @param attribute the attribute
   * @return true if the given attribute is part of a foreign key
   */
  boolean isForeignKeyAttribute(Attribute<?> attribute);

  /**
   * Returns the foreign keys referencing entities of the given type
   * @param referencedEntityType the referenced entity type
   * @return a List containing the foreign keys, an empty list is returned in case no foreign keys are found
   */
  List<ForeignKey> foreignKeys(EntityType referencedEntityType);

  /**
   * @param foreignKey the foreign key
   * @return the ForeignKeyProperty based on the given foreign key
   * @throws IllegalArgumentException in case no such property exists
   */
  ForeignKeyProperty foreignKeyProperty(ForeignKey foreignKey);

  /**
   * @param columnAttribute the column attribute
   * @param <T> the attribute type
   * @return the ForeignKeyProperties based on the given column attribute
   */
  <T> List<ForeignKeyProperty> foreignKeyProperties(Attribute<T> columnAttribute);

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
   * Creates a new {@link Key} instance based on this definition, initialised with the given value
   * @param value the key value, assuming a single value key
   * @param <T> the key value type
   * @return a new {@link Key} instance
   * @throws IllegalStateException in case the given primary key is a composite key
   * @throws IllegalArgumentException in case the value is not of the correct type
   */
  <T> Key primaryKey(T value);

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
   * Returns a serialization version indicator for the underlying entity.
   * Two {@link EntityDefinition}s having the same version
   * means they are (very likely) compatible when it comes to serialization.
   * @return a version indicator for the underlying entity
   */
  int serializationVersion();

  /**
   * Builds a EntityDefinition
   * @see #definition(Property.Builder[])
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
     * of the properties when defining the entity  must match the column order in the given query.
     * @param selectQuery the select query to use for this entity type
     * @return this {@link Builder} instance
     */
    Builder selectQuery(SelectQuery selectQuery);

    /**
     * Sets the string factory, based on the value of the given attribute. Shortcut for:
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
     * Sets the string factory, that is, the object responsible for creating toString() values for this entity type
     * @param stringFactory the string factory function
     * @return this {@link Builder} instance
     */
    Builder stringFactory(Function<Entity, String> stringFactory);

    /**
     * Sets the comparator to use when comparing this entity type to other entities
     * @param comparator the comparator
     * @return this {@link Builder} instance
     */
    Builder comparator(Comparator<Entity> comparator);

    /**
     * @return a new {@link EntityDefinition} instance based on this builder
     */
    EntityDefinition build();
  }

  /**
   * Creates a {@link EntityDefinition.Builder} instance based on the given property builders.
   * @param propertyBuilders builders for the properties comprising the entity
   * @return a {@link EntityDefinition.Builder} instance
   * @throws IllegalArgumentException in case {@code propertyBuilders} is empty
   */
  static EntityDefinition.Builder definition(Property.Builder<?, ?>... propertyBuilders) {
    return definition(Arrays.asList(requireNonNull(propertyBuilders)));
  }

  /**
   * Creates a {@link EntityDefinition.Builder} instance based on the given property builders.
   * @param propertyBuilders builders for the properties comprising the entity
   * @return a {@link EntityDefinition.Builder} instance
   * @throws IllegalArgumentException in case {@code propertyBuilders} is empty
   */
  static EntityDefinition.Builder definition(List<Property.Builder<?, ?>> propertyBuilders) {
    return new DefaultEntityDefinition.DefaultBuilder(requireNonNull(propertyBuilders));
  }
}
