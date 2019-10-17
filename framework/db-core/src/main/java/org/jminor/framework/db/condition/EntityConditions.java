/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * A class for creating query conditions.
 */
public final class EntityConditions {

  private static final String ENTITY_ID_PARAM = "entityId";

  private final Domain domain;

  /**
   * Instantiates a new EntityConditions instance based on the given domain model
   * @param domain the domain model
   */
  private EntityConditions(final Domain domain) {
    this.domain = requireNonNull(domain, "domain");
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
                                               final ConditionType conditionType, final Object value) {
    return selectCondition(entityId, propertyCondition(entityId, propertyId, conditionType, value));
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
                                   final ConditionType conditionType, final Object value) {
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
  public Condition propertyCondition(final String entityId, final String propertyId,
                                     final ConditionType conditionType, final Object value) {
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
  public Condition propertyCondition(final String entityId, final String propertyId,
                                     final ConditionType conditionType, final boolean caseSensitive,
                                     final Object value) {
    requireNonNull(entityId, ENTITY_ID_PARAM);
    requireNonNull(propertyId, "propertyId");
    requireNonNull(conditionType, Conditions.CONDITION_TYPE_PARAM);
    final Property property = domain.getProperty(entityId, propertyId);
    if (property instanceof Property.ForeignKeyProperty) {
      if (value instanceof Collection) {
        return Conditions.foreignKeyCondition((Property.ForeignKeyProperty) property, conditionType, (Collection) value);
      }

      return Conditions.foreignKeyCondition((Property.ForeignKeyProperty) property, conditionType, singletonList(value));
    }
    if (!(property instanceof Property.ColumnProperty)) {
      throw new IllegalArgumentException(property + " is not a " + Property.ColumnProperty.class.getSimpleName());
    }

    return Conditions.propertyCondition((Property.ColumnProperty) property, conditionType, caseSensitive, value);
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
  public Condition foreignKeyCondition(final String entityId, final String foreignKeyPropertyId,
                                       final ConditionType conditionType, final Entity value) {
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
  public Condition foreignKeyCondition(final String entityId, final String foreignKeyPropertyId,
                                       final ConditionType conditionType, final Entity.Key value) {
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
+   */
  public Condition foreignKeyCondition(final String entityId, final String foreignKeyPropertyId,
                                       final ConditionType conditionType, final Collection values) {
    return Conditions.foreignKeyCondition(domain.getForeignKeyProperty(entityId, foreignKeyPropertyId), conditionType, values);
  }

  /**
   * Returns a EntityConditions instance based on the given domain model
   * @param domain the domain model
   * @return a EntityConditions instance
   */
  public static EntityConditions using(final Domain domain) {
    return new EntityConditions(domain);
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting the entity with the given key
   * @param key the key
   * @return a select condition based on the given key
   */
  public static EntitySelectCondition selectCondition(final Entity.Key key) {
    return selectCondition(singletonList(requireNonNull(key, "key")));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting the entities with the given keys,
   * it is assumed they are all of the same type
   * @param keys the keys
   * @return a select condition based on the given keys
   */
  public static EntitySelectCondition selectCondition(final List<Entity.Key> keys) {
    checkKeysParameter(keys);
    return new DefaultEntitySelectCondition(keys.get(0).getEntityId(), Conditions.createKeyCondition(keys));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting all entities of the type identified by {@code entityId}
   * @param entityId the entity ID
   * @return a select condition encompassing all entities of the given type
   */
  public static EntitySelectCondition selectCondition(final String entityId) {
    return new DefaultEntitySelectCondition(entityId);
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities of the type identified by {@code entityId},
   * using the given {@link Condition}
   * @param entityId the entity ID
   * @param condition the column condition
   * @return a select condition based on the given column condition
   */
  public static EntitySelectCondition selectCondition(final String entityId, final Condition condition) {
    return new DefaultEntitySelectCondition(entityId, condition);
  }

  /**
   * Creates a {@link EntityCondition} instance specifying the entity of the type identified by {@code key}
   * @param key the primary key
   * @return a condition specifying the entity with the given primary key
   */
  public static EntityCondition condition(final Entity.Key key) {
    return condition(singletonList(key));
  }

  /**
   * Creates a {@link EntityCondition} instance specifying the entities of the type identified by {@code key},
   * using the given {@link Condition}
   * @param entityId the entity ID
   * @param condition the column condition
   * @return a condition based on the given column condition
   */
  public static EntityCondition condition(final String entityId, final Condition condition) {
    return new DefaultEntityCondition(entityId, condition);
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all of the same type
   * @param keys the primary keys
   * @return a condition specifying the entities having the given primary keys
   */
  public static EntityCondition condition(final List<Entity.Key> keys) {
    checkKeysParameter(keys);
    return new DefaultEntityCondition(keys.get(0).getEntityId(), Conditions.createKeyCondition(keys));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance specifying all entities of the type identified by {@code entityId}
   * @param entityId the entity ID
   * @return a condition specifying all entities of the given type
   */
  public static EntityCondition condition(final String entityId) {
    return new DefaultEntityCondition(entityId);
  }

  private static void checkKeysParameter(final Collection<Entity.Key> keys) {
    if (nullOrEmpty(keys)) {
      throw new IllegalArgumentException("Entity key condition requires at least one key");
    }
  }

  private static final class DefaultEntityCondition implements EntityCondition {

    private static final long serialVersionUID = 1;

    private String entityId;
    private Condition condition;
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
     */
    private DefaultEntityCondition(final String entityId, final Condition condition) {
      this.entityId = requireNonNull(entityId, ENTITY_ID_PARAM);
      this.condition = condition;
    }

    @Override
    public List getValues() {
      return condition == null ? emptyList() : condition.getValues();
    }

    @Override
    public List<Property.ColumnProperty> getProperties() {
      return condition == null ? emptyList() : condition.getProperties();
    }

    @Override
    public String getEntityId() {
      return entityId;
    }

    @Override
    public Condition getCondition() {
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
      condition = (Condition) stream.readObject();
    }
  }

  private static final class DefaultEntitySelectCondition implements EntitySelectCondition {

    private static final long serialVersionUID = 1;

    private EntityCondition condition;
    private HashMap<String, Integer> foreignKeyFetchDepthLimits;
    private List<String> selectPropertyIds;

    private Entity.OrderBy orderBy;
    private Integer foreignKeyFetchDepthLimit;
    private int fetchCount = -1;
    private boolean forUpdate;
    private int limit;
    private int offset;

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}, which includes all the underlying entities
     * @param domain the domain model
     * @param entityId the ID of the entity to select
     */
    private DefaultEntitySelectCondition(final String entityId) {
      this(entityId, null);
    }

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}
     * @param domain the domain model
     * @param entityId the ID of the entity to select
     * @param condition the Condition object
     * @see Conditions.DefaultCondition
     * @see EntityKeyCondition
     */
    private DefaultEntitySelectCondition(final String entityId, final Condition condition) {
      this.condition = new DefaultEntityCondition(entityId, condition);
      this.fetchCount = fetchCount;
    }

    @Override
    public Condition getCondition() {
      return condition.getCondition();
    }

    @Override
    public String getEntityId() {
      return condition.getEntityId();
    }

    @Override
    public List<Property.ColumnProperty> getProperties() {
      return condition.getProperties();
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
    public Integer getForeignKeyFetchDepthLimit(final String foreignKeyPropertyId) {
      if (foreignKeyFetchDepthLimits != null && foreignKeyFetchDepthLimits.containsKey(foreignKeyPropertyId)) {
        return foreignKeyFetchDepthLimits.get(foreignKeyPropertyId);
      }

      return foreignKeyFetchDepthLimit;
    }

    @Override
    public EntitySelectCondition setForeignKeyFetchDepthLimit(final int fetchDepthLimit) {
      this.foreignKeyFetchDepthLimit = fetchDepthLimit;
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
      stream.writeObject(orderBy);
      stream.writeInt(fetchCount);
      stream.writeBoolean(forUpdate);
      stream.writeObject(foreignKeyFetchDepthLimit);
      stream.writeObject(foreignKeyFetchDepthLimits);
      stream.writeObject(selectPropertyIds);
      stream.writeObject(condition);
      stream.writeInt(limit);
      stream.writeInt(offset);
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      orderBy = (Entity.OrderBy) stream.readObject();
      fetchCount = stream.readInt();
      forUpdate = stream.readBoolean();
      foreignKeyFetchDepthLimit = (Integer) stream.readObject();
      foreignKeyFetchDepthLimits = (HashMap<String, Integer>) stream.readObject();
      selectPropertyIds = (List<String>) stream.readObject();
      condition = (EntityCondition) stream.readObject();
      limit = stream.readInt();
      offset = stream.readInt();
    }
  }
}
