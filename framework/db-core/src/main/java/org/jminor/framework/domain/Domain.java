/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.Serializer;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.common.db.valuemap.exception.LengthValidationException;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.RangeValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * A repository specifying the {@link Entity.Definition}s for a given domain.
 * Used to instantiate {@link Entity} and {@link Entity.Key} instances.
 */
public class Domain implements Serializable {

  private static final long serialVersionUID = 1;

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(Domain.class.getName(), Locale.getDefault());

  private static final String MSG_PROPERTY_VALUE_IS_REQUIRED = "property_value_is_required";

  /**
   * Specifies whether or not to allow entities to be re-defined, that is,
   * allow a new definition to replace an old one.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final Value<Boolean> ALLOW_REDEFINE_ENTITY = Configuration.booleanValue("jminor.domain.allowRedefineEntity", false);

  /**
   * Specifies the class used for serializing and deserializing entity instances.<br>
   * Value type: String, the name of the class implementing org.jminor.common.Serializer&#60;Entity&#62;<br>
   * Default value: none
   */
  public static final Value<String> ENTITY_SERIALIZER_CLASS = Configuration.stringValue("jminor.domain.entitySerializerClass", null);

  private static final String ENTITY_PARAM = "entity";
  private static final String ENTITY_ID_PARAM = "entityId";
  private static final String PROPERTY_ID_PARAM = "propertyId";
  private static final String PROPERTY_PARAM = "property";

  private static final Map<String, Domain> REGISTERED_DOMAINS = new HashMap<>();

  private final String domainId;
  private final Map<String, Entity.Definition> entityDefinitions = new LinkedHashMap<>();

  private final transient Map<String, List<Property.ForeignKeyProperty>> foreignKeyReferenceMap = new HashMap<>();
  private final transient Map<String, DatabaseConnection.Operation> databaseOperations = new HashMap<>();

  /**
   * Instantiates a new Domain with the simple name of the class as domain id
   * @see Class#getSimpleName()
   */
  public Domain() {
    this.domainId = getClass().getSimpleName();
  }

  /**
   * Instantiates a new Domain
   * @param domainId the domain identifier
   */
  public Domain(final String domainId) {
    this.domainId = Objects.requireNonNull(domainId, "domainId");
  }

  /**
   * Instantiates a new domain and copies all the entity definitions from {@code domain}
   * @param domain the domain to copy
   */
  public Domain(final Domain domain) {
    this.domainId = Objects.requireNonNull(domain).domainId;
    this.entityDefinitions.putAll(domain.entityDefinitions);
  }

  /**
   * @return the domain Id
   */
  public final String getDomainId() {
    return domainId;
  }

