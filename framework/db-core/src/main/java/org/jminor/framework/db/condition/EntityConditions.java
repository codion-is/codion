/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.Util;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.framework.domain.Entities;
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
import java.util.Objects;

/**
 * A class for creating query conditions.
 */
public final class EntityConditions {

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";
  private static final String ENTITY_ID_PARAM = "entityId";
  private static final String CONDITION_TYPE_PARAM = "conditionType";

  private final Entities entities;

  /**
   * @param entities the domain entities
   */
  public EntityConditions(final Entities entities) {
    this.entities = Objects.requireNonNull(entities, "entities");
  }

  /**
   * @param key the key
   * @return a select condition based on the given key
   */
  public EntitySelectCondition selectCondition(final Entity.Key key) {
    return selectCondition(Collections.singletonList(Objects.requireNonNull(key, "key")));
  }

  /**
   * @param keys the keys
   * @return a select condition based on the given keys
   */
  public EntitySelectCondition selectCondition(final List<Entity.Key> keys) {
    checkKeysParameter(keys);
    return new DefaultEntitySelectCondition(entities, keys.get(0).getEntityId(), createKeyCondition(keys));
  }

  /**
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a select condition based on the given value
   */
  public EntitySelectCondition selectCondition(final String entityId, final String propertyId,
                                               final Condition.Type conditionType, final Object value) {
    return selectCondition(entityId, propertyId, conditionType, -1, value);
  }

  /**
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @param conditionType the search type
   * @param fetchCount the maximum number of entities to fetch
   * @param value the condition value, can be a Collection of values
   * @return a select condition based on the given value
   */
  public EntitySelectCondition selectCondition(final String entityId, final String propertyId,
                                               final Condition.Type conditionType, final int fetchCount, final Object value) {
    return new DefaultEntitySelectCondition(entities, entityId, propertyCondition(entityId, propertyId, conditionType, value),
            fetchCount);
  }

  /**
   * @param entityId the entity ID
   * @param propertyCondition the column condition
   * @param fetchCount the maximum number of entities to fetch
   * @return a select condition based on the given column condition
   */
  public EntitySelectCondition selectCondition(final String entityId, final Condition<Property.ColumnProperty> propertyCondition,
                                               final int fetchCount) {
    return new DefaultEntitySelectCondition(entities, entityId, propertyCondition, fetchCount);
  }

  /**
   * @param entityId the entity ID
   * @return a select condition encompassing all entities of the given type
   */
  public EntitySelectCondition selectCondition(final String entityId) {
    return new DefaultEntitySelectCondition(entities, entityId);
  }

  /**
   * @param entityId the entity ID
   * @param fetchCount the maximum number of entities to fetch
   * @return a select condition encompassing all entities of the given type
   */
  public EntitySelectCondition selectCondition(final String entityId, final int fetchCount) {
    return new DefaultEntitySelectCondition(entities, entityId, null, fetchCount);
  }

  /**
   * @param entityId the entity ID
   * @param propertyCondition the column condition
   * @return a select condition based on the given column condition
   */
  public EntitySelectCondition selectCondition(final String entityId, final Condition<Property.ColumnProperty> propertyCondition) {
    return new DefaultEntitySelectCondition(entities, entityId, propertyCondition);
  }

  /**
   * @param key the primary key
   * @return a condition specifying the entity having the given primary key
   */
  public EntityCondition condition(final Entity.Key key) {
    return condition(Collections.singletonList(key));
  }

