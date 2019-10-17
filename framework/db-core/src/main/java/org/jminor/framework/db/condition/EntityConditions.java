/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.condition.Condition;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.Conjunction.AND;
import static org.jminor.common.Conjunction.OR;
import static org.jminor.common.Util.nullOrEmpty;
import static org.jminor.common.db.condition.Condition.Type.LIKE;
import static org.jminor.common.db.condition.Conditions.conditionSet;
import static org.jminor.framework.domain.Entities.getValues;

/**
 * A class for creating query conditions.
 */
public final class EntityConditions {

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";
  private static final String ENTITY_ID_PARAM = "entityId";
  private static final String CONDITION_TYPE_PARAM = "conditionType";

  private final Domain domain;

  /**
   * Instantiates a new EntityConditions instance based on the given domain model
   * @param domain the domain model
   */
  public EntityConditions(final Domain domain) {
    this.domain = requireNonNull(domain, "domain");
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting the entity with the given key
   * @param key the key
   * @return a select condition based on the given key
   */
  public EntitySelectCondition selectCondition(final Entity.Key key) {
    return selectCondition(singletonList(requireNonNull(key, "key")));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting the entities with the given keys,
   * it is assumed they are all of the same type
   * @param keys the keys
   * @return a select condition based on the given keys
   */
  public EntitySelectCondition selectCondition(final List<Entity.Key> keys) {
    checkKeysParameter(keys);
    return new DefaultEntitySelectCondition(domain, keys.get(0).getEntityId(), createKeyCondition(keys));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities of the type identified by {@code entityId}
   * with a where condition based on the property identified by {@code propertyId}, the operators based on
   * {@code conditionType} and {@code value}. Note that {@code value} may be a single value, a Collection
   * of values or null.
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @param conditionType the condition type
   * @param value the condition value, can be a Collection of values
   * @return a select condition based on the given value
   */
  public EntitySelectCondition selectCondition(final String entityId, final String propertyId,
                                               final Condition.Type conditionType, final Object value) {
    return selectCondition(entityId, propertyCondition(entityId, propertyId, conditionType, value));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting all entities of the type identified by {@code entityId}
   * @param entityId the entity ID
   * @return a select condition encompassing all entities of the given type
   */
  public EntitySelectCondition selectCondition(final String entityId) {
    return new DefaultEntitySelectCondition(domain, entityId);
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities of the type identified by {@code entityId},
   * using the given {@link Condition}
   * @param entityId the entity ID
   * @param propertyCondition the column condition
   * @return a select condition based on the given column condition
   */
  public EntitySelectCondition selectCondition(final String entityId, final Condition<Property.ColumnProperty> propertyCondition) {
    return new DefaultEntitySelectCondition(domain, entityId, propertyCondition);
  }

  /**
   * Creates a {@link EntityCondition} instance specifying the entity of the type identified by {@code key}
   * @param key the primary key
   * @return a condition specifying the entity with the given primary key
   */
  public EntityCondition condition(final Entity.Key key) {
    return condition(singletonList(key));
  }

  /**
   * Creates a {@link EntityCondition} instance specifying the entities of the type identified by {@code key},
   * using the given {@link Condition}
   * @param entityId the entity ID
   * @param condition the column condition
   * @return a condition based on the given column condition
   */
  public EntityCondition condition(final String entityId, final Condition<Property.ColumnProperty> condition) {
    return new DefaultEntityCondition(entityId, condition);
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all of the same type
   * @param keys the primary keys
   * @return a condition specifying the entities having the given primary keys
   */
  public EntityCondition condition(final List<Entity.Key> keys) {
    checkKeysParameter(keys);
    return new DefaultEntityCondition(keys.get(0).getEntityId(), createKeyCondition(keys));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance specifying all entities of the type identified by {@code entityId}
   * @param entityId the entity ID
   * @return a condition specifying all entities of the given type
   */
  public EntityCondition condition(final String entityId) {
    return new DefaultEntityCondition(entityId);
  }

  /**
   * Creates a {@link EntityCondition} instance for specifying entities of the type identified by {@code entityId}
   * with a where condition based on the property identified by {@code propertyId}, the operators based on
   * {@code conditionType} and {@code value}. Note that {@code value} may be a single value, a Collection
   * of values or null.
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a condition based on the given value
   */
  public EntityCondition condition(final String entityId, final String propertyId,
                                   final Condition.Type conditionType, final Object value) {
    return new DefaultEntityCondition(entityId, propertyCondition(entityId, propertyId, conditionType, value));
  }

  /**
   * Creates a {@link Condition} for the entity type identified by {@code entityId}, based on the property
   * identified by {@code propertyId}, with the operator specified by the {@code conditionType} and {@code value}.
   * Note that {@code value} may be a single value, a Collection of values or null.
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public Condition<Property.ColumnProperty> propertyCondition(final String entityId, final String propertyId,
                                                              final Condition.Type conditionType, final Object value) {
    return propertyCondition(entityId, propertyId, conditionType, true, value);
  }

  /**
   * Creates a {@link Condition} for the entity type identified by {@code entityId}, based on the property
   * identified by {@code propertyId}, with the operator specified by the {@code conditionType} and {@code value}.
   * Note that {@code value} may be a single value, a Collection of values or null.
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @param conditionType the search type
   * @param caseSensitive true if the condition should be case sensitive, only applicable to string properties
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public Condition<Property.ColumnProperty> propertyCondition(final String entityId, final String propertyId,
                                                              final Condition.Type conditionType, final boolean caseSensitive,
                                                              final Object value) {
    requireNonNull(entityId, ENTITY_ID_PARAM);
    requireNonNull(propertyId, "propertyId");
    requireNonNull(conditionType, CONDITION_TYPE_PARAM);
    final Property property = domain.getProperty(entityId, propertyId);
    if (property instanceof Property.ForeignKeyProperty) {
      if (value instanceof Collection) {
        return foreignKeyCondition((Property.ForeignKeyProperty) property, conditionType, (Collection) value);
      }

      return foreignKeyCondition((Property.ForeignKeyProperty) property, conditionType, singletonList(value));
    }
    if (!(property instanceof Property.ColumnProperty)) {
      throw new IllegalArgumentException(property + " is not a " + Property.ColumnProperty.class.getSimpleName());
    }

    return propertyCondition((Property.ColumnProperty) property, conditionType, caseSensitive, value);
  }

  /**
   * Creates a {@link Condition} for the given property, with the operator specified by the {@code conditionType}
   * and {@code value}. Note that {@code value} may be a single value, a Collection of values or null.
   * @param property the property
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public Condition<Property.ColumnProperty> propertyCondition(final Property.ColumnProperty property,
                                                              final Condition.Type conditionType, final Object value) {
    return propertyCondition(property, conditionType, true, value);
  }

  /**
   * Creates a {@link Condition} for the given property, with the operator specified by the {@code conditionType}
   * and {@code value}. Note that {@code value} may be a single value, a Collection of values or null.
   * @param property the property
   * @param conditionType the search type
   * @param caseSensitive true if the condition should be case sensitive, only applicable to string properties
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public Condition<Property.ColumnProperty> propertyCondition(final Property.ColumnProperty property,
                                                              final Condition.Type conditionType, final boolean caseSensitive,
                                                              final Object value) {
    return new PropertyCondition(property, conditionType, value).setCaseSensitive(caseSensitive);
  }

  /**
   * Creates a {@link Condition} for the entity type identified by {@code entityId}, based on the foreign key property
   * identified by {@code foreignKeyPropertyId}, with the operator specified by the {@code conditionType} and {@code value}.
   * @param entityId the entity ID
   * @param foreignKeyPropertyId the property ID
   * @param conditionType the search type
   * @param value the condition value, may be null
   * @return a foreign key property condition based on the given value
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final String entityId, final String foreignKeyPropertyId,
                                                                final Condition.Type conditionType, final Entity value) {
    return foreignKeyCondition(entityId, foreignKeyPropertyId, conditionType, singletonList(value));
  }

  /**
   * Creates a {@link Condition} for the entity type identified by {@code entityId}, based on the foreign key property
   * identified by {@code foreignKeyPropertyId}, with the operator specified by the {@code conditionType} and {@code value}.
   * @param entityId the entity ID
   * @param foreignKeyPropertyId the property ID
   * @param conditionType the search type
   * @param value the condition value, may be null
   * @return a foreign key property condition based on the given value
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final String entityId, final String foreignKeyPropertyId,
                                                                final Condition.Type conditionType, final Entity.Key value) {
    return foreignKeyCondition(entityId, foreignKeyPropertyId, conditionType, singletonList(value));
  }

  /**
   * Creates a {@link Condition} for the entity type identified by {@code entityId}, based on the foreign key property
   * identified by {@code foreignKeyPropertyId}, with the operator specified by the {@code conditionType} and {@code values}.
   * {@code values} may contain either instances of {@link Entity} or {@link Entity.Key}
   * @param entityId the entity ID
   * @param foreignKeyPropertyId the property ID
   * @param conditionType the search type
   * @param values the condition values
   * @return a foreign key property condition based on the given values
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final String entityId, final String foreignKeyPropertyId,
                                                                final Condition.Type conditionType, final Collection values) {
    return foreignKeyCondition(domain.getForeignKeyProperty(entityId, foreignKeyPropertyId), conditionType, values);
  }

  /**
   * Creates a {@link Condition} for the given foreign key property, with the operator specified by the {@code conditionType}
   * and {@code value}.
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param value the condition value, may be null
   * @return a foreign key condition based on the given value
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                final Condition.Type conditionType, final Entity value) {
    return foreignKeyCondition(foreignKeyProperty, conditionType, singletonList(value));
  }

  /**
   * Creates a {@link Condition} for the given foreign key property, with the operator specified by the {@code conditionType}
   * and {@code value}.
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param value the condition value
   * @return a foreign key condition based on the given value
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                final Condition.Type conditionType, final Entity.Key value) {
    return foreignKeyCondition(foreignKeyProperty, conditionType, singletonList(value));
  }

  /**
   * Creates a {@link Condition} for the given foreign key property, with the operator specified by
   * the {@code conditionType} and {@code values}.
   * {@code values} may contain either instances of {@link Entity} or {@link Entity.Key}
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param values the condition values
   * @return a foreign key condition based on the given values
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                final Condition.Type conditionType, final Collection values) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    requireNonNull(conditionType, CONDITION_TYPE_PARAM);
    final List<Entity.Key> keys = getKeys(values);
    if (foreignKeyProperty.isCompositeKey()) {
      return createCompositeKeyCondition(foreignKeyProperty.getProperties(),
              domain.getPrimaryKeyProperties(foreignKeyProperty.getForeignEntityId()), conditionType, keys);
    }

    if (keys.size() == 1) {
      final Entity.Key entityKey = keys.get(0);
      return propertyCondition(foreignKeyProperty.getProperties().get(0), conditionType,
              entityKey == null ? null : entityKey.getFirstValue());
    }

    return propertyCondition(foreignKeyProperty.getProperties().get(0), conditionType, getValues(keys));
  }

  /**
   * Creates a new {@link Condition} based on the given condition string
   * @param conditionString the condition string without the WHERE keyword
   * @return a new Condition instance
   * @throws NullPointerException in case the condition string is null
   */
  public static Condition<Property.ColumnProperty> stringCondition(final String conditionString) {
    return stringCondition(conditionString, emptyList(), emptyList());
  }

  /**
   * Creates a new {@link Condition} based on the given condition string
   * @param conditionString the condition string without the WHERE keyword
   * @param values the values used by this condition string
   * @param properties the properties representing the values used by this condition, in the same order as their respective values
   * @return a new Condition instance
   * @throws NullPointerException in case any of the parameters are null
   */
  public static Condition<Property.ColumnProperty> stringCondition(final String conditionString, final List values,
                                                                   final List<Property.ColumnProperty> properties) {
    return new StringCondition(conditionString, values, properties);
  }

  /** Assumes {@code keys} is not empty. */
  private Condition<Property.ColumnProperty> createKeyCondition(final List<Entity.Key> keys) {
    final Entity.Key firstKey = keys.get(0);
    if (firstKey.isCompositeKey()) {
      return createCompositeKeyCondition(firstKey.getProperties(), firstKey.getProperties(), LIKE, keys);
    }

    return propertyCondition(firstKey.getFirstProperty(), LIKE, getValues(keys));
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition<Property.ColumnProperty> createCompositeKeyCondition(final List<Property.ColumnProperty> properties,
                                                                                final List<Property.ColumnProperty> foreignProperties,
                                                                                final Condition.Type conditionType,
                                                                                final List<Entity.Key> keys) {
    if (keys.size() == 1) {
      return createSingleCompositeCondition(properties, foreignProperties, conditionType, keys.get(0));
    }

    return createMultipleCompositeCondition(properties, foreignProperties, conditionType, keys);
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition<Property.ColumnProperty> createMultipleCompositeCondition(final List<Property.ColumnProperty> properties,
                                                                                     final List<Property.ColumnProperty> foreignProperties,
                                                                                     final Condition.Type conditionType,
                                                                                     final List<Entity.Key> keys) {
    final Condition.Set<Property.ColumnProperty> conditionSet = conditionSet(OR);
    for (int i = 0; i < keys.size(); i++) {
      conditionSet.add(createSingleCompositeCondition(properties, foreignProperties, conditionType, keys.get(i)));
    }

    return conditionSet;
  }

  private static Condition<Property.ColumnProperty> createSingleCompositeCondition(final List<Property.ColumnProperty> properties,
                                                                                   final List<Property.ColumnProperty> foreignProperties,
                                                                                   final Condition.Type conditionType,
                                                                                   final Entity.Key entityKey) {
    final Condition.Set<Property.ColumnProperty> conditionSet = conditionSet(AND);
    for (int i = 0; i < properties.size(); i++) {
      conditionSet.add(new PropertyCondition(properties.get(i), conditionType,
              entityKey == null ? null : entityKey.get(foreignProperties.get(i))));
    }

    return conditionSet;
  }

  private static List<Entity.Key> getKeys(final Object value) {
    final List<Entity.Key> keys = new ArrayList<>();
    if (value instanceof Collection) {
      if (((Collection) value).isEmpty()) {
        keys.add(null);
      }
      else {
        for (final Object object : (Collection) value) {
          keys.add(getKey(object));
        }
      }
    }
    else {
      keys.add(getKey(value));
    }

    return keys;
  }

  private static Entity.Key getKey(final Object value) {
    if (value == null || value instanceof Entity.Key) {
      return (Entity.Key) value;
    }
    else if (value instanceof Entity) {
      return ((Entity) value).getKey();
    }

    throw new IllegalArgumentException("Foreign key condition uses only Entity or Entity.Key instances for values");
  }

  private static void checkKeysParameter(final Collection<Entity.Key> keys) {
    if (nullOrEmpty(keys)) {
      throw new IllegalArgumentException("Entity key condition requires at least one key");
    }
  }

  private static final class DefaultEntityCondition implements EntityCondition {

    private static final long serialVersionUID = 1;

    private String entityId;
    private Condition<Property.ColumnProperty> condition;
    private String cachedWhereClause;

    /**
     * Instantiates a new empty {@link DefaultEntityCondition}.
     * Using an empty condition means all underlying records should be selected
     * @param entityId the ID of the entity to select
     */
    private DefaultEntityCondition(final String entityId) {
      this(entityId, null);
    }

    /**
     * Instantiates a new {@link EntityCondition}
     * @param entityId the ID of the entity to select
     * @param condition the Condition object
     * @see Set
     * @see PropertyCondition
     * @see EntityKeyCondition
     */
    private DefaultEntityCondition(final String entityId, final Condition<Property.ColumnProperty> condition) {
      this.entityId = requireNonNull(entityId, ENTITY_ID_PARAM);
      this.condition = condition;
    }

    @Override
    public List getValues() {
      return condition == null ? emptyList() : condition.getValues();
    }

    @Override
    public List<Property.ColumnProperty> getColumns() {
      return condition == null ? emptyList() : condition.getColumns();
    }

    @Override
    public String getEntityId() {
      return entityId;
    }

    @Override
    public Condition<Property.ColumnProperty> getCondition() {
      return condition;
    }

    @Override
    public String getWhereClause() {
      if (cachedWhereClause == null) {
        cachedWhereClause = condition == null ? "" : condition.getWhereClause();
      }

      return cachedWhereClause;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(entityId);
      stream.writeObject(condition);
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      entityId = (String) stream.readObject();
      condition = (Condition<Property.ColumnProperty>) stream.readObject();
    }
  }

  private static final class DefaultEntitySelectCondition implements EntitySelectCondition {

    private static final long serialVersionUID = 1;

    private Domain domain;

    private EntityCondition condition;
    private HashMap<String, Integer> foreignKeyFetchDepthLimits;
    private List<String> selectPropertyIds;

    private Entity.OrderBy orderBy;
    private int fetchCount = -1;
    private boolean forUpdate;
    private int limit;
    private int offset;

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}, which includes all the underlying entities
     * @param domain the domain model
     * @param entityId the ID of the entity to select
     */
    private DefaultEntitySelectCondition(final Domain domain, final String entityId) {
      this(domain, entityId, null);
    }

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}
     * @param domain the domain model
     * @param entityId the ID of the entity to select
     * @param condition the Condition object
     * @see PropertyCondition
     * @see EntityKeyCondition
     */
    private DefaultEntitySelectCondition(final Domain domain, final String entityId, final Condition<Property.ColumnProperty> condition) {
      this.domain = requireNonNull(domain);
      this.condition = new DefaultEntityCondition(entityId, condition);
      this.fetchCount = fetchCount;
    }

    @Override
    public Condition<Property.ColumnProperty> getCondition() {
      return condition.getCondition();
    }

    @Override
    public String getEntityId() {
      return condition.getEntityId();
    }

    @Override
    public List<Property.ColumnProperty> getColumns() {
      return condition.getColumns();
    }

    @Override
    public List getValues() {
      return condition.getValues();
    }

    @Override
    public String getWhereClause() {
      return condition.getWhereClause();
    }

    @Override
    public int getFetchCount() {
      return fetchCount;
    }

    @Override
    public EntitySelectCondition setFetchCount(final int fetchCount) {
      this.fetchCount = fetchCount;
      return this;
    }

    @Override
    public EntitySelectCondition setOrderBy(final Entity.OrderBy orderBy) {
      this.orderBy = orderBy;
      return this;
    }

    @Override
    public Entity.OrderBy getOrderBy() {
      return orderBy;
    }

    @Override
    public int getLimit() {
      return limit;
    }

    @Override
    public EntitySelectCondition setLimit(final int limit) {
      this.limit = limit;
      return this;
    }

    @Override
    public int getOffset() {
      return offset;
    }

    @Override
    public EntitySelectCondition setOffset(final int offset) {
      this.offset = offset;
      return this;
    }

    @Override
    public EntitySelectCondition setForeignKeyFetchDepthLimit(final String foreignKeyPropertyId, final int fetchDepthLimit) {
      if (foreignKeyFetchDepthLimits == null) {
        foreignKeyFetchDepthLimits = new HashMap<>();
      }
      this.foreignKeyFetchDepthLimits.put(foreignKeyPropertyId, fetchDepthLimit);
      return this;
    }

    @Override
    public int getForeignKeyFetchDepthLimit(final String foreignKeyPropertyId) {
      if (foreignKeyFetchDepthLimits != null && foreignKeyFetchDepthLimits.containsKey(foreignKeyPropertyId)) {
        return foreignKeyFetchDepthLimits.get(foreignKeyPropertyId);
      }

      return domain.getForeignKeyProperty(getEntityId(), foreignKeyPropertyId).getFetchDepth();
    }

    @Override
    public EntitySelectCondition setForeignKeyFetchDepthLimit(final int fetchDepthLimit) {
      final List<Property.ForeignKeyProperty> properties = domain.getForeignKeyProperties(getEntityId());
      for (int i = 0; i < properties.size(); i++) {
        setForeignKeyFetchDepthLimit(properties.get(i).getPropertyId(), fetchDepthLimit);
      }

      return this;
    }

    @Override
    public EntitySelectCondition setSelectPropertyIds(final String... propertyIds) {
      this.selectPropertyIds = asList(propertyIds);
      return this;
    }

    @Override
    public List<String> getSelectPropertyIds() {
      return selectPropertyIds == null ? emptyList() : selectPropertyIds;
    }

    @Override
    public boolean isForUpdate() {
      return forUpdate;
    }

    @Override
    public EntitySelectCondition setForUpdate(final boolean forUpdate) {
      this.forUpdate = forUpdate;
      return this;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(domain.getDomainId());
      stream.writeObject(orderBy);
      stream.writeInt(fetchCount);
      stream.writeBoolean(forUpdate);
      stream.writeObject(foreignKeyFetchDepthLimits);
      stream.writeObject(selectPropertyIds);
      stream.writeObject(condition);
      stream.writeInt(limit);
      stream.writeInt(offset);
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      final String domainId = (String) stream.readObject();
      orderBy = (Entity.OrderBy) stream.readObject();
      fetchCount = stream.readInt();
      forUpdate = stream.readBoolean();
      foreignKeyFetchDepthLimits = (HashMap<String, Integer>) stream.readObject();
      selectPropertyIds = (List<String>) stream.readObject();
      condition = (EntityCondition) stream.readObject();
      limit = stream.readInt();
      offset = stream.readInt();
      domain = Domain.getDomain(domainId);
    }
  }

  private static final class StringCondition implements Condition<Property.ColumnProperty> {

    private static final long serialVersionUID = 1;

    private String conditionString;
    private List values;
    private List<Property.ColumnProperty> properties;

    private StringCondition(final String conditionString, final List values, final List<Property.ColumnProperty> properties) {
      this.conditionString = requireNonNull(conditionString, "conditionString");
      this.values = requireNonNull(values, "values");
      this.properties = requireNonNull(properties, "properties");
    }

    @Override
    public String getWhereClause() {
      return conditionString;
    }

    @Override
    public List getValues() {
      return values;
    }

    @Override
    public List<Property.ColumnProperty> getColumns() {
      return properties;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(conditionString);
      stream.writeInt(values.size());
      for (int i = 0; i < values.size(); i++) {
        stream.writeObject(values.get(i));
      }
      stream.writeInt(properties.size());
      for (int i = 0; i < properties.size(); i++) {
        final Property.ColumnProperty property = properties.get(i);
        stream.writeObject(property.getDomainId());
        stream.writeObject(property.getEntityId());
        stream.writeObject(property.getPropertyId());
      }
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      conditionString = (String) stream.readObject();
      final int valueCount = stream.readInt();
      values = new ArrayList<>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add(stream.readObject());
      }
      final int columnCount = stream.readInt();
      properties = new ArrayList<>(columnCount);
      for (int i = 0; i < columnCount; i++) {
        final String domainId = (String) stream.readObject();
        final String entityId = (String) stream.readObject();
        final String propertyId = (String) stream.readObject();
        properties.add(Domain.getDomain(domainId).getColumnProperty(entityId, propertyId));
      }
    }
  }

  /**
   * A object for encapsulating a query condition with a single property and one or more values.
   */
  private static final class PropertyCondition implements Condition<Property.ColumnProperty> {

    private static final long serialVersionUID = 1;

    /**
     * The property used in this condition
     */
    private Property.ColumnProperty property;

    /**
     * The values used in this condition
     */
    private List values;

    /**
     * True if this condition tests for null
     */
    private boolean isNullCondition;

    /**
     * The search type used in this condition
     */
    private Type conditionType;

    /**
     * True if this condition should be case sensitive, only applies to condition based on string properties
     */
    private boolean caseSensitive = true;

    /**
     * Instantiates a new PropertyCondition instance
     * @param property the property
     * @param conditionType the search type
     * @param value the value, can be a Collection
     */
    private PropertyCondition(final Property.ColumnProperty property, final Type conditionType, final Object value) {
      requireNonNull(property, "property");
      requireNonNull(conditionType, CONDITION_TYPE_PARAM);
      this.property = property;
      this.conditionType = conditionType;
      this.isNullCondition = value == null;
      this.values = initializeValues(value, property);
    }

    @Override
    public List getValues() {
      if (isNullCondition) {
        return emptyList();
      }//null condition, uses 'x is null', not 'x = ?'

      return values;
    }

    @Override
    public List<Property.ColumnProperty> getColumns() {
      if (isNullCondition) {
        return emptyList();
      }//null condition, uses 'x is null', not 'x = ?'

      return Collections.nCopies(values.size(), property);
    }

    @Override
    public String getWhereClause() {
      return getConditionString();
    }

    /**
     * @return the number of values contained in this condition.
     */
    private int getValueCount() {
      if (isNullCondition) {
        return 0;
      }

      return values.size();
    }

    /**
     * Sets whether this condition should be case sensitive, only applies to condition based on string properties
     * @param caseSensitive if true then this condition is case sensitive, false otherwise
     * @return this PropertyCondition instance
     */
    private PropertyCondition setCaseSensitive(final boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
      return this;
    }

    private String getConditionString() {
      final String columnIdentifier = initializeColumnIdentifier(property.isString());
      if (isNullCondition) {
        return columnIdentifier + (conditionType == LIKE ? " is null" : " is not null");
      }

      final String valuePlaceholder = getValuePlaceholder();
      final String value2Placeholder = getValueCount() == 2 ? getValuePlaceholder() : null;

      switch (conditionType) {
        case LIKE:
          return getLikeCondition(columnIdentifier, valuePlaceholder, false);
        case NOT_LIKE:
          return getLikeCondition(columnIdentifier, valuePlaceholder, true);
        case LESS_THAN:
          return columnIdentifier + " <= " + valuePlaceholder;
        case GREATER_THAN:
          return columnIdentifier + " >= " + valuePlaceholder;
        case WITHIN_RANGE:
          return "(" + columnIdentifier + " >= " + valuePlaceholder + " and " + columnIdentifier + " <= " + value2Placeholder + ")";
        case OUTSIDE_RANGE:
          return "(" + columnIdentifier + " <= " + valuePlaceholder + " or " + columnIdentifier + " >= " + value2Placeholder + ")";
        default:
          throw new IllegalArgumentException("Unknown search type" + conditionType);
      }
    }

    private String getValuePlaceholder() {
      return property.isString() && !caseSensitive ? "upper(?)" : "?";
    }

    private String getLikeCondition(final String columnIdentifier, final String valuePlaceholder, final boolean notLike) {
      if (getValueCount() > 1) {
        return getInList(columnIdentifier, valuePlaceholder, getValueCount(), notLike);
      }

      if (property.isString() && containsWildcards((String) values.get(0))) {
        return columnIdentifier + (notLike ? " not like " : " like ") + valuePlaceholder;
      }
      else {
        return columnIdentifier + (notLike ? " <> " : " = ") + valuePlaceholder;
      }
    }

    private static List initializeValues(final Object value, final Property.ColumnProperty property) {
      final List values;
      if (value instanceof List) {
        values = (List) value;
      }
      else if (value instanceof Collection) {
        values = new ArrayList((Collection) value);
      }
      else {
        values = singletonList(value);
      }
      if (values.isEmpty()) {
        throw new IllegalArgumentException("No values specified for PropertyCondition: " + property);
      }
      for (int i = 0; i < values.size(); i++) {
        property.validateType(values.get(i));
      }

      return values;
    }

    private static String getInList(final String columnIdentifier, final String valuePlaceholder, final int valueCount, final boolean not) {
      final StringBuilder stringBuilder = new StringBuilder("(").append(columnIdentifier).append(not ? NOT_IN_PREFIX : IN_PREFIX);
      int cnt = 1;
      for (int i = 0; i < valueCount; i++) {
        stringBuilder.append(valuePlaceholder);
        if (cnt++ == IN_CLAUSE_LIMIT && i < valueCount - 1) {
          stringBuilder.append(not ? ") and " : ") or ").append(columnIdentifier).append(not ? NOT_IN_PREFIX : IN_PREFIX);
          cnt = 1;
        }
        else if (i < valueCount - 1) {
          stringBuilder.append(", ");
        }
      }
      stringBuilder.append("))");

      return stringBuilder.toString();
    }

    private String initializeColumnIdentifier(final boolean isStringProperty) {
      String columnName;
      if (property instanceof Property.SubqueryProperty) {
        columnName = "(" + ((Property.SubqueryProperty) property).getSubQuery() + ")";
      }
      else {
        columnName = property.getColumnName();
      }

      if (!isNullCondition && isStringProperty && !caseSensitive) {
        columnName = "upper(" + columnName + ")";
      }

      return columnName;
    }

    private static boolean containsWildcards(final String value) {
      return value.contains("%") || value.contains("_");
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(property.getDomainId());
      stream.writeObject(property.getEntityId());
      stream.writeObject(property.getPropertyId());
      stream.writeObject(conditionType);
      stream.writeBoolean(isNullCondition);
      stream.writeBoolean(caseSensitive);
      stream.writeInt(values.size());
      for (int i = 0; i < values.size(); i++) {
        stream.writeObject(values.get(i));
      }
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      final String domainId = (String) stream.readObject();
      final String entityId = (String) stream.readObject();
      final String propertyId = (String) stream.readObject();
      property = Domain.getDomain(domainId).getColumnProperty(entityId, propertyId);
      conditionType = (Type) stream.readObject();
      isNullCondition = stream.readBoolean();
      caseSensitive = stream.readBoolean();
      final int valueCount = stream.readInt();
      values = new ArrayList<>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add(stream.readObject());
      }
    }
  }
}
