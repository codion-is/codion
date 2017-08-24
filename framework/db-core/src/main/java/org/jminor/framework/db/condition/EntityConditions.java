/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A factory class for query condition implementations.
 */
public final class EntityConditions {

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";

  private final Entities entities;

  /**
   * @param entities the domain entities
   */
  public EntityConditions(final Entities entities) {this.entities = entities;}

  /**
   * @param key the key
   * @return a select condition based on the given key
   */
  public EntitySelectCondition selectCondition(final Entity.Key key) {
    return selectCondition(Collections.singletonList(key));
  }

  /**
   * @param keys the keys
   * @return a select condition based on the given keys
   */
  public EntitySelectCondition selectCondition(final List<Entity.Key> keys) {
    checkKeysParameter(keys);
    return new DefaultEntitySelectCondition(entities, keys.get(0).getEntityID(), createKeyCondition(keys));
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a select condition based on the given value
   */
  public EntitySelectCondition selectCondition(final String entityID, final String propertyID,
                                               final Condition.Type conditionType, final Object value) {
    return selectCondition(entityID, propertyID, conditionType, -1, value);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param conditionType the search type
   * @param fetchCount the maximum number of entities to fetch
   * @param value the condition value, can be a Collection of values
   * @return a select condition based on the given value
   */
  public EntitySelectCondition selectCondition(final String entityID, final String propertyID,
                                               final Condition.Type conditionType, final int fetchCount,
                                               final Object value) {
    return selectCondition(entityID, propertyID, conditionType, null, fetchCount, value);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param conditionType the search type
   * @param orderByClause the order by clause, without the 'order by' keywords
   * @param fetchCount the maximum number of entities to fetch
   * @param value the condition value, can be a Collection of values
   * @return a select condition based on the given value
   */
  public EntitySelectCondition selectCondition(final String entityID, final String propertyID,
                                               final Condition.Type conditionType, final String orderByClause,
                                               final int fetchCount, final Object value) {
    return new DefaultEntitySelectCondition(entities, entityID, propertyCondition(entityID, propertyID, conditionType, value),
            fetchCount).setOrderByClause(orderByClause);
  }

  /**
   * @param entityID the entity ID
   * @param orderByClause the order by clause, without the 'order by' keywords
   * @return a select condition including all entities of the given type
   */
  public EntitySelectCondition selectCondition(final String entityID, final String orderByClause) {
    return new DefaultEntitySelectCondition(entities, entityID, null).setOrderByClause(orderByClause);
  }

  /**
   * @param entityID the entity ID
   * @param propertyCondition the column condition
   * @param orderByClause the order by clause, without the 'order by' keywords
   * @return a select condition based on the given column condition
   */
  public EntitySelectCondition selectCondition(final String entityID, final Condition<Property.ColumnProperty> propertyCondition,
                                               final String orderByClause) {
    return selectCondition(entityID, propertyCondition, orderByClause, -1);
  }

  /**
   * @param entityID the entity ID
   * @param propertyCondition the column condition
   * @param orderByClause the order by clause, without the 'order by' keywords
   * @param fetchCount the maximum number of entities to fetch
   * @return a select condition based on the given column condition
   */
  public EntitySelectCondition selectCondition(final String entityID, final Condition<Property.ColumnProperty> propertyCondition,
                                               final String orderByClause, final int fetchCount) {
    return new DefaultEntitySelectCondition(entities, entityID, propertyCondition, fetchCount).setOrderByClause(orderByClause);
  }

  /**
   * @param entityID the entity ID
   * @return a select condition encompassing all entities of the given type
   */
  public EntitySelectCondition selectCondition(final String entityID) {
    return new DefaultEntitySelectCondition(entities, entityID);
  }

  /**
   * @param entityID the entity ID
   * @param fetchCount the maximum number of entities to fetch
   * @return a select condition encompassing all entities of the given type
   */
  public EntitySelectCondition selectCondition(final String entityID, final int fetchCount) {
    return new DefaultEntitySelectCondition(entities, entityID, null, fetchCount);
  }

  /**
   * @param entityID the entity ID
   * @param propertyCondition the column condition
   * @return a select condition based on the given column condition
   */
  public EntitySelectCondition selectCondition(final String entityID, final Condition<Property.ColumnProperty> propertyCondition) {
    return new DefaultEntitySelectCondition(entities, entityID, propertyCondition);
  }

  /**
   * @param key the primary key
   * @return a condition specifying the entity having the given primary key
   */
  public EntityCondition condition(final Entity.Key key) {
    return condition(Collections.singletonList(key));
  }

  /**
   * @param entityID the entity ID
   * @param condition the column condition
   * @return a condition based on the given column condition
   */
  public EntityCondition condition(final String entityID, final Condition<Property.ColumnProperty> condition) {
    return new DefaultEntityCondition(entityID, condition);
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all for the same entityID
   * @param keys the primary keys
   * @return a condition specifying the entities having the given primary keys
   */
  public EntityCondition condition(final List<Entity.Key> keys) {
    checkKeysParameter(keys);
    return new DefaultEntityCondition(keys.get(0).getEntityID(), createKeyCondition(keys));
  }

  /**
   * @param entityID the entity ID
   * @return a condition specifying all entities of the given type
   */
  public EntityCondition condition(final String entityID) {
    return new DefaultEntityCondition(entityID);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a condition based on the given value
   */
  public EntityCondition condition(final String entityID, final String propertyID,
                                   final Condition.Type conditionType, final Object value) {
    return new DefaultEntityCondition(entityID, propertyCondition(entityID, propertyID, conditionType, value));
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public Condition<Property.ColumnProperty> propertyCondition(final String entityID, final String propertyID,
                                                              final Condition.Type conditionType, final Object value) {
    return propertyCondition(entityID, propertyID, conditionType, true, value);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param conditionType the search type
   * @param caseSensitive true if the condition should be case sensitive, only applicable to string properties
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public Condition<Property.ColumnProperty> propertyCondition(final String entityID, final String propertyID,
                                                              final Condition.Type conditionType, final boolean caseSensitive,
                                                              final Object value) {
    final Property property = entities.getProperty(entityID, propertyID);
    if (property instanceof Property.ForeignKeyProperty) {
      if (value instanceof Collection) {
        return foreignKeyCondition((Property.ForeignKeyProperty) property, conditionType, (Collection) value);
      }

      return foreignKeyCondition((Property.ForeignKeyProperty) property, conditionType, Collections.singletonList(value));
    }
    if (!(property instanceof Property.ColumnProperty)) {
      throw new IllegalArgumentException(property + " is not a " + Property.ColumnProperty.class.getSimpleName());
    }

    return propertyCondition((Property.ColumnProperty) property, conditionType, caseSensitive, value);
  }

  /**
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
   * @param entityID the entity ID
   * @param fkPropertyID the property ID
   * @param conditionType the search type
   * @param value the condition value
   * @return a foreign key property condition based on the given value
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final String entityID, final String fkPropertyID,
                                                                final Condition.Type conditionType, final Entity value) {
    return foreignKeyCondition(entityID, fkPropertyID, conditionType, Collections.singletonList(value));
  }

  /**
   * @param entityID the entity ID
   * @param fkPropertyID the property ID
   * @param conditionType the search type
   * @param value the condition value
   * @return a foreign key property condition based on the given value
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final String entityID, final String fkPropertyID,
                                                                final Condition.Type conditionType, final Entity.Key value) {
    return foreignKeyCondition(entityID, fkPropertyID, conditionType, Collections.singletonList(value));
  }

  /**
   * @param entityID the entity ID
   * @param fkPropertyID the property ID
   * @param conditionType the search type
   * @param values the condition values
   * @return a foreign key property condition based on the given values
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final String entityID, final String fkPropertyID,
                                                                final Condition.Type conditionType, final Collection values) {
    return foreignKeyCondition(entities.getForeignKeyProperty(entityID, fkPropertyID), conditionType, values);
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param value the condition value
   * @return a property condition based on the given value
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                final Condition.Type conditionType, final Entity value) {
    return foreignKeyCondition(foreignKeyProperty, conditionType, Collections.singletonList(value));
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param value the condition value
   * @return a property condition based on the given value
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                final Condition.Type conditionType, final Entity.Key value) {
    return foreignKeyCondition(foreignKeyProperty, conditionType, Collections.singletonList(value));
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param values the condition values
   * @return a property condition based on the given values
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                final Condition.Type conditionType, final Collection values) {
    final List<Entity.Key> keys = getEntityKeys(values);
    if (foreignKeyProperty.isCompositeReference()) {
      return createCompositeKeyCondition(foreignKeyProperty.getReferenceProperties(),
              entities.getReferencedProperties(foreignKeyProperty), conditionType, keys);
    }

    if (keys.size() == 1) {
      final Entity.Key entityKey = keys.get(0);
      return propertyCondition(foreignKeyProperty.getReferenceProperties().get(0), conditionType,
              entityKey == null ? null : entityKey.getFirstValue());
    }

    return propertyCondition(foreignKeyProperty.getReferenceProperties().get(0), conditionType, Entities.getValues(keys));
  }

  /** Assumes {@code keys} is not empty. */
  private Condition<Property.ColumnProperty> createKeyCondition(final List<Entity.Key> keys) {
    final Entity.Key firstKey = keys.get(0);
    if (firstKey.isCompositeKey()) {
      return createCompositeKeyCondition(firstKey.getProperties(), firstKey.getProperties(), Condition.Type.LIKE, keys);
    }

    return propertyCondition(firstKey.getFirstProperty(), Condition.Type.LIKE, Entities.getValues(keys));
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition<Property.ColumnProperty> createCompositeKeyCondition(final List<Property.ColumnProperty> referenceProperties,
                                                                                final List<Property.ColumnProperty> foreignProperties,
                                                                                final Condition.Type conditionType,
                                                                                final List<Entity.Key> keys) {
    if (keys.size() == 1) {
      return createSingleCompositeCondition(referenceProperties, foreignProperties, conditionType, keys.get(0));
    }

    return createMultipleCompositeCondition(referenceProperties, foreignProperties, conditionType, keys);
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition<Property.ColumnProperty> createMultipleCompositeCondition(final List<Property.ColumnProperty> referenceProperties,
                                                                                     final List<Property.ColumnProperty> foreignProperties,
                                                                                     final Condition.Type conditionType,
                                                                                     final List<Entity.Key> keys) {
    final Condition.Set<Property.ColumnProperty> conditionSet = Conditions.conditionSet(Conjunction.OR);
    for (int i = 0; i < keys.size(); i++) {
      conditionSet.add(createSingleCompositeCondition(referenceProperties, foreignProperties, conditionType, keys.get(i)));
    }

    return conditionSet;
  }

  private static Condition<Property.ColumnProperty> createSingleCompositeCondition(final List<Property.ColumnProperty> referenceProperties,
                                                                                   final List<Property.ColumnProperty> foreignProperties,
                                                                                   final Condition.Type conditionType,
                                                                                   final Entity.Key entityKey) {
    final Condition.Set<Property.ColumnProperty> conditionSet = Conditions.conditionSet(Conjunction.AND);
    for (int i = 0; i < referenceProperties.size(); i++) {
      conditionSet.add(new PropertyCondition(referenceProperties.get(i), conditionType,
              entityKey == null ? null : entityKey.get(foreignProperties.get(i))));
    }

    return conditionSet;
  }

  private static List<Entity.Key> getEntityKeys(final Object value) {
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
    Objects.requireNonNull(keys, "keys");
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("Entity key condition requires at least one key");
    }
  }

  private static final class DefaultEntityCondition implements EntityCondition {

    private static final long serialVersionUID = 1;

    private String entityID;
    private Condition<Property.ColumnProperty> condition;
    private String cachedWhereClause;

    /**
     * Instantiates a new empty {@link DefaultEntityCondition}.
     * Using an empty condition means all underlying records should be selected
     * @param entityID the ID of the entity to select
     */
    private DefaultEntityCondition(final String entityID) {
      this(entityID, null);
    }

    /**
     * Instantiates a new {@link EntityCondition}
     * @param entityID the ID of the entity to select
     * @param condition the Condition object
     * @see Set
     * @see PropertyCondition
     * @see EntityKeyCondition
     */
    private DefaultEntityCondition(final String entityID, final Condition<Property.ColumnProperty> condition) {
      Objects.requireNonNull(entityID, "entityID");
      this.entityID = entityID;
      this.condition = condition;
    }

    @Override
    public List<?> getValues() {
      return condition == null ? Collections.emptyList() : condition.getValues();
    }

    @Override
    public List<Property.ColumnProperty> getColumns() {
      return condition == null ? Collections.emptyList() : condition.getColumns();
    }

    @Override
    public String getEntityID() {
      return entityID;
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
      stream.writeObject(entityID);
      stream.writeObject(condition);
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      entityID = (String) stream.readObject();
      condition = (Condition<Property.ColumnProperty>) stream.readObject();
    }
  }

  private static final class DefaultEntitySelectCondition implements EntitySelectCondition {

    private static final long serialVersionUID = 1;

    private Entities entities;

    private EntityCondition condition;
    private HashMap<String, Integer> foreignKeyFetchDepthLimits;

    private OrderBy orderBy;
    private String orderByClause;
    private int fetchCount;
    private boolean forUpdate;
    private int limit;
    private int offset;

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}, which includes all the underlying entities
     * @param entityID the ID of the entity to select
     */
    private DefaultEntitySelectCondition(final Entities entities, final String entityID) {
      this(entities, entityID, null);
    }

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}
     * @param entityID the ID of the entity to select
     * @param condition the Condition object
     * @see PropertyCondition
     * @see EntityKeyCondition
     */
    private DefaultEntitySelectCondition(final Entities entities, final String entityID, final Condition<Property.ColumnProperty> condition) {
      this(entities, entityID, condition, -1);
    }

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}
     * @param entityID the ID of the entity to select
     * @param condition the Condition object
     * @param fetchCount the maximum number of records to fetch from the result
     * @see PropertyCondition
     * @see EntityKeyCondition
     */
    private DefaultEntitySelectCondition(final Entities entities, final String entityID,
                                         final Condition<Property.ColumnProperty> condition, final int fetchCount) {
      this.entities = entities;
      this.condition = new DefaultEntityCondition(entityID, condition);
      this.fetchCount = fetchCount;
    }

    @Override
    public Condition<Property.ColumnProperty> getCondition() {
      return condition.getCondition();
    }

    @Override
    public String getEntityID() {
      return condition.getEntityID();
    }

    @Override
    public List<Property.ColumnProperty> getColumns() {
      return condition.getColumns();
    }

    @Override
    public List<?> getValues() {
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
    public String getOrderByClause() {
      return orderBy != null ? orderBy.getOrderByClause() : orderByClause;
    }

    @Override
    public EntitySelectCondition setOrderByClause(final String orderByClause) {
      this.orderByClause = orderByClause;
      return this;
    }

    @Override
    public EntitySelectCondition orderByAscending(final String propertyID) {
      getOrderBy().add(propertyID, OrderBy.SortOrder.ASCENDING);
      return this;
    }

    @Override
    public EntitySelectCondition orderByDescending(final String propertyID) {
      getOrderBy().add(propertyID, OrderBy.SortOrder.DESCENDING);
      return this;
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
    public EntitySelectCondition setForeignKeyFetchDepthLimit(final String foreignKeyPropertyID, final int fetchDepthLimit) {
      if (foreignKeyFetchDepthLimits == null) {
        foreignKeyFetchDepthLimits = new HashMap<>();
      }
      this.foreignKeyFetchDepthLimits.put(foreignKeyPropertyID, fetchDepthLimit);
      return this;
    }

    @Override
    public int getForeignKeyFetchDepthLimit(final String foreignKeyPropertyID) {
      if (foreignKeyFetchDepthLimits != null && foreignKeyFetchDepthLimits.containsKey(foreignKeyPropertyID)) {
        return foreignKeyFetchDepthLimits.get(foreignKeyPropertyID);
      }

      return entities.getForeignKeyProperty(getEntityID(), foreignKeyPropertyID).getFetchDepth();
    }

    @Override
    public EntitySelectCondition setForeignKeyFetchDepthLimit(final int fetchDepthLimit) {
      final List<Property.ForeignKeyProperty > properties = entities.getForeignKeyProperties(getEntityID());
      for (int i = 0; i < properties.size(); i++) {
        setForeignKeyFetchDepthLimit(properties.get(i).getPropertyID(), fetchDepthLimit);
      }

      return this;
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

    private OrderBy getOrderBy() {
      if (orderBy == null) {
        orderBy = new OrderBy(entities, getEntityID());
      }

      return orderBy;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(entities.getClass().getName());
      stream.writeObject(orderBy);
      stream.writeObject(orderByClause);
      stream.writeInt(fetchCount);
      stream.writeBoolean(forUpdate);
      stream.writeObject(foreignKeyFetchDepthLimits);
      stream.writeObject(condition);
      stream.writeInt(limit);
      stream.writeInt(offset);
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      final String domainID = (String) stream.readObject();
      orderBy = (OrderBy) stream.readObject();
      orderByClause = (String) stream.readObject();
      fetchCount = stream.readInt();
      forUpdate = stream.readBoolean();
      foreignKeyFetchDepthLimits = (HashMap<String, Integer>) stream.readObject();
      condition = (EntityCondition) stream.readObject();
      limit = stream.readInt();
      offset = stream.readInt();
      entities = Entities.getDomainEntities(domainID);
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
      Objects.requireNonNull(property, "property");
      Objects.requireNonNull(conditionType, "conditionType");
      this.values = initializeValues(value);
      if (values.isEmpty()) {
        throw new IllegalArgumentException("No values specified for PropertyCondition: " + property);
      }
      this.property = property;
      this.conditionType = conditionType;
      this.isNullCondition = value == null;
    }

    @Override
    public List getValues() {
      if (isNullCondition) {
        return Collections.emptyList();
      }//null condition, uses 'x is null', not 'x = ?'

      return values;
    }

    @Override
    public List<Property.ColumnProperty> getColumns() {
      if (isNullCondition) {
        return Collections.emptyList();
      }//null condition, uses 'x is null', not 'x = ?'

      return Collections.nCopies(values.size(), property);
    }

    @Override
    public String getWhereClause() {
      return getConditionString();
    }

    /**
     * @return the number values contained in this condition.
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
        return columnIdentifier + (conditionType == Type.LIKE ? " is null" : " is not null");
      }

      final String valuePlaceholder = getValuePlaceholder();
      final String value2Placeholder = getValueCount() == 2 ? getValuePlaceholder() : null;

      switch(conditionType) {
        case LIKE:
          return getLikeCondition(columnIdentifier, valuePlaceholder, false);
        case NOT_LIKE:
          return getLikeCondition(columnIdentifier, valuePlaceholder, true);
        case LESS_THAN:
          return columnIdentifier + " <= " + valuePlaceholder;
        case GREATER_THAN:
          return columnIdentifier + " >= " + valuePlaceholder;
        case WITHIN_RANGE:
          return "(" + columnIdentifier + " >= " + valuePlaceholder + " and " + columnIdentifier +  " <= " + value2Placeholder + ")";
        case OUTSIDE_RANGE:
          return "(" + columnIdentifier + " <= " + valuePlaceholder + " or " + columnIdentifier + " >= " + value2Placeholder + ")";
        default:
          throw new IllegalArgumentException("Unknown search type" + conditionType);
      }
    }

    private String getValuePlaceholder() {
      return property.isString() && !caseSensitive ? "upper(?)" : "?";
    }

    private String getLikeCondition(final String columnIdentifier, final String value, final boolean not) {
      if (getValueCount() > 1) {
        return getInList(columnIdentifier, value, getValueCount(), not);
      }

      if (property.isString()) {
        return columnIdentifier + (not ? " not like " : " like ") + value;
      }
      else {
        return columnIdentifier + (not ? " <> " : " = ") + value;
      }
    }

    private static List initializeValues(final Object value) {
      if (value instanceof List) {
        return (List) value;
      }
      else if (value instanceof Collection) {
        return new ArrayList((Collection) value);
      }

      return Collections.singletonList(value);
    }

    private static String getInList(final String columnIdentifier, final String value, final int valueCount, final boolean not) {
      final StringBuilder stringBuilder = new StringBuilder("(").append(columnIdentifier).append(not ? NOT_IN_PREFIX : IN_PREFIX);
      int cnt = 1;
      for (int i = 0; i < valueCount; i++) {
        stringBuilder.append(value);
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

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(property.getDomainID());
      stream.writeObject(property.getEntityID());
      stream.writeObject(property.getPropertyID());
      stream.writeObject(conditionType);
      stream.writeBoolean(isNullCondition);
      stream.writeBoolean(caseSensitive);
      stream.writeInt(values.size());
      for (int i = 0; i < values.size(); i++) {
        stream.writeObject(values.get(i));
      }
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      final String domainID = (String) stream.readObject();
      final String entityID = (String) stream.readObject();
      final String propertyID = (String) stream.readObject();
      property = Entities.getDomainEntities(domainID).getColumnProperty(entityID, propertyID);
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

  private static final class OrderBy implements Serializable {

    private static final long serialVersionUID = 1;

    private enum SortOrder {
      ASCENDING, DESCENDING
    }

    private Entities entities;
    private String domainID;
    private String entityID;
    private LinkedHashMap<String, SortOrder> propertySortOrder = new LinkedHashMap<>();

    private OrderBy(final Entities entities, final String entityID) {
      Objects.requireNonNull(entities, "entities");
      Objects.requireNonNull(entityID, "entityID");
      this.entities = entities;
      this.domainID = entities.getClass().getName();
      this.entityID = entityID;
    }

    private OrderBy add(final String propertyID, final SortOrder order) {
      Objects.requireNonNull(propertyID, "propertyID");
      if (propertySortOrder.containsKey(propertyID)) {
        throw new IllegalArgumentException("Order by already contains the given property: " + propertyID);
      }
      propertySortOrder.put(propertyID, order);
      return this;
    }

    private String getOrderByClause() {
      final StringBuilder builder = new StringBuilder();
      final Set<Map.Entry<String, SortOrder>> entries = propertySortOrder.entrySet();
      int counter = 0;
      for (final Map.Entry<String, SortOrder> entry : entries) {
        final Property.ColumnProperty property = entities.getColumnProperty(entityID, entry.getKey());
        builder.append(property.getColumnName());
        if (entry.getValue().equals(SortOrder.DESCENDING)) {
          builder.append(" desc");
        }
        if (counter++ < entries.size() - 1) {
          builder.append(", ");
        }
      }

      return builder.toString();
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(domainID);
      stream.writeObject(entityID);
      stream.writeObject(propertySortOrder);
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      domainID = (String) stream.readObject();
      entityID = (String) stream.readObject();
      propertySortOrder = (LinkedHashMap<String, SortOrder>) stream.readObject();
      entities = Entities.getDomainEntities(domainID);
    }
  }
}