  /**
   * @param entityId the entity ID
   * @param condition the column condition
   * @return a condition based on the given column condition
   */
  public EntityCondition condition(final String entityId, final Condition<Property.ColumnProperty> condition) {
    return new DefaultEntityCondition(entityId, condition);
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all for the same entityId
   * @param keys the primary keys
   * @return a condition specifying the entities having the given primary keys
   */
  public EntityCondition condition(final List<Entity.Key> keys) {
    checkKeysParameter(keys);
    return new DefaultEntityCondition(keys.get(0).getEntityId(), createKeyCondition(keys));
  }

  /**
   * @param entityId the entity ID
   * @return a condition specifying all entities of the given type
   */
  public EntityCondition condition(final String entityId) {
    return new DefaultEntityCondition(entityId);
  }

  /**
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
    Objects.requireNonNull(entityId, ENTITY_ID_PARAM);
    Objects.requireNonNull(propertyId, "propertyId");
    Objects.requireNonNull(conditionType, CONDITION_TYPE_PARAM);
    final Property property = entities.getProperty(entityId, propertyId);
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
   * @param entityId the entity ID
   * @param fkPropertyId the property ID
   * @param conditionType the search type
   * @param value the condition value
   * @return a foreign key property condition based on the given value
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final String entityId, final String fkPropertyId,
                                                                final Condition.Type conditionType, final Entity value) {
    return foreignKeyCondition(entityId, fkPropertyId, conditionType, Collections.singletonList(value));
  }

  /**
   * @param entityId the entity ID
   * @param fkPropertyId the property ID
   * @param conditionType the search type
   * @param value the condition value
   * @return a foreign key property condition based on the given value
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final String entityId, final String fkPropertyId,
                                                                final Condition.Type conditionType, final Entity.Key value) {
    return foreignKeyCondition(entityId, fkPropertyId, conditionType, Collections.singletonList(value));
  }

  /**
   * @param entityId the entity ID
   * @param fkPropertyId the property ID
   * @param conditionType the search type
   * @param values the condition values
   * @return a foreign key property condition based on the given values
   */
  public Condition<Property.ColumnProperty> foreignKeyCondition(final String entityId, final String fkPropertyId,
                                                                final Condition.Type conditionType, final Collection values) {
    return foreignKeyCondition(entities.getForeignKeyProperty(entityId, fkPropertyId), conditionType, values);
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
    Objects.requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    Objects.requireNonNull(conditionType, CONDITION_TYPE_PARAM);
    final List<Entity.Key> keys = getEntityKeys(values);
    if (foreignKeyProperty.isCompositeKey()) {
      return createCompositeKeyCondition(foreignKeyProperty.getProperties(),
              entities.getForeignProperties(foreignKeyProperty), conditionType, keys);
    }

    if (keys.size() == 1) {
      final Entity.Key entityKey = keys.get(0);
      return propertyCondition(foreignKeyProperty.getProperties().get(0), conditionType,
              entityKey == null ? null : entityKey.getFirstValue());
    }

    return propertyCondition(foreignKeyProperty.getProperties().get(0), conditionType, Entities.getValues(keys));
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
    final Condition.Set<Property.ColumnProperty> conditionSet = Conditions.conditionSet(Conjunction.OR);
    for (int i = 0; i < keys.size(); i++) {
      conditionSet.add(createSingleCompositeCondition(properties, foreignProperties, conditionType, keys.get(i)));
    }

    return conditionSet;
  }

  private static Condition<Property.ColumnProperty> createSingleCompositeCondition(final List<Property.ColumnProperty> properties,
                                                                                   final List<Property.ColumnProperty> foreignProperties,
                                                                                   final Condition.Type conditionType,
                                                                                   final Entity.Key entityKey) {
    final Condition.Set<Property.ColumnProperty> conditionSet = Conditions.conditionSet(Conjunction.AND);
    for (int i = 0; i < properties.size(); i++) {
      conditionSet.add(new PropertyCondition(properties.get(i), conditionType,
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
    if (Util.nullOrEmpty(keys)) {
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
      this.entityId = Objects.requireNonNull(entityId, ENTITY_ID_PARAM);
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

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      entityId = (String) stream.readObject();
      condition = (Condition<Property.ColumnProperty>) stream.readObject();
    }
  }

  private static final class DefaultEntitySelectCondition implements EntitySelectCondition {

    private static final long serialVersionUID = 1;

    private Entities entities;

    private EntityCondition condition;
    private HashMap<String, Integer> foreignKeyFetchDepthLimits;

    private Entity.OrderBy orderBy;
    private int fetchCount;
    private boolean forUpdate;
    private int limit;
    private int offset;

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}, which includes all the underlying entities
     * @param entityId the ID of the entity to select
     */
    private DefaultEntitySelectCondition(final Entities entities, final String entityId) {
      this(entities, entityId, null);
    }

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}
     * @param entityId the ID of the entity to select
     * @param condition the Condition object
     * @see PropertyCondition
     * @see EntityKeyCondition
     */
    private DefaultEntitySelectCondition(final Entities entities, final String entityId, final Condition<Property.ColumnProperty> condition) {
      this(entities, entityId, condition, -1);
    }

    /**
     * Instantiates a new {@link DefaultEntitySelectCondition}
     * @param entityId the ID of the entity to select
     * @param condition the Condition object
     * @param fetchCount the maximum number of records to fetch from the result
     * @see PropertyCondition
     * @see EntityKeyCondition
     */
    private DefaultEntitySelectCondition(final Entities entities, final String entityId,
                                         final Condition<Property.ColumnProperty> condition, final int fetchCount) {
      this.entities = Objects.requireNonNull(entities);
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
      return orderBy != null ? orderBy.getOrderByClause(condition.getEntityId()) : null;
    }

    @Override
    public EntitySelectCondition setOrderBy(final Entity.OrderBy orderBy) {
      this.orderBy = orderBy;
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

      return entities.getForeignKeyProperty(getEntityId(), foreignKeyPropertyId).getFetchDepth();
    }

    @Override
    public EntitySelectCondition setForeignKeyFetchDepthLimit(final int fetchDepthLimit) {
      final List<Property.ForeignKeyProperty > properties = entities.getForeignKeyProperties(getEntityId());
      for (int i = 0; i < properties.size(); i++) {
        setForeignKeyFetchDepthLimit(properties.get(i).getPropertyId(), fetchDepthLimit);
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

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(entities.getDomainId());
      stream.writeObject(orderBy);
      stream.writeInt(fetchCount);
      stream.writeBoolean(forUpdate);
      stream.writeObject(foreignKeyFetchDepthLimits);
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
      condition = (EntityCondition) stream.readObject();
      limit = stream.readInt();
      offset = stream.readInt();
      entities = Entities.getDomainEntities(domainId);
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
      Objects.requireNonNull(conditionType, CONDITION_TYPE_PARAM);
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

    private String getLikeCondition(final String columnIdentifier, final String value, final boolean notLike) {
      if (getValueCount() > 1) {
        return getInList(columnIdentifier, value, getValueCount(), notLike);
      }

      if (property.isString()) {
        return columnIdentifier + (notLike ? " not like " : " like ") + value;
      }
      else {
        return columnIdentifier + (notLike ? " <> " : " = ") + value;
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
      property = Entities.getDomainEntities(domainId).getColumnProperty(entityId, propertyId);
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
