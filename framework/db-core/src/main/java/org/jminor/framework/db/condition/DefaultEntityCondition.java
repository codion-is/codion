/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.Conjunction.AND;
import static org.jminor.common.Conjunction.OR;
import static org.jminor.common.Util.nullOrEmpty;
import static org.jminor.common.db.ConditionType.LIKE;
import static org.jminor.framework.db.condition.DefaultConditionSet.NULL_CONDITION;
import static org.jminor.framework.domain.Entities.getValues;

class DefaultEntityCondition implements EntityCondition {

  private static final long serialVersionUID = 1;

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";

  private String entityId;
  private Condition condition;
  private String cachedWhereClause;
  private boolean expandRequired = true;
  private transient boolean expanded;

  /**
   * Instantiates a new {@link DefaultEntityCondition}.
   * @param key the key of the Entity to select
   */
  DefaultEntityCondition(final Entity.Key key) {
    this(singletonList(requireNonNull(key, "key")));
  }

  /**
   * Instantiates a new {@link DefaultEntityCondition}.
   * @param keys the keys of the Entity to select
   */
  DefaultEntityCondition(final List<Entity.Key> keys) {
    this(checkKeysParameter(keys).get(0).getEntityId(), createKeyCondition(keys));
    this.expandRequired = false;
  }

  /**
   * Instantiates a new empty {@link DefaultEntityCondition}.
   * Using an empty condition means all underlying records should be selected
   * @param entityId the ID of the entity to select
   */
  DefaultEntityCondition(final String entityId) {
    this(entityId, null);
    this.expandRequired = false;
  }

