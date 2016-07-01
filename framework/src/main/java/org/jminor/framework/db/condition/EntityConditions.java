/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.ConditionSet;
import org.jminor.common.db.condition.ConditionType;
import org.jminor.common.db.condition.Conditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
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

  private EntityConditions() {}

  /**
   * @param key the key
   * @return a select condition based on the given key
   */
  public static EntitySelectCondition selectCondition(final Entity.Key key) {
    return selectCondition(Collections.singletonList(key));
  }

  /**
   * @param keys the keys
   * @return a select condition based on the given keys
   */
  public static EntitySelectCondition selectCondition(final Collection<Entity.Key> keys) {
    final EntityKeyCondition keyCondition = new EntityKeyCondition(keys);
    return new DefaultEntitySelectCondition(keyCondition.getEntityID(), keyCondition);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a select condition based on the given value
   */
  public static EntitySelectCondition selectCondition(final String entityID, final String propertyID,
                                                      final ConditionType conditionType, final Object value) {
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
  public static EntitySelectCondition selectCondition(final String entityID, final String propertyID,
                                                      final ConditionType conditionType, final int fetchCount,
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
  public static EntitySelectCondition selectCondition(final String entityID, final String propertyID,
                                                      final ConditionType conditionType, final String orderByClause,
                                                      final int fetchCount, final Object value) {
    return new DefaultEntitySelectCondition(entityID, createPropertyCondition(entityID, propertyID, conditionType, value),
            fetchCount).setOrderByClause(orderByClause);
  }

  /**
   * @param entityID the entity ID
   * @param orderByClause the order by clause, without the 'order by' keywords
   * @return a select condition including all entities of the given type
   */
  public static EntitySelectCondition selectCondition(final String entityID, final String orderByClause) {
    return new DefaultEntitySelectCondition(entityID, null).setOrderByClause(orderByClause);
  }

  /**
   * @param entityID the entity ID
   * @param foreignKeyReferenceProperties the foreign key reference properties
   * @param primaryKeys the primary keys referenced by the given properties
   * @return a select condition based on the given values
   */
  public static EntitySelectCondition selectCondition(final String entityID,
                                                      final List<Property.ColumnProperty> foreignKeyReferenceProperties,
                                                      final List<Entity.Key> primaryKeys) {
    final EntityKeyCondition keyCondition = new EntityKeyCondition(foreignKeyReferenceProperties, primaryKeys);
    return new DefaultEntitySelectCondition(entityID, keyCondition);
  }

  /**
   * @param entityID the entity ID
   * @param propertyCondition the column condition
   * @param orderByClause the order by clause, without the 'order by' keywords
   * @return a select condition based on the given column condition
   */
  public static EntitySelectCondition selectCondition(final String entityID,
                                                      final Condition<Property.ColumnProperty> propertyCondition,
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
  public static EntitySelectCondition selectCondition(final String entityID,
                                                      final Condition<Property.ColumnProperty> propertyCondition,
                                                      final String orderByClause, final int fetchCount) {
    return new DefaultEntitySelectCondition(entityID, propertyCondition, fetchCount).setOrderByClause(orderByClause);
  }

  /**
   * @param entityID the entity ID
   * @return a select condition encompassing all entities of the given type
   */
  public static EntitySelectCondition selectCondition(final String entityID) {
    return new DefaultEntitySelectCondition(entityID);
  }

  /**
   * @param entityID the entity ID
   * @param fetchCount the maximum number of entities to fetch
   * @return a select condition encompassing all entities of the given type
   */
  public static EntitySelectCondition selectCondition(final String entityID, final int fetchCount) {
    return new DefaultEntitySelectCondition(entityID, null, fetchCount);
  }

  /**
   * @param entityID the entity ID
   * @param propertyCondition the column condition
   * @return a select condition based on the given column condition
   */
  public static EntitySelectCondition selectCondition(final String entityID,
                                                      final Condition<Property.ColumnProperty> propertyCondition) {
    return new DefaultEntitySelectCondition(entityID, propertyCondition);
  }

  /**
   * @param key the primary key
   * @return a condition specifying the entity having the given primary key
   */
  public static EntityCondition condition(final Entity.Key key) {
    return condition(Collections.singletonList(key));
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all for the same entityID
   * @param keys the primary keys
   * @return a condition specifying the entities having the given primary keys
   */
  public static EntityCondition condition(final Collection<Entity.Key> keys) {
    final EntityKeyCondition keyCondition = new EntityKeyCondition(keys);
    return new DefaultEntityCondition(keyCondition.getEntityID(), keyCondition);
  }

  /**
   * @param entityID the entity ID
   * @return a condition specifying all entities of the given type
   */
  public static EntityCondition condition(final String entityID) {
    return new DefaultEntityCondition(entityID);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a condition based on the given value
   */
  public static EntityCondition condition(final String entityID, final String propertyID,
                                          final ConditionType conditionType, final Object value) {
    return new DefaultEntityCondition(entityID, createPropertyCondition(entityID, propertyID, conditionType, value));
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public static Condition<Property.ColumnProperty> propertyCondition(final String entityID, final String propertyID,
                                                                     final ConditionType conditionType, final Object value) {
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
  public static Condition<Property.ColumnProperty> propertyCondition(final String entityID, final String propertyID,
                                                                     final ConditionType conditionType, final boolean caseSensitive,
                                                                     final Object value) {
    final Property property = Entities.getProperty(entityID, propertyID);
    if (!(property instanceof Property.ColumnProperty)) {
      throw new IllegalArgumentException(property + " is not a " + Property.ColumnProperty.class.getName());
    }

    return propertyCondition((Property.ColumnProperty) property, conditionType, caseSensitive, value);
  }

  /**
   * @param property the property
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public static Condition<Property.ColumnProperty> propertyCondition(final Property.ColumnProperty property,
                                                                     final ConditionType conditionType, final Object value) {
    return propertyCondition(property, conditionType, true, value);
  }

  /**
   * @param property the property
   * @param conditionType the search type
   * @param caseSensitive true if the condition should be case sensitive, only applicable to string properties
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public static Condition<Property.ColumnProperty> propertyCondition(final Property.ColumnProperty property,
                                                                     final ConditionType conditionType, final boolean caseSensitive,
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
  public static Condition<Property.ColumnProperty> foreignKeyCondition(final String entityID, final String fkPropertyID,
                                                                       final ConditionType conditionType, final Entity value) {
    return foreignKeyCondition(entityID, fkPropertyID, conditionType, Collections.singletonList(value));
  }

  /**
   * @param entityID the entity ID
   * @param fkPropertyID the property ID
   * @param conditionType the search type
   * @param value the condition value
   * @return a foreign key property condition based on the given value
   */
  public static Condition<Property.ColumnProperty> foreignKeyCondition(final String entityID, final String fkPropertyID,
                                                                       final ConditionType conditionType, final Entity.Key value) {
    return foreignKeyCondition(entityID, fkPropertyID, conditionType, Collections.singletonList(value));
  }

  /**
   * @param entityID the entity ID
   * @param fkPropertyID the property ID
   * @param conditionType the search type
   * @param values the condition values
   * @return a foreign key property condition based on the given values
   */
  public static Condition<Property.ColumnProperty> foreignKeyCondition(final String entityID, final String fkPropertyID,
                                                                       final ConditionType conditionType, final Collection values) {
    return foreignKeyCondition(Entities.getForeignKeyProperty(entityID, fkPropertyID), conditionType, values);
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param value the condition value
   * @return a property condition based on the given value
   */
  public static Condition<Property.ColumnProperty> foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                       final ConditionType conditionType, final Entity value) {
    return foreignKeyCondition(foreignKeyProperty, conditionType, Collections.singletonList(value));
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param value the condition value
   * @return a property condition based on the given value
   */
  public static Condition<Property.ColumnProperty> foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                       final ConditionType conditionType, final Entity.Key value) {
    return foreignKeyCondition(foreignKeyProperty, conditionType, Collections.singletonList(value));
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param values the condition values
   * @return a property condition based on the given values
   */
  public static Condition<Property.ColumnProperty> foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                       final ConditionType conditionType, final Collection values) {
    return new ForeignKeyCondition(foreignKeyProperty, conditionType, values);
  }

  /**
   * @param entityID the entity ID
   * @param condition the column condition
   * @return a condition based on the given column condition
   */
  public static EntityCondition condition(final String entityID, final Condition<Property.ColumnProperty> condition) {
    return new DefaultEntityCondition(entityID, condition);
  }

  private static Condition<Property.ColumnProperty> createPropertyCondition(final String entityID, final String propertyID,
                                                                            final ConditionType conditionType, final Object value) {
    final Property property = Entities.getProperty(entityID, propertyID);
    final Condition<Property.ColumnProperty> condition;
    if (property instanceof Property.ForeignKeyProperty) {
      condition = new ForeignKeyCondition((Property.ForeignKeyProperty) property, conditionType, value);
    }
    else {
      condition = new PropertyCondition((Property.ColumnProperty) property, conditionType, value);
    }

    return condition;
  }

  private static final class DefaultEntityCondition implements EntityCondition, Serializable {

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
     * @see ConditionSet
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
      return condition == null ? null : condition.getValues();
    }

    @Override
    public List<Property.ColumnProperty> getValueKeys() {
      return condition == null ? null : condition.getValueKeys();
    }

    @Override
    public String getEntityID() {
      return entityID;
    }

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

  private static final class DefaultEntitySelectCondition implements EntitySelectCondition, Serializable {

    private static final long serialVersionUID = 1;

    private EntityCondition condition;
    private Map<String, Integer> foreignKeyFetchDepthLimits;

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
    private DefaultEntitySelectCondition(final String entityID) {
      this(entityID, null);
    }

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}
     * @param entityID the ID of the entity to select
     * @param condition the Condition object
     * @see ConditionSet
     * @see PropertyCondition
     * @see EntityKeyCondition
     */
    private DefaultEntitySelectCondition(final String entityID, final Condition<Property.ColumnProperty> condition) {
      this(entityID, condition, -1);
    }

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}
     * @param entityID the ID of the entity to select
     * @param condition the Condition object
     * @param fetchCount the maximum number of records to fetch from the result
     * @see ConditionSet
     * @see PropertyCondition
     * @see EntityKeyCondition
     */
    private DefaultEntitySelectCondition(final String entityID, final Condition<Property.ColumnProperty> condition,
                                         final int fetchCount) {
      this.condition = new DefaultEntityCondition(entityID, condition);
      this.fetchCount = fetchCount;
    }

    public Condition<Property.ColumnProperty> getCondition() {
      return condition.getCondition();
    }

    @Override
    public String getEntityID() {
      return condition.getEntityID();
    }

    @Override
    public List<Property.ColumnProperty> getValueKeys() {
      return condition.getValueKeys();
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

      return Entities.getForeignKeyProperty(getEntityID(), foreignKeyPropertyID).getFetchDepth();
    }

    @Override
    public EntitySelectCondition setForeignKeyFetchDepthLimit(final int fetchDepthLimit) {
      final Collection<Property.ForeignKeyProperty > properties = Entities.getForeignKeyProperties(getEntityID());
      for (final Property.ForeignKeyProperty property : properties) {
        setForeignKeyFetchDepthLimit(property.getPropertyID(), fetchDepthLimit);
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
        orderBy = new OrderBy(getEntityID());
      }

      return orderBy;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(orderBy);
      stream.writeObject(orderByClause);
      stream.writeInt(fetchCount);
      stream.writeBoolean(forUpdate);
      stream.writeObject(foreignKeyFetchDepthLimits);
      stream.writeObject(condition);
      stream.writeInt(limit);
      stream.writeInt(offset);
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      orderBy = (OrderBy) stream.readObject();
      orderByClause = (String) stream.readObject();
      fetchCount = stream.readInt();
      forUpdate = stream.readBoolean();
      foreignKeyFetchDepthLimits = (HashMap<String, Integer>) stream.readObject();
      condition = (EntityCondition) stream.readObject();
      limit = stream.readInt();
      offset = stream.readInt();
    }
  }

  /**
   * A class encapsulating a query condition with Entity.Key objects as values.
   */
  private static final class EntityKeyCondition implements Condition<Property.ColumnProperty>, Serializable {

    private static final long serialVersionUID = 1;

    private String entityID;
    private ConditionSet<Property.ColumnProperty> conditionSet;

    /**
     * Instantiates a new {@link EntityKeyCondition} comprised of the given keys
     * @param keys the keys
     */
    private EntityKeyCondition(final Collection<Entity.Key> keys) {
      this(null, keys);
    }

    /**
     * Instantiates a new {@link EntityKeyCondition} comprised of the given keys which uses the given properties
     * as column names when constructing the condition string
     * @param properties the properties to use for column names when constructing the condition string
     * @param keys the keys
     */
    private EntityKeyCondition(final List<Property.ColumnProperty> properties, final Collection<Entity.Key> keys) {
      conditionSet = Conditions.conditionSet(Conjunction.OR);
      Objects.requireNonNull(keys, "keys");
      if (keys.isEmpty()) {
        throw new IllegalArgumentException("EntityKeyCondition requires at least one key");
      }
      final Entity.Key firstKey = keys.iterator().next();
      if (properties != null && properties.size() != firstKey.getPropertyCount()) {
        throw new IllegalArgumentException("Reference property count mismatch");
      }
      entityID = firstKey.getEntityID();
      setupCondition(properties, keys, firstKey);
    }

    @Override
    public String getWhereClause() {
      return conditionSet.getWhereClause();
    }

    @Override
    public List<Property.ColumnProperty> getValueKeys() {
      return conditionSet.getValueKeys();
    }

    @Override
    public List<Object> getValues() {
      return conditionSet.getValues();
    }

    /**
     * @return the entityID
     */
    private String getEntityID() {
      return entityID;
    }

    private void setupCondition(final List<Property.ColumnProperty> properties, final Collection<Entity.Key> keys,
                                final Entity.Key firstKey) {
      if (firstKey.isCompositeKey()) {//multiple column key
        final List<Property.ColumnProperty> pkProperties = firstKey.getProperties();
        final List<? extends Property.ColumnProperty> propertyList = properties == null ? pkProperties : properties;
        //(a = b and c = d) or (a = g and c = d)
        for (final Entity.Key key : keys) {
          final ConditionSet<Property.ColumnProperty> andSet = Conditions.conditionSet(Conjunction.AND);
          int i = 0;
          for (final Property.ColumnProperty property : propertyList) {
            andSet.add(new PropertyCondition(property, ConditionType.LIKE, key.get(pkProperties.get(i++).getPropertyID())));
          }

          conditionSet.add(andSet);
        }
      }
      else {
        final Property.ColumnProperty property = properties == null ? firstKey.getFirstProperty() : properties.get(0);
        final Property primaryKeyProperty = properties == null ? property : firstKey.getFirstProperty();
        //a = b
        if (keys.size() == 1) {
          conditionSet.add(new PropertyCondition(property, ConditionType.LIKE, firstKey.get(primaryKeyProperty.getPropertyID())));
        }
        else { //a in (c, v, d, s)
          conditionSet.add(new PropertyCondition(property, ConditionType.LIKE, EntityUtil.getValues(keys)));
        }
      }
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(entityID);
      stream.writeObject(conditionSet);
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      entityID = (String) stream.readObject();
      conditionSet = (ConditionSet<Property.ColumnProperty>) stream.readObject();
    }
  }

  /**
   * A object for encapsulating a query condition with a single property and one or more values.
   */
  private static final class PropertyCondition implements Condition<Property.ColumnProperty>, Serializable {

    private static final long serialVersionUID = 1;

    /**
     * The property used in this condition
     */
    private Property.ColumnProperty property;

    /**
     * The values used in this condition
     */
    private Collection values;

    /**
     * True if this condition tests for null
     */
    private boolean isNullCondition;

    /**
     * The search type used in this condition
     */
    private ConditionType conditionType;

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
    private PropertyCondition(final Property.ColumnProperty property, final ConditionType conditionType, final Object value) {
      Objects.requireNonNull(property, "property");
      Objects.requireNonNull(conditionType, "conditionType");
      if (value instanceof Collection) {
        this.values = (Collection) value;
      }
      else {
        this.values = Collections.singletonList(value);
      }
      if (values.isEmpty()) {
        throw new IllegalArgumentException("No values specified for PropertyCondition: " + property);
      }
      this.property = property;
      this.conditionType = conditionType;
      this.isNullCondition = this.values.size() == 1 && this.values.iterator().next() == null;
    }

    @Override
    public List getValues() {
      if (isNullCondition) {
        return Collections.emptyList();
      }//null condition, uses 'x is null', not 'x = ?'

      //noinspection unchecked
      return new ArrayList(values);
    }

    @Override
    public List<Property.ColumnProperty> getValueKeys() {
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
        return columnIdentifier + (conditionType == ConditionType.LIKE ? " is null" : " is not null");
      }

      final String sqlValue = getSqlValue("?");
      final String sqlValue2 = getValueCount() == 2 ? getSqlValue("?") : null;

      switch(conditionType) {
        case LIKE:
          return getLikeCondition(columnIdentifier, sqlValue, false);
        case NOT_LIKE:
          return getLikeCondition(columnIdentifier, sqlValue, true);
        case LESS_THAN:
          return columnIdentifier + " <= " + sqlValue;
        case GREATER_THAN:
          return columnIdentifier + " >= " + sqlValue;
        case WITHIN_RANGE:
          return "(" + columnIdentifier + " >= " + sqlValue + " and " + columnIdentifier +  " <= " + sqlValue2 + ")";
        case OUTSIDE_RANGE:
          return "(" + columnIdentifier + " <= " + sqlValue + " or " + columnIdentifier + " >= " + sqlValue2 + ")";
        default:
          throw new IllegalArgumentException("Unknown search type" + conditionType);
      }
    }

    private String getSqlValue(final String sqlStringValue) {
      return property.isString() && !caseSensitive ? "upper(" + sqlStringValue + ")" : sqlStringValue;
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

    private static String getInList(final String columnIdentifier, final String value, final int valueCount, final boolean not) {
      final StringBuilder stringBuilder = new StringBuilder("(").append(columnIdentifier).append(not ? " not in (" : IN_PREFIX);
      int cnt = 1;
      for (int i = 0; i < valueCount; i++) {
        stringBuilder.append(value);
        if (cnt++ == IN_CLAUSE_LIMIT && i < valueCount - 1) {
          stringBuilder.append(not ? ") and " : ") or ").append(columnIdentifier).append(IN_PREFIX);
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
      stream.writeObject(property.getEntityID());
      stream.writeObject(property.getPropertyID());
      stream.writeObject(conditionType);
      stream.writeBoolean(isNullCondition);
      stream.writeBoolean(caseSensitive);
      stream.writeInt(values.size());
      for (final Object value : values) {
        stream.writeObject(value);
      }
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      final String entityID = (String) stream.readObject();
      final String propertyID = (String) stream.readObject();
      property = (Property.ColumnProperty) Entities.getProperty(entityID, propertyID);
      conditionType = (ConditionType) stream.readObject();
      isNullCondition = stream.readBoolean();
      caseSensitive = stream.readBoolean();
      final int valueCount = stream.readInt();
      values = new ArrayList<>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add(stream.readObject());
      }
    }
  }

  private static final class ForeignKeyCondition implements Condition<Property.ColumnProperty>, Serializable {

    private static final long serialVersionUID = 1;

    /**
     * The property used in this condition
     */
    private Property.ForeignKeyProperty property;

    /**
     * The values used in this condition
     */
    private Collection<Entity.Key> values;
    private ConditionType conditionType;
    private boolean isNullCondition;

    private ForeignKeyCondition(final Property.ForeignKeyProperty property, final ConditionType conditionType, final Object value) {
      Objects.requireNonNull(property, "property");
      Objects.requireNonNull(conditionType, "conditionType");
      this.property = property;
      this.conditionType = conditionType;
      this.values = getEntityKeys(value);
      this.isNullCondition = this.values.size() == 1 && this.values.iterator().next() == null;
    }

    @Override
    public String getWhereClause() {
      return getForeignKeyConditionString();
    }

    @Override
    public List<Property.ColumnProperty> getValueKeys() {
      if (isNullCondition) {
        return Collections.emptyList();
      }//null condition, uses 'x is null', not 'x = ?'

      return getForeignKeyValueProperties();
    }

    @Override
    public List<?> getValues() {
      if (isNullCondition) {
        return Collections.emptyList();
      }//null condition, uses 'x is null', not 'x = ?'

      return getForeignKeyConditionValues();
    }

    private String getForeignKeyConditionString() {
      if (getValues().size() > 1) {
        return getMultipleForeignKeyConditionString();
      }

      return createSingleForeignKeyCondition(values.iterator().next()).getWhereClause();
    }

    private String getMultipleForeignKeyConditionString() {
      if (property.isCompositeReference()) {
        return createMultipleCompositeForeignKeyCondition().getWhereClause();
      }
      else {
        return PropertyCondition.getInList(property.getReferenceProperties().get(0).getColumnName(), "?",
                getValues().size(), conditionType == ConditionType.NOT_LIKE);
      }
    }

    private List<?> getForeignKeyConditionValues() {
      if (values.size() > 1) {
        return getCompositeForeignKeyConditionValues();
      }

      return createSingleForeignKeyCondition(values.iterator().next()).getValues();
    }

    private List<?> getCompositeForeignKeyConditionValues() {
      return createMultipleCompositeForeignKeyCondition().getValues();
    }

    private List<Property.ColumnProperty> getForeignKeyValueProperties() {
      if (values.size() > 1) {
        return createMultipleCompositeForeignKeyCondition().getValueKeys();
      }

      return createSingleForeignKeyCondition(values.iterator().next()).getValueKeys();
    }

    private Condition<Property.ColumnProperty> createMultipleCompositeForeignKeyCondition() {
      final ConditionSet<Property.ColumnProperty> conditionSet = Conditions.conditionSet(Conjunction.OR);
      for (final Object entityKey : values) {
        conditionSet.add(createSingleForeignKeyCondition((Entity.Key) entityKey));
      }

      return conditionSet;
    }

    private Condition<Property.ColumnProperty> createSingleForeignKeyCondition(final Entity.Key entityKey) {
      final Property.ForeignKeyProperty foreignKeyProperty = property;
      if (foreignKeyProperty.isCompositeReference()) {
        final ConditionSet<Property.ColumnProperty> pkSet = Conditions.conditionSet(Conjunction.AND);
        for (final Property.ColumnProperty referencedProperty : foreignKeyProperty.getReferenceProperties()) {
          final String referencedPropertyID = foreignKeyProperty.getReferencedPropertyID(referencedProperty);
          final Object referencedValue = entityKey == null ? null : entityKey.get(referencedPropertyID);
          pkSet.add(new PropertyCondition(referencedProperty, conditionType, referencedValue));
        }

        return pkSet;
      }
      else {
        return new PropertyCondition(foreignKeyProperty.getReferenceProperties().get(0), conditionType,
                entityKey == null ? null : entityKey.getFirstValue());
      }
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeBoolean(isNullCondition);
      stream.writeObject(property.getEntityID());
      stream.writeObject(property.getPropertyID());
      stream.writeObject(conditionType);
      stream.writeInt(values.size());
      for (final Entity.Key key : values) {
        stream.writeObject(key);
      }
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      isNullCondition = stream.readBoolean();
      final String entityID = (String) stream.readObject();
      final String propertyID = (String) stream.readObject();
      property = (Property.ForeignKeyProperty) Entities.getProperty(entityID, propertyID);
      conditionType = (ConditionType) stream.readObject();
      final int valueCount = stream.readInt();
      values = new ArrayList<>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add((Entity.Key) stream.readObject());
      }
    }

    @SuppressWarnings({"unchecked"})
    private static Collection<Entity.Key> getEntityKeys(final Object value) {
      final Collection<Entity.Key> keys = new ArrayList<>();
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
  }

  private static final class OrderBy implements Serializable {

    private static final long serialVersionUID = 1;

    private enum SortOrder {
      ASCENDING, DESCENDING
    }

    private String entityID;
    private LinkedHashMap<String, SortOrder> propertySortOrder = new LinkedHashMap<>();

    private OrderBy(final String entityID) {
      Objects.requireNonNull(entityID, "entityID");
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
        final Property.ColumnProperty property = Entities.getColumnProperty(entityID, entry.getKey());
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
      stream.writeObject(entityID);
      stream.writeObject(propertySortOrder);
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      entityID = (String) stream.readObject();
      propertySortOrder = (LinkedHashMap<String, SortOrder>) stream.readObject();
    }
  }
}