  /**
   * Creates a new {@link Entity} instance with the given entityId
   * @param entityId the entity id
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final String entityId) {
    return new DefaultEntity(this, entityId);
  }

  /**
   * Creates a new {@link Entity} instance with the given primary key
   * @param key the primary key
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final Entity.Key key) {
    return new DefaultEntity(this, key);
  }

  /**
   * Instantiates a new {@link Entity} instance using the given maps for the values and original values respectively.
   * Note that the given map instances are used internally, modifying the contents of those maps outside the
   * {@link Entity} instance will definitely result in some unexpected and unpleasant behaviour.
   * @param entityId the entity id
   * @param values the values
   * @param originalValues the original values
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final String entityId, final Map<Property, Object> values, final Map<Property, Object> originalValues) {
    return new DefaultEntity(this, entityId, values, originalValues);
  }

  /**
   * Instantiates a new {@link Entity} of the given type using the values provided by {@code valueProvider}.
   * Values are fetched for {@link Property.ColumnProperty} and its descendants, {@link Property.ForeignKeyProperty}
   * and {@link Property.TransientProperty} (excluding its descendants).
   * If a {@link Property.ColumnProperty}s column has a default value the property is
   * skipped unless it has a default value, which then overrides the columns default value.
   * @param entityId the entity id
   * @param valueProvider the value provider
   * @return the populated entity
   * @see Property.ColumnProperty#setColumnHasDefaultValue(boolean)
   * @see Property.ColumnProperty#setDefaultValue(Object)
   */
  public final Entity defaultEntity(final String entityId, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = entity(entityId);
    final Collection<Property.ColumnProperty> columnProperties = getColumnProperties(entityId);
    for (final Property.ColumnProperty property : columnProperties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()//these are set via their respective parent properties
              && (!property.columnHasDefaultValue() || property.hasDefaultValue())) {
        entity.put(property, valueProvider.get(property));
      }
    }
    final Collection<Property.TransientProperty> transientProperties = getTransientProperties(entityId);
    for (final Property.TransientProperty transientProperty : transientProperties) {
      if (!(transientProperty instanceof Property.DerivedProperty)) {
        entity.put(transientProperty, valueProvider.get(transientProperty));
      }
    }
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties(entityId);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      entity.put(foreignKeyProperty, valueProvider.get(foreignKeyProperty));
    }
    entity.saveAll();

    return entity;
  }

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId
   * @param entityId the entity id
   * @return a new {@link Entity.Key} instance
   */
  public final Entity.Key key(final String entityId) {
    return new DefaultEntity.DefaultKey(getDefinition(entityId), null);
  }

  /**
   * Adds a new {@link Entity.Definition} to this domain model, using the {@code entityId} as table name.
   * Returns the {@link Entity.Definition} instance for further configuration.
   * @param entityId the id uniquely identifying the entity type
   * @param properties the {@link Property} objects to base this entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a new {@link Entity.Definition}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  public final Entity.Definition define(final String entityId, final Property... properties) {
    return define(entityId, entityId, properties);
  }

  /**
   * Adds a new {@link Entity.Definition} to this domain model.
   * Returns the {@link Entity.Definition} instance for further configuration.
   * @param entityId the id uniquely identifying the entity type
   * @param tableName the name of the underlying table
   * @param properties the {@link Property} objects to base the entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a new {@link Entity.Definition}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  public final Entity.Definition define(final String entityId, final String tableName, final Property... properties) {
    Objects.requireNonNull(entityId, ENTITY_ID_PARAM);
    Objects.requireNonNull(tableName, "tableName");
    if (entityDefinitions.containsKey(entityId) && !ALLOW_REDEFINE_ENTITY.get()) {
      throw new IllegalArgumentException("Entity has already been defined: " + entityId + ", for table: " + tableName);
    }
    final Map<String, Property> propertyMap = initializePropertyMap(domainId, entityId, properties);
    final List<Property.ColumnProperty> columnProperties = Collections.unmodifiableList(getColumnProperties(propertyMap.values()));
    final List<Property.ForeignKeyProperty> foreignKeyProperties = Collections.unmodifiableList(getForeignKeyProperties(propertyMap.values()));
    final List<Property.TransientProperty> transientProperties = Collections.unmodifiableList(getTransientProperties(propertyMap.values()));

    final DefaultEntityDefinition entityDefinition = new DefaultEntityDefinition(domainId, entityId,
            tableName, propertyMap, columnProperties, foreignKeyProperties, transientProperties);
    entityDefinition.setValidator(new Validator());
    entityDefinitions.put(entityId, entityDefinition);

    return entityDefinition;
  }

  /**
   * Returns the propertyIds specifying the properties to search by when looking up
   * entities of the type identified by {@code entityId}
   * @param entityId the entity id
   * @return the ids of the properties used as search properties for entities identified by {@code entityId}
   * @see Entity.Definition#setSearchPropertyIds(String...)
   */
  public final Collection<String> getSearchPropertyIds(final String entityId) {
    return getDefinition(entityId).getSearchPropertyIds();
  }

  /**
   * Returns the properties to search by when looking up entities of the type identified by {@code entityId}
   * @param entityId the entity id
   * @return the properties to use when searching
   * @see Entity.Definition#setSearchPropertyIds(String...)
   */
  public final Collection<Property.ColumnProperty> getSearchProperties(final String entityId) {
    final Collection<String> searchPropertyIds = getSearchPropertyIds(entityId);
    return getSearchProperties(entityId, searchPropertyIds.toArray(new String[0]));
  }

  /**
   * Retrieves the properties used when searching for a entity of the given type, restricted to those
   * identified by {@code searchPropertyIds}
   * @param entityId the entity id
   * @param searchPropertyIds the ids of the search properties to retrieve
   * @return the search properties
   * @see Entity.Definition#setSearchPropertyIds(String...)
   */
  public final Collection<Property.ColumnProperty> getSearchProperties(final String entityId, final String... searchPropertyIds) {
    if (searchPropertyIds != null && searchPropertyIds.length > 0) {
      return Arrays.stream(searchPropertyIds).map(propertyId -> getColumnProperty(entityId, propertyId)).collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  /**
   * Returns the properties comprising the primary key of entities of the type identified by {@code entityId}
   * @param entityId the entity id
   * @return a list containing the primary key properties of the entity identified by {@code entityId}
   */
  public final List<Property.ColumnProperty> getPrimaryKeyProperties(final String entityId) {
    return getDefinition(entityId).getPrimaryKeyProperties();
  }

  /**
   * Returns the readOnly status of entities of the type identified by {@code entityId}
   * @param entityId the entity id
   * @return true if entities identified by {@code entityId} are read only
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final boolean isReadOnly(final String entityId) {
    return getDefinition(entityId).isReadOnly();
  }

  /**
   * Returns whether or not the entity identified by {@code entityId} is based on a small database.
   * @param entityId the entity id
   * @return true if the entity identified by {@code entityId} is based on a small dataset
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final boolean isSmallDataset(final String entityId) {
    return getDefinition(entityId).isSmallDataset();
  }

  /**
   * Returns whether or not the data the entity identified by {@code entityId} is based on is static,
   * that is, rarely changes
   * @param entityId the entity id
   * @return true if the entity identified by {@code entityId} is based on static data
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final boolean isStaticData(final String entityId) {
    return getDefinition(entityId).isStaticData();
  }

  /**
   * @param entityId the entity id
   * @return the default order by for this entity
   */
  public final Entity.OrderBy getOrderBy(final String entityId) {
    return getDefinition(entityId).getOrderBy();
  }

  /**
   * @param entityId the entity id
   * @return a comma separated list of columns to use in the group by clause
   */
  public final String getGroupByClause(final String entityId) {
    return getDefinition(entityId).getGroupByClause();
  }

  /**
   * @param entityId the entity id
   * @return the having clause associated with this entity
   */
  public final String getHavingClause(final String entityId) {
    return getDefinition(entityId).getHavingClause();
  }

  /**
   * @param entityId the entity id
   * @return the name of the table used to select entities identified by {@code entityId}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getSelectTableName(final String entityId) {
    return getDefinition(entityId).getSelectTableName();
  }

  /**
   * @param entityId the entity id
   * @return the name of the table on which entities identified by {@code entityId} are based
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getTableName(final String entityId) {
    return getDefinition(entityId).getTableName();
  }

  /**
   * @param entityId the entity id
   * @return the sql query used when selecting entities identified by {@code entityId}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getSelectQuery(final String entityId) {
    return getDefinition(entityId).getSelectQuery();
  }

  /**
   * @param entityId the entity id
   * @return true if the select query for the given entity, if any, contains a where clause
   */
  public final boolean selectQueryContainsWhereClause(final String entityId) {
    return getDefinition(entityId).selectQueryContainsWhereClause();
  }

  /**
   * @param entityId the entity id
   * @return the query string used to select entities identified by {@code entityId}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getSelectColumnsString(final String entityId) {
    return getDefinition(entityId).getSelectColumnsString();
  }

  /**
   * @param entityId the entity id
   * @return the primary key generator for entities identified by {@code entityId}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final Entity.KeyGenerator getKeyGenerator(final String entityId) {
    return getDefinition(entityId).getKeyGenerator();
  }

  /**
   * @param entityId the entity id
   * @return the type of primary key generator used by entities identified by {@code entityId}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final Entity.KeyGenerator.Type getKeyGeneratorType(final String entityId) {
    return getDefinition(entityId).getKeyGeneratorType();
  }

  /**
   * @param entityId the entity id
   * @return the {@link Entity.ToString} instance used to provide string representations
   * of entities of the given type
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final Entity.ToString getStringProvider(final String entityId) {
    return getDefinition(entityId).getStringProvider();
  }

  /**
   * @param entityId the entity id
   * @return the default Comparator to use when sorting entities of the given type
   */
  public final Comparator<Entity> getComparator(final String entityId) {
    return getDefinition(entityId).getComparator();
  }

  /**
   * Returns true if the value for the primary key of this entity is automatically generated, either by the framework,
   * such as values queried from sequences or set by triggers. If not the primary key value must be set manually
   * before the entity is inserted.
   * @param entityId the entity id
   * @return true if the value for the primary key is automatically generated
   */
  public final boolean isPrimaryKeyAutoGenerated(final String entityId) {
    return !getKeyGeneratorType(entityId).isManual();
  }

  /**
   * @param entityId the entity id
   * @return true if the primary key of the given type of entity is comprised of a single integer value
   */
  public final boolean hasSingleIntegerPrimaryKey(final String entityId) {
    final List<Property.ColumnProperty> primaryKeyProperties = getDefinition(entityId).getPrimaryKeyProperties();
    return primaryKeyProperties.size() == 1 && primaryKeyProperties.get(0).isInteger();
  }

  /**
   * Retrieves the column properties comprising the entity identified by {@code entityId}
   * @param entityId the entity id
   * @param includePrimaryKeyProperties if true primary key properties are included
   * @param includeReadOnly if true then properties that are marked as 'read only' are included
   * @param includeNonUpdatable if true then non updatable properties are included
   * @return a list containing the database properties (properties that map to database columns) comprising
   * the entity identified by {@code entityId}
   */
  public final List<Property.ColumnProperty> getColumnProperties(final String entityId,
                                                                 final boolean includePrimaryKeyProperties,
                                                                 final boolean includeReadOnly,
                                                                 final boolean includeNonUpdatable) {
    final List<Property.ColumnProperty> properties = new ArrayList<>(getDefinition(entityId).getColumnProperties());
    properties.removeIf(property ->
            !includeReadOnly && property.isReadOnly()
                    || !includeNonUpdatable && !property.isUpdatable()
                    || !includePrimaryKeyProperties && property.isPrimaryKeyProperty());

    return properties;
  }

  /**
   * @param entityId the entity id
   * @return a list containing the visible (non-hidden) properties
   * in the entity identified by {@code entityId}
   */
  public final List<Property> getVisibleProperties(final String entityId) {
    Objects.requireNonNull(entityId, ENTITY_ID_PARAM);
    return getDefinition(entityId).getVisibleProperties();
  }

  /**
   * @param entityId the entityId
   * @param propertyIds the ids of the properties to retrieve
   * @return the {@link Property.ColumnProperty}s specified by the given property ids
   * @throws IllegalArgumentException in case a given propertyId does not represent a {@link Property.ColumnProperty}
   */
  public final List<Property.ColumnProperty> getColumnProperties(final String entityId, final Collection<String> propertyIds) {
    if (propertyIds == null || propertyIds.isEmpty()) {
      return Collections.emptyList();
    }

    return propertyIds.stream().map(propertyId -> getColumnProperty(entityId, propertyId)).collect(Collectors.toList());
  }

  /**
   * @param entityId the entity id
   * @param propertyId the property id
   * @return the column property identified by property id
   * @throws IllegalArgumentException in case the propertyId does not represent a {@link Property.ColumnProperty}
   */
  public final Property.ColumnProperty getColumnProperty(final String entityId, final String propertyId) {
    final Property property = getProperty(entityId, propertyId);
    if (!(property instanceof Property.ColumnProperty)) {
      throw new IllegalArgumentException(propertyId + ", " + property.getClass() + " does not implement Property.ColumnProperty");
    }

    return (Property.ColumnProperty) property;
  }

  /**
   * @param entityId the entity id
   * @param propertyId the property id
   * @return the property identified by {@code propertyId} in the entity identified by {@code entityId}
   * @throws IllegalArgumentException in case no such property exists
   */
  public final Property getProperty(final String entityId, final String propertyId) {
    Objects.requireNonNull(entityId, ENTITY_ID_PARAM);
    Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
    final Property property = getDefinition(entityId).getPropertyMap().get(propertyId);
    if (property == null) {
      throw new IllegalArgumentException("Property '" + propertyId + "' not found in entity: " + entityId);
    }

    return property;
  }

  /**
   * @param entityId the entity id
   * @param propertyIds the ids of the properties to retrieve
   * @return a list containing the properties identified by {@code propertyIds}, found in
   * the entity identified by {@code entityId}
   */
  public final List<Property> getProperties(final String entityId, final Collection<String> propertyIds) {
    Objects.requireNonNull(propertyIds, PROPERTY_ID_PARAM);
    return getProperties(entityId, propertyIds.toArray(new String[0]));
  }

  /**
   * @param entityId the entity id
   * @param propertyIds the ids of the properties to retrieve
   * @return a list containing the properties identified by {@code propertyIds}, found in
   * the entity identified by {@code entityId}
   */
  public final List<Property> getProperties(final String entityId, final String... propertyIds) {
    Objects.requireNonNull(entityId, ENTITY_ID_PARAM);
    Objects.requireNonNull(propertyIds, PROPERTY_ID_PARAM);

    return Arrays.stream(propertyIds).map(propertyId -> getProperty(entityId, propertyId)).collect(Collectors.toList());
  }

  /**
   * @param entityId the entity id
   * @param includeHidden true if hidden properties should be included in the result
   * @return a collection containing the properties found in the entity identified by {@code entityId}
   */
  public final Collection<Property> getProperties(final String entityId, final boolean includeHidden) {
    return includeHidden ? getProperties(entityId) : getVisibleProperties(entityId);
  }

  /**
   * @param entityId the entity id
   * @return a list containing all database properties found in the entity identified by {@code entityId},
   * that is, properties that map to database columns
   */
  public final List<Property.ColumnProperty> getColumnProperties(final String entityId) {
    return getDefinition(entityId).getColumnProperties();
  }

  /**
   * @param entityId the entity id
   * @return a list containing all transient database properties found in the entity identified by {@code entityId},
   * that is, properties that do not map to database columns
   */
  public final List<Property.TransientProperty> getTransientProperties(final String entityId) {
    return getDefinition(entityId).getTransientProperties();
  }

  /**
   * @param entityId the entity id
   * @return a list containing all the foreign key properties found in the entity
   * identified by {@code entityId}
   */
  public final List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityId) {
    return getDefinition(entityId).getForeignKeyProperties();
  }

  /**
   * @param entityId the entity id
   * @return true if the given entity contains denormalized properties
   */
  public final boolean hasDenormalizedProperties(final String entityId) {
    return getDefinition(entityId).hasDenormalizedProperties();
  }

  /**
   * @param entityId the entity id
   * @param foreignKeyPropertyId the foreign key id
   * @return a list containing all denormalized properties of the entity identified by {@code entityId}
   * which source is the entity referenced by {@code foreignKeyPropertyId}
   */
  public final List<Property.DenormalizedProperty> getDenormalizedProperties(final String entityId,
                                                                             final String foreignKeyPropertyId) {
    return getDefinition(entityId).getDenormalizedProperties(foreignKeyPropertyId);
  }

  /**
   * @param entityId the entity id
   * @param foreignKeyPropertyId the foreign key id
   * @return true if the entity identified by {@code entityId} contains denormalized properties
   * which source is the entity referenced by {@code foreignKeyPropertyId}
   */
  public final boolean hasDenormalizedProperties(final String entityId, final String foreignKeyPropertyId) {
    return getDefinition(entityId).hasDenormalizedProperties(foreignKeyPropertyId);
  }

  /**
   * Returns true if this entity contains properties which values are derived from the value of the given property
   * @param entityId the entityId
   * @param propertyId the id of the property
   * @return true if any properties are derived from the given property
   */
  public final boolean hasDerivedProperties(final String entityId, final String propertyId) {
    return getDefinition(entityId).hasDerivedProperties(propertyId);
  }

  /**
   * Returns the properties which values are derived from the value of the given property,
   * an empty collection if no such derived properties exist
   * @param entityId the entityId
   * @param propertyId the id of the property
   * @return a collection containing the properties which are derived from the given property
   */
  public final Collection<Property.DerivedProperty> getDerivedProperties(final String entityId, final String propertyId) {
    return getDefinition(entityId).getDerivedProperties(propertyId);
  }

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param entityId the id of the entity from which to retrieve the foreign key properties
   * @param foreignEntityId the id of the referenced entity
   * @return a List containing the properties, an empty list is returned in case no properties fit the condition
   */
  public final List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityId, final String foreignEntityId) {
    return getForeignKeyProperties(entityId).stream().filter(foreignKeyProperty ->
            foreignKeyProperty.getForeignEntityId().equals(foreignEntityId)).collect(Collectors.toList());
  }

  /**
   * @param entityId the entity id
   * @param propertyId the property id
   * @return the Property.ForeignKeyProperty with the given propertyId
   * @throws IllegalArgumentException in case no such property exists
   */
  public final Property.ForeignKeyProperty getForeignKeyProperty(final String entityId, final String propertyId) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties(entityId);
    for (int i = 0; i < foreignKeyProperties.size(); i++) {
      final Property.ForeignKeyProperty foreignKeyProperty = foreignKeyProperties.get(i);
      if (foreignKeyProperty.is(propertyId)) {
        return foreignKeyProperty;
      }
    }

    throw new IllegalArgumentException("Foreign key property with id: " + propertyId + " not found in entity of type: " + entityId);
  }

  /**
   * @param entityId the entityId
   * @return all foreign keys referencing entities of type {@code entityId}
   */
  public final Collection<Property.ForeignKeyProperty> getForeignKeyReferences(final String entityId) {
    List<Property.ForeignKeyProperty> foreignKeyReferences = foreignKeyReferenceMap.get(entityId);
    if (foreignKeyReferences == null) {
      foreignKeyReferences = new ArrayList<>();
      for (final String definedEntityId : entityDefinitions.keySet()) {
        for (final Property.ForeignKeyProperty foreignKeyProperty : getDefinition(definedEntityId).getForeignKeyProperties()) {
          if (foreignKeyProperty.getForeignEntityId().equals(entityId)) {
            foreignKeyReferences.add(foreignKeyProperty);
          }
        }
      }
      foreignKeyReferenceMap.put(entityId, foreignKeyReferences);
    }

    return foreignKeyReferences;
  }

  /**
   * @param entityId the entity id
   * @return the properties comprising the given entity type
   */
  public final List<Property> getProperties(final String entityId) {
    return getDefinition(entityId).getProperties();
  }

  /**
   * @param entityId the entity id
   * @return the caption associated with the given entity type
   */
  public final String getCaption(final String entityId) {
    return getDefinition(entityId).getCaption();
  }

  /**
   * @param entityId the entityId
   * @return the validator for the given entity type
   */
  public final Entity.Validator getValidator(final String entityId) {
    return getDefinition(entityId).getValidator();
  }

  /**
   * @return the entityIds of all defined entities
   */
  public final Collection<String> getDefinedEntities() {
    return new ArrayList<>(entityDefinitions.keySet());
  }

  /**
   * @param entityId the entity id
   * @return true if the entity is defined
   */
  public final boolean isDefined(final String entityId) {
    return entityDefinitions.containsKey(entityId);
  }

  /**
   * @param entityId the entity id
   * @return a list containing all updatable properties associated with the given entity id
   */
  public final List<Property> getUpdatableProperties(final String entityId) {
    final List<Property.ColumnProperty> columnProperties = getColumnProperties(entityId,
            getKeyGeneratorType(entityId).isManual(), false, false);
    columnProperties.removeIf(property -> property.isForeignKeyProperty() || property.isDenormalized());
    final List<Property> updatable = new ArrayList<>(columnProperties);
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties(entityId);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      if (!foreignKeyProperty.isReadOnly() && foreignKeyProperty.isUpdatable()) {
        updatable.add(foreignKeyProperty);
      }
    }
    Properties.sort(updatable);

    return updatable;
  }

  /**
   * Creates an empty Entity instance returning the given string on a call to toString(), all other
   * method calls are routed to an empty Entity instance.
   * @param entityId the entityId
   * @param toStringValue the string to return by a call to toString() on the resulting entity
   * @return an empty entity wrapping a string
   */
  public final Entity createToStringEntity(final String entityId, final String toStringValue) {
    final Entity entity = entity(entityId);
    return Util.initializeProxy(Entity.class, (proxy, method, args) -> {
      if ("toString".equals(method.getName())) {
        return toStringValue;
      }

      return method.invoke(entity, args);
    });
  }

  /**
   * @return a Serializer, if one is available on the classpath
   */
  @SuppressWarnings({"unchecked"})
  public final Serializer<Entity> getEntitySerializer() {
    if (!entitySerializerAvailable()) {
      throw new IllegalArgumentException("Required configuration property is missing: " + Domain.ENTITY_SERIALIZER_CLASS);
    }

    try {
      final String serializerClass = Domain.ENTITY_SERIALIZER_CLASS.get();

      return (Serializer<Entity>) Class.forName(serializerClass).getConstructor().newInstance();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param entityId the entity id
   * @param propertyIds the property ids
   * @return the given properties sorted by caption, or if that is not available, property id
   */
  public final List<Property> getSortedProperties(final String entityId, final Collection<String> propertyIds) {
    final List<Property> properties = new ArrayList<>(getProperties(entityId, propertyIds));
    Properties.sort(properties);

    return properties;
  }

  /**
   * @return true if a entity serializer is specified and available on the classpath
   */
  public final boolean entitySerializerAvailable() {
    final String serializerClass = ENTITY_SERIALIZER_CLASS.get();
    return serializerClass != null && Util.onClasspath(serializerClass);
  }

  /**
   * Adds the given Operation to this domain
   * @param operation the operation to add
   * @throws IllegalArgumentException in case an operation with the same id has already been added
   */
  public final void addOperation(final DatabaseConnection.Operation operation) {
    if (databaseOperations.containsKey(operation.getId())) {
      throw new IllegalArgumentException("Operation already defined: " + databaseOperations.get(operation.getId()).getName());
    }

    databaseOperations.put(operation.getId(), operation);
  }

  /**
   * @param <C> the type of the database connection this procedure requires
   * @param procedureId the procedure id
   * @return the procedure
   * @throws IllegalArgumentException in case the procedure is not found
   */
  public final <C> DatabaseConnection.Procedure<C> getProcedure(final String procedureId) {
    final DatabaseConnection.Operation operation = databaseOperations.get(procedureId);
    if (operation == null) {
      throw new IllegalArgumentException("Procedure not found: " + procedureId);
    }

    return (DatabaseConnection.Procedure) operation;
  }

  /**
   * @param <C> the type of the database connection this function requires
   * @param functionId the function id
   * @return the function
   * @throws IllegalArgumentException in case the function is not found
   */
  public final <C> DatabaseConnection.Function<C> getFunction(final String functionId) {
    final DatabaseConnection.Operation operation = databaseOperations.get(functionId);
    if (operation == null) {
      throw new IllegalArgumentException("Function not found: " + functionId);
    }

    return (DatabaseConnection.Function) operation;
  }

  /**
   * Registers this instance for lookup via {@link Domain#getDomain(String)}, required for serialization
   * of domain objects, entities and related classes.
   * @return this Domain instance
   * @see #getDomainId()
   */
  public final Domain registerDomain() {
    return registerDomain(domainId, this);
  }

  /**
   * @return a new OrderBy instance
   */
  public static final Entity.OrderBy orderBy() {
    return new DefaultOrderBy();
  }

  /**
   * Instantiates a primary key generator which fetches the current maximum primary key value and increments
   * it by one prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param tableName the table name
   * @param columnName the primary key column name
   * @return a incrementing primary key generator
   */
  public final Entity.KeyGenerator incrementKeyGenerator(final String tableName, final String columnName) {
    return new IncrementKeyGenerator(tableName, columnName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values from a sequence prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param sequenceName the sequence name
   * @return a sequence based primary key generator
   */
  public final Entity.KeyGenerator sequenceKeyGenerator(final String sequenceName) {
    return new SequenceKeyGenerator(sequenceName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values using the given query prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param query a query for retrieving the primary key value
   * @return a query based primary key generator
   */
  public final Entity.KeyGenerator queriedKeyGenerator(final String query) {
    return new AbstractQueriedKeyGenerator() {
      @Override
      protected String getQuery(final Database database) {
        return query;
      }
    };
  }

  /**
   * Instantiates a primary key generator which fetches automatically incremented primary key values after insert.
   * @param valueSource the value source, whether a sequence or a table name
   * @return a auto-increment based primary key generator
   */
  public final Entity.KeyGenerator automaticKeyGenerator(final String valueSource) {
    return new AutomaticKeyGenerator(valueSource);
  }

  /**
   * @param domainId the id of the domain for which to retrieve the entity definitions
   * @return the domain instance registered for the given domainId
   * @throws IllegalArgumentException in case the domain has not been registered
   * @see #registerDomain()
   */
  public static Domain getDomain(final String domainId) {
    final Domain domain = REGISTERED_DOMAINS.get(domainId);
    if (domain == null) {
      throw new IllegalArgumentException("Domain '" + domainId + "' has not been registered");
    }

    return domain;
  }

  /**
   * @return all domains that have been registered via {@link #registerDomain()}
   */
  public static Collection<Domain> getRegisteredDomains() {
    return Collections.unmodifiableCollection(REGISTERED_DOMAINS.values());
  }

  /**
   * @param entityId the entity id
   * @return the definition of the given entity
   * @throws IllegalArgumentException in case no entity with the given id has been defined
   */
  protected final Entity.Definition getDefinition(final String entityId) {
    final Entity.Definition definition = entityDefinitions.get(Objects.requireNonNull(entityId, ENTITY_ID_PARAM));
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityId);
    }

    return definition;
  }

  private Map<String, Property> initializePropertyMap(final String domainId, final String entityId, final Property... properties) {
    final Map<String, Property> propertyMap = new LinkedHashMap<>(properties.length);
    for (final Property property : properties) {
      validateAndAddProperty(property, domainId, entityId, propertyMap);
      if (property instanceof Property.ForeignKeyProperty) {
        initializeForeignKeyProperty(domainId, entityId, propertyMap, (Property.ForeignKeyProperty) property);
      }
    }
    checkIfPrimaryKeyIsSpecified(entityId, propertyMap);

    return Collections.unmodifiableMap(propertyMap);
  }

  private void initializeForeignKeyProperty(final String domainId, final String entityId, final Map<String, Property> propertyMap,
                                            final Property.ForeignKeyProperty foreignKeyProperty) {
    final List<Property.ColumnProperty> properties = foreignKeyProperty.getProperties();
    if (!entityId.equals(foreignKeyProperty.getForeignEntityId()) && Entity.Definition.STRICT_FOREIGN_KEYS.get()) {
      final Entity.Definition foreignEntity = entityDefinitions.get(foreignKeyProperty.getForeignEntityId());
      if (foreignEntity == null) {
        throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getForeignEntityId()
                + "' referenced by entity '" + entityId + "' via foreign key property '"
                + foreignKeyProperty.getPropertyId() + "' has not been defined");
      }
      if (properties.size() != foreignEntity.getPrimaryKeyProperties().size()) {
        throw new IllegalArgumentException("Number of column properties in '" + entityId + "." + foreignKeyProperty.getPropertyId() +
                "' does not match the number of foreign properties in the referenced entity '" + foreignKeyProperty.getForeignEntityId() + "'");
      }
    }
    for (final Property.ColumnProperty property : properties) {
      if (!(property instanceof Property.MirrorProperty)) {
        validateAndAddProperty(property, domainId, entityId, propertyMap);
      }
    }
  }

  private static void validateAndAddProperty(final Property property, final String domainId, final String entityId,
                                             final Map<String, Property> propertyMap) {
    checkIfUniquePropertyId(property, entityId, propertyMap);
    property.setDomainId(domainId);
    property.setEntityId(entityId);
    propertyMap.put(property.getPropertyId(), property);
  }

  private static void checkIfUniquePropertyId(final Property property, final String entityId, final Map<String, Property> propertyMap) {
    if (propertyMap.containsKey(property.getPropertyId())) {
      throw new IllegalArgumentException("Property with id " + property.getPropertyId()
              + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
              + " has already been defined as: " + propertyMap.get(property.getPropertyId()) + " in entity: " + entityId);
    }
  }

  private static void checkIfPrimaryKeyIsSpecified(final String entityId, final Map<String, Property> propertyMap) {
    final Collection<Integer> usedPrimaryKeyIndexes = new ArrayList<>();
    boolean primaryKeyPropertyFound = false;
    for (final Property property : propertyMap.values()) {
      if (property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isPrimaryKeyProperty()) {
        final Integer index = ((Property.ColumnProperty) property).getPrimaryKeyIndex();
        if (usedPrimaryKeyIndexes.contains(index)) {
          throw new IllegalArgumentException("Primary key index " + index + " in property " + property + " has already been used");
        }
        usedPrimaryKeyIndexes.add(index);
        primaryKeyPropertyFound = true;
      }
    }
    if (primaryKeyPropertyFound) {
      return;
    }

    throw new IllegalArgumentException("Entity is missing a primary key: " + entityId);
  }

  private Domain registerDomain(final String domainId, final Domain domain) {
    REGISTERED_DOMAINS.put(domainId, domain);

    return domain;
  }

  private static List<Property.ForeignKeyProperty> getForeignKeyProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof Property.ForeignKeyProperty)
            .map(property -> (Property.ForeignKeyProperty) property).collect(Collectors.toList());
  }

  private static List<Property.ColumnProperty> getColumnProperties(final Collection<Property> properties) {
    final List<Property.ColumnProperty> columnProperties = properties.stream()
            .filter(property -> property instanceof Property.ColumnProperty)
            .map(property -> (Property.ColumnProperty) property).collect(Collectors.toList());

    for (int idx = 0; idx < columnProperties.size(); idx++) {
      columnProperties.get(idx).setSelectIndex(idx + 1);
    }

    return columnProperties;
  }

  private static List<Property.TransientProperty> getTransientProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof Property.TransientProperty)
            .map(property -> (Property.TransientProperty) property)
            .collect(Collectors.toList());
  }

  /**
   * Provides String representations of {@link Entity} instances.<br>
   * Given a {@link Entity} instance named entity containing the following mappings:
   * <pre>
   * "key1" -&#62; value1
   * "key2" -&#62; value2
   * "key3" -&#62; value3
   * "key4" -&#62; {Entity instance with a single mapping "refKey" -&#62; refValue}
   * </pre>
   * {@code
   * Domain.StringProvider provider = new Domain.StringProvider();<br>
   * provider.addText("key1=").addValue("key1").addText(", key3='").addValue("key3")<br>
   *         .addText("' foreign key value=").addForeignKeyValue("key4", "refKey");<br>
   * System.out.println(provider.toString(entity));<br>
   * }
   * <br>
   * outputs the following String:<br><br>
   * {@code key1=value1, key3='value3' foreign key value=refValue}
   */
  public static final class StringProvider implements Entity.ToString {

    /**
     * Holds the ValueProviders used when constructing the String representation
     */
    private final List<ValueProvider> valueProviders = new ArrayList<>();

    /**
     * Instantiates a new {@link StringProvider} instance
     */
    public StringProvider() {}

    /**
     * Instantiates a new {@link StringProvider} instance
     * @param propertyId the id of the property which value should be used for a string representation
     */
    public StringProvider(final String propertyId) {
      addValue(propertyId);
    }

    /** {@inheritDoc} */
    @Override
    public String toString(final Entity entity) {
      Objects.requireNonNull(entity, ENTITY_PARAM);

      return valueProviders.stream().map(valueProvider -> valueProvider.toString(entity)).collect(Collectors.joining());
    }

    /**
     * Adds the value mapped to the given key to this {@link StringProvider}
     * @param propertyId the id of the property which value should be added to the string representation
     * @return this {@link StringProvider} instance
     */
    public StringProvider addValue(final String propertyId) {
      Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
      valueProviders.add(new StringValueProvider(propertyId));
      return this;
    }

    /**
     * Adds the value mapped to the given key to this StringProvider
     * @param propertyId the id of the property which value should be added to the string representation
     * @param format the Format to use when appending the value
     * @return this {@link StringProvider} instance
     */
    public StringProvider addFormattedValue(final String propertyId, final Format format) {
      Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
      Objects.requireNonNull(format, "format");
      valueProviders.add(new FormattedValueProvider(propertyId, format));
      return this;
    }

    /**
     * Adds the value mapped to the given property in the {@link Entity} instance mapped to the given foreignKeyProperty
     * to this {@link StringProvider}
     * @param foreignKeyProperty the foreign key property
     * @param propertyId the id of the property in the referenced entity to use
     * @return this {@link StringProvider} instance
     */
    public StringProvider addForeignKeyValue(final Property.ForeignKeyProperty foreignKeyProperty,
                                             final String propertyId) {
      Objects.requireNonNull(foreignKeyProperty, "foreignKeyProperty");
      Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
      valueProviders.add(new ForeignKeyValueProvider(foreignKeyProperty, propertyId));
      return this;
    }

    /**
     * Adds the given static text to this {@link StringProvider}
     * @param text the text to add
     * @return this {@link StringProvider} instance
     */
    public StringProvider addText(final String text) {
      valueProviders.add(new StaticTextProvider(text));
      return this;
    }

    private interface ValueProvider extends Serializable {
      /**
       * @param entity the entity
       * @return a String representation of a property value from the given entity
       */
      String toString(final Entity entity);
    }

    private static final class FormattedValueProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final String propertyId;
      private final Format format;

      private FormattedValueProvider(final String propertyId, final Format format) {
        this.propertyId = propertyId;
        this.format = format;
      }

      @Override
      public String toString(final Entity entity) {
        if (entity.isValueNull(propertyId)) {
          return "";
        }

        return format.format(entity.get(propertyId));
      }
    }

    private static final class ForeignKeyValueProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final Property.ForeignKeyProperty foreignKeyProperty;
      private final String propertyId;

      private ForeignKeyValueProvider(final Property.ForeignKeyProperty foreignKeyProperty,
                                      final String propertyId) {
        this.foreignKeyProperty = foreignKeyProperty;
        this.propertyId = propertyId;
      }

      @Override
      public String toString(final Entity entity) {
        if (entity.isValueNull(foreignKeyProperty)) {
          return "";
        }

        return entity.getForeignKey(foreignKeyProperty).getAsString(propertyId);
      }
    }

    private static final class StringValueProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final String propertyId;

      private StringValueProvider(final String propertyId) {
        this.propertyId = propertyId;
      }

      @Override
      public String toString(final Entity entity) {
        return entity.getAsString(propertyId);
      }
    }

    private static final class StaticTextProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final String text;

      private StaticTextProvider(final String text) {
        this.text = text;
      }

      @Override
      public String toString(final Entity entity) {
        return text;
      }
    }
  }

  /**
   * A default {@link Entity.Validator} implementation providing null validation for properties marked as not null,
   * range validation for numerical properties with max and/or min values specified and string length validation
   * based on the specified max length.
   * This Validator can be extended to provide further validation.
   * @see Property#setNullable(boolean)
   * @see Property#setMin(double)
   * @see Property#setMax(double)
   * @see Property#setMaxLength(int)
   */
  public static class Validator extends DefaultValueMap.DefaultValidator<Property, Entity> implements Entity.Validator {

    private static final long serialVersionUID = 1;

    private final boolean performNullValidation;

    /**
     * Instantiates a new {@link Entity.Validator}
     */
    public Validator() {
      this(true);
    }

    /**
     * Instantiates a new {@link Entity.Validator}
     * @param performNullValidation if true then automatic null validation is performed
     */
    public Validator(final boolean performNullValidation) {
      this.performNullValidation = performNullValidation;
    }

    /**
     * Returns true if the given property accepts a null value for the given entity,
     * by default this method simply returns {@code property.isNullable()}
     * @param entity the entity being validated
     * @param property the property
     * @return true if the property accepts a null value
     */
    @Override
    public boolean isNullable(final Entity entity, final Property property) {
      return property.isNullable();
    }

    /**
     * Validates all writable properties in the given entities
     * @param entities the entities to validate
     * @throws ValidationException in case validation fails
     */
    @Override
    public final void validate(final Collection<Entity> entities) throws ValidationException {
      for (final Entity entity : entities) {
        validate(entity);
      }
    }

    /**
     * Validates all writable properties in the given entity
     * @param entity the entity to validate
     * @throws ValidationException in case validation fails
     */
    @Override
    public void validate(final Entity entity) throws ValidationException {
      Objects.requireNonNull(entity, ENTITY_PARAM);
      for (final Property property : entity.getProperties()) {
        if (!property.isReadOnly()) {
          validate(entity, property);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final Entity entity, final Property property) throws ValidationException {
      Objects.requireNonNull(entity, ENTITY_PARAM);
      if (performNullValidation && !isForeignKeyProperty(property)) {
        performNullValidation(entity, property);
      }
      if (property.isNumerical()) {
        performRangeValidation(entity, property);
      }
      else if (property.isString()) {
        performLengthValidation(entity, property);
      }
    }

    /** {@inheritDoc} */
    @Override
    public final void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException {
      Objects.requireNonNull(entity, ENTITY_PARAM);
      Objects.requireNonNull(property, PROPERTY_PARAM);
      if (entity.isValueNull(property)) {
        return;
      }

      final Number value = (Number) entity.get(property);
      if (value.doubleValue() < (property.getMin() == null ? Double.NEGATIVE_INFINITY : property.getMin())) {
        throw new RangeValidationException(property.getPropertyId(), value, "'" + property + "' " +
                MESSAGES.getString("property_value_too_small") + " " + property.getMin());
      }
      if (value.doubleValue() > (property.getMax() == null ? Double.POSITIVE_INFINITY : property.getMax())) {
        throw new RangeValidationException(property.getPropertyId(), value, "'" + property + "' " +
                MESSAGES.getString("property_value_too_large") + " " + property.getMax());
      }
    }

    /** {@inheritDoc} */
    @Override
    public final void performNullValidation(final Entity entity, final Property property) throws NullValidationException {
      Objects.requireNonNull(entity, ENTITY_PARAM);
      Objects.requireNonNull(property, PROPERTY_PARAM);
      if (!isNullable(entity, property) && entity.isValueNull(property)) {
        if ((entity.getKey().isNull() || entity.getOriginalKey().isNull()) && !(property instanceof Property.ForeignKeyProperty)) {
          //a new entity being inserted, allow null for columns with default values and auto generated primary key values
          final boolean nonKeyColumnPropertyWithoutDefaultValue = isNonKeyColumnPropertyWithoutDefaultValue(property);
          final boolean primaryKeyPropertyWithoutAutoGenerate = isPrimaryKeyPropertyWithoutAutoGenerate(entity, property);
          if (nonKeyColumnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
            throw new NullValidationException(property.getPropertyId(), MESSAGES.getString(MSG_PROPERTY_VALUE_IS_REQUIRED) + ": " + property);
          }
        }
        else {
          throw new NullValidationException(property.getPropertyId(), MESSAGES.getString(MSG_PROPERTY_VALUE_IS_REQUIRED) + ": " + property);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public void performLengthValidation(final Entity entity, final Property property) throws LengthValidationException {
      Objects.requireNonNull(entity, ENTITY_PARAM);
      Objects.requireNonNull(property, PROPERTY_PARAM);
      if (entity.isValueNull(property)) {
        return;
      }

      final int maxLength = property.getMaxLength();
      final String value = (String) entity.get(property);
      if (maxLength != -1 && value.length() > maxLength) {
        throw new LengthValidationException(property.getPropertyId(), value, "'" + property + "' " +
                MESSAGES.getString("property_value_too_long") + " " + maxLength);
      }
    }

    private boolean isPrimaryKeyPropertyWithoutAutoGenerate(final Entity entity, final Property property) {
      return (property instanceof Property.ColumnProperty
              && ((Property.ColumnProperty) property).isPrimaryKeyProperty()) && entity.getKeyGeneratorType().isManual();
    }

    /**
     * @param property the property
     * @return true if the property is a part of a foreign key
     */
    private static boolean isForeignKeyProperty(final Property property) {
      return property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isForeignKeyProperty();
    }

    private static boolean isNonKeyColumnPropertyWithoutDefaultValue(final Property property) {
      return property instanceof Property.ColumnProperty && !((Property.ColumnProperty) property).isPrimaryKeyProperty()
              && !((Property.ColumnProperty) property).columnHasDefaultValue();
    }
  }

  private abstract class AbstractQueriedKeyGenerator implements Entity.KeyGenerator {

    @Override
    public Type getType() {
      return Type.QUERY;
    }

    protected final Property.ColumnProperty getPrimaryKeyProperty(final String entityId) {
      return getPrimaryKeyProperties(entityId).get(0);
    }

    protected final void queryAndSet(final Entity entity, final Property.ColumnProperty keyProperty,
                                     final DatabaseConnection connection) throws SQLException {
      final Object value;
      switch (keyProperty.getColumnType()) {
        case Types.INTEGER:
          value = connection.queryInteger(getQuery(connection.getDatabase()));
          break;
        case Types.BIGINT:
          value = connection.queryLong(getQuery(connection.getDatabase()));
          break;
        default:
          throw new SQLException("Queried key generator only implemented for Types.INTEGER and Types.BIGINT datatypes", null, null);
      }
      entity.put(keyProperty, value);
    }

    protected abstract String getQuery(final Database database);
  }

  private final class IncrementKeyGenerator extends AbstractQueriedKeyGenerator {

    private final String query;

    private IncrementKeyGenerator(final String tableName, final String columnName) {
      this.query = "select max(" + columnName + ") + 1 from " + tableName;
    }

    @Override
    public Type getType() {
      return Type.INCREMENT;
    }

    @Override
    public void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {
      final Property.ColumnProperty primaryKeyProperty = getPrimaryKeyProperty(entity.getEntityId());
      if (entity.isValueNull(primaryKeyProperty)) {
        queryAndSet(entity, primaryKeyProperty, connection);
      }
    }

    @Override
    protected String getQuery(final Database database) {
      return query;
    }
  }

  private final class SequenceKeyGenerator extends AbstractQueriedKeyGenerator {

    private final String sequenceName;

    private SequenceKeyGenerator(final String sequenceName) {
      this.sequenceName = sequenceName;
    }

    @Override
    public Type getType() {
      return Type.SEQUENCE;
    }

    @Override
    public void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {
      final Property.ColumnProperty primaryKeyProperty = getPrimaryKeyProperty(entity.getEntityId());
      if (entity.isValueNull(primaryKeyProperty)) {
        queryAndSet(entity, primaryKeyProperty, connection);
      }
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getSequenceQuery(sequenceName);
    }
  }

  private final class AutomaticKeyGenerator extends AbstractQueriedKeyGenerator {

    private final String valueSource;

    private AutomaticKeyGenerator(final String valueSource) {
      this.valueSource = valueSource;
    }

    @Override
    public Type getType() {
      return Type.AUTOMATIC;
    }

    @Override
    public void afterInsert(final Entity entity, final DatabaseConnection connection, final Statement statement) throws SQLException {
      queryAndSet(entity, getPrimaryKeyProperty(entity.getEntityId()), connection);
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getAutoIncrementQuery(valueSource);
    }
  }

  private static final class DefaultOrderBy implements Entity.OrderBy {

    private static final long serialVersionUID = 1;

    private final List<OrderByProperty> orderByProperties = new LinkedList<>();

    @Override
    public Entity.OrderBy ascending(final String... propertyIds) {
      add(false, propertyIds);
      return this;
    }

    @Override
    public Entity.OrderBy descending(final String... propertyIds) {
      add(true, propertyIds);
      return this;
    }

    @Override
    public List<OrderByProperty> getOrderByProperties() {
      return Collections.unmodifiableList(orderByProperties);
    }

    private void add(final boolean descending, final String... propertyIds) {
      Objects.requireNonNull(propertyIds, "propertyIds");
      for (final String propertyId : propertyIds) {
        final OrderByProperty property = new DefaultOrderByProperty(propertyId, descending);
        if (orderByProperties.contains(property)) {
          throw new IllegalArgumentException("Order by already contains property: " + propertyId);
        }
        orderByProperties.add(property);
      }
    }

    private static final class DefaultOrderByProperty implements OrderByProperty {

      private static final long serialVersionUID = 1;

      private final String propertyId;
      private final boolean descending;

      private DefaultOrderByProperty(final String propertyId, final boolean descending) {
        this.propertyId = Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
        this.descending = descending;
      }

      @Override
      public String getPropertyId() {
        return propertyId;
      }

      @Override
      public boolean isDescending() {
        return descending;
      }

      @Override
      public boolean equals(final Object object) {
        if (this == object) {
          return true;
        }
        if (object == null || getClass() != object.getClass()) {
          return false;
        }

        return propertyId.equals(((DefaultOrderByProperty) object).propertyId);
      }

      @Override
      public int hashCode() {
        return propertyId.hashCode();
      }
    }
  }
}