  /**
   * Instantiates a new {@link EntityCondition}
   * @param entityId the ID of the entity to select
   * @param condition the Condition object
   */
  DefaultEntityCondition(final String entityId, final Condition condition) {
    this.entityId = requireNonNull(entityId, "entityId");
    this.condition = condition == null ? NULL_CONDITION : condition;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final Condition getCondition(final Domain domain) {
    expandForeignKeyConditions(requireNonNull(domain, "domain"));

    return condition;
  }

  /** {@inheritDoc} */
  @Override
  public final String getWhereClause(final Domain domain) {
    if (cachedWhereClause == null) {
      cachedWhereClause = getWhereClause(getCondition(domain), domain);
    }

    return cachedWhereClause;
  }

  private void expandForeignKeyConditions(final Domain domain) {
    if (expandRequired && !expanded) {
      condition = expandForeignKeyConditions(this.condition, domain);
      expanded = true;
    }
  }

  private Condition expandForeignKeyConditions(final Condition condition, final Domain domain) {
    if (condition instanceof Condition.Set) {
      final Condition.Set conditionSet = (Condition.Set) condition;
      final ListIterator<Condition> conditionsIterator = conditionSet.getConditions().listIterator();
      while (conditionsIterator.hasNext()) {
        conditionsIterator.set(expandForeignKeyConditions(conditionsIterator.next(), domain));
      }
    }
    else if (condition instanceof Condition.PropertyCondition) {
      final Condition.PropertyCondition propertyCondition = (Condition.PropertyCondition) condition;
      final Property property = domain.getProperty(entityId, propertyCondition.getPropertyId());
      if (property instanceof Property.ForeignKeyProperty) {
        return foreignKeyCondition((Property.ForeignKeyProperty) property, propertyCondition.getConditionType(), condition.getValues());
      }
    }

    return condition;
  }

  private String getWhereClause(final Condition condition, final Domain domain) {
    if (condition instanceof DefaultConditionSet.NullCondition) {
      return "";
    }
    if (condition instanceof Condition.Set) {
      return getWhereClause((Condition.Set) condition, domain);
    }
    else if (condition instanceof Condition.PropertyCondition) {
      return getConditionString((Condition.PropertyCondition) condition, domain);
    }
    else if (condition instanceof Condition.StringCondition) {
      return ((Condition.StringCondition) condition).getConditionString();
    }

    throw new IllegalArgumentException("Unsupported condition: " + condition.getClass());
  }

  private String getConditionString(final Condition.PropertyCondition condition, final Domain domain) {
    final Property.ColumnProperty property = domain.getColumnProperty(entityId, condition.getPropertyId());

    return createColumnPropertyConditionString(property, condition.getConditionType(), condition.getValues(),
            condition.isNullCondition(), condition.isCaseSensitive());
  }

  private static String createColumnPropertyConditionString(final Property.ColumnProperty property,
                                                            final ConditionType conditionType, final List values,
                                                            final boolean isNullCondition, final boolean isCaseSensitive) {
    for (int i = 0; i < values.size(); i++) {
      property.validateType(values.get(i));
    }
    final String columnIdentifier = initializeColumnIdentifier(property, isNullCondition, isCaseSensitive);
    if (isNullCondition) {
      return columnIdentifier + (conditionType == LIKE ? " is null" : " is not null");
    }

    final int valueCount = isNullCondition ? 0 : values.size();

    final String valuePlaceholder = getValuePlaceholder(property, isCaseSensitive);
    final String value2Placeholder = valueCount == 2 ? getValuePlaceholder(property, isCaseSensitive) : null;

    switch (conditionType) {
      case LIKE:
        return getLikeCondition(property, columnIdentifier, valuePlaceholder, false, values, valueCount);
      case NOT_LIKE:
        return getLikeCondition(property, columnIdentifier, valuePlaceholder, true, values, valueCount);
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

  private static String getValuePlaceholder(final Property.ColumnProperty property, final boolean caseSensitive) {
    return property.isString() && !caseSensitive ? "upper(?)" : "?";
  }

  private static String getLikeCondition(final Property.ColumnProperty property, final String columnIdentifier,
                                         final String valuePlaceholder, final boolean notLike, final List values,
                                         final int valueCount) {
    if (valueCount > 1) {
      return getInList(columnIdentifier, valuePlaceholder, valueCount, notLike);
    }
    if (property.isString() && containsWildcards((String) values.get(0))) {
      return columnIdentifier + (notLike ? " not like " : " like ") + valuePlaceholder;
    }
    else {
      return columnIdentifier + (notLike ? " <> " : " = ") + valuePlaceholder;
    }
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

  private static String initializeColumnIdentifier(final Property.ColumnProperty property, final boolean isNullCondition,
                                                   final boolean caseSensitive) {
    String columnName;
    if (property instanceof Property.SubqueryProperty) {
      columnName = "(" + ((Property.SubqueryProperty) property).getSubQuery() + ")";
    }
    else {
      columnName = property.getColumnName();
    }

    if (!isNullCondition && property.isString() && !caseSensitive) {
      columnName = "upper(" + columnName + ")";
    }

    return columnName;
  }

  private static boolean containsWildcards(final String value) {
    return value.contains("%") || value.contains("_");
  }

  private String getWhereClause(final Condition.Set conditionSet, final Domain domain) {
    final List<Condition> conditions = conditionSet.getConditions();
    if (conditions.isEmpty()) {
      return "";
    }

    final StringBuilder conditionString = new StringBuilder(conditions.size() > 1 ? "(" : "");
    for (int i = 0; i < conditions.size(); i++) {
      conditionString.append(getWhereClause(conditions.get(i), domain));
      if (i < conditions.size() - 1) {
        conditionString.append(toString(conditionSet.getConjunction()));
      }
    }

    return conditionString.append(conditions.size() > 1 ? ")" : "").toString();
  }

  private static String toString(final Conjunction conjunction) {
    switch (conjunction) {
      case AND: return " and ";
      case OR: return " or ";
      default: throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
    }
  }

  private static Condition foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                               final ConditionType conditionType, final Collection values) {
    final List<Entity.Key> keys = getKeys(values);
    if (foreignKeyProperty.isCompositeKey()) {
      return createCompositeKeyCondition(foreignKeyProperty.getProperties(), conditionType, keys);
    }

    if (keys.size() == 1) {
      final Entity.Key entityKey = keys.get(0);

      return Conditions.propertyCondition(foreignKeyProperty.getProperties().get(0).getPropertyId(), conditionType,
              entityKey == null ? null : entityKey.getFirstValue());
    }

    return Conditions.propertyCondition(foreignKeyProperty.getProperties().get(0).getPropertyId(), conditionType, getValues(keys));
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

  /** Assumes {@code keys} is not empty. */
  protected static Condition createKeyCondition(final List<Entity.Key> keys) {
    final Entity.Key firstKey = keys.get(0);
    if (firstKey.isCompositeKey()) {
      return createCompositeKeyCondition(firstKey.getProperties(), LIKE, keys);
    }

    return Conditions.propertyCondition(firstKey.getFirstProperty().getPropertyId(), LIKE, getValues(keys));
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition createCompositeKeyCondition(final List<Property.ColumnProperty> properties,
                                                       final ConditionType conditionType,
                                                       final List<Entity.Key> keys) {
    if (keys.size() == 1) {
      return createSingleCompositeCondition(properties, conditionType, keys.get(0));
    }

    return createMultipleCompositeCondition(properties, conditionType, keys);
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition createMultipleCompositeCondition(final List<Property.ColumnProperty> properties,
                                                            final ConditionType conditionType,
                                                            final List<Entity.Key> keys) {
    final Condition.Set conditionSet = Conditions.conditionSet(OR);
    for (int i = 0; i < keys.size(); i++) {
      conditionSet.add(createSingleCompositeCondition(properties, conditionType, keys.get(i)));
    }

    return conditionSet;
  }

  private static Condition createSingleCompositeCondition(final List<Property.ColumnProperty> properties,
                                                          final ConditionType conditionType,
                                                          final Entity.Key entityKey) {
    final Condition.Set conditionSet = Conditions.conditionSet(AND);
    for (int i = 0; i < properties.size(); i++) {
      conditionSet.add(Conditions.propertyCondition(properties.get(i).getPropertyId(), conditionType,
              entityKey == null ? null : entityKey.get(entityKey.getProperties().get(i))));
    }

    return conditionSet;
  }

  protected static List<Entity.Key> checkKeysParameter(final List<Entity.Key> keys) {
    if (nullOrEmpty(keys)) {
      throw new IllegalArgumentException("Entity key condition requires at least one key");
    }

    return keys;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(entityId);
    stream.writeObject(condition);
    stream.writeBoolean(expandRequired);
  }

  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    entityId = (String) stream.readObject();
    condition = (Condition) stream.readObject();
    expandRequired = stream.readBoolean();
  }
}
