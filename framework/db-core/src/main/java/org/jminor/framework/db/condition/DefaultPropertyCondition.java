/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.SubqueryProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.db.ConditionType.LIKE;

/**
 * Encapsulates a query condition based on a single property with one or more values.
 */
final class DefaultPropertyCondition implements Condition.PropertyCondition {

  private static final long serialVersionUID = 1;

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";

  /**
   * The property used in this condition
   */
  private String propertyId;

  /**
   * The values used in this condition
   */
  private ArrayList values;

  /**
   * True if this condition tests for null
   */
  private boolean nullCondition;

  /**
   * The search type used in this condition
   */
  private ConditionType conditionType;

  /**
   * True if this condition should be case sensitive, only applies to condition based on string properties
   */
  private boolean caseSensitive;

  /**
   * Instantiates a new PropertyCondition instance
   * @param propertyId the id of the property
   * @param conditionType the condition type
   * @param value the value, can be a Collection
   */
  DefaultPropertyCondition(final String propertyId, final ConditionType conditionType, final Object value,
                           final boolean caseSensitive) {
    requireNonNull(propertyId, "propertyId");
    requireNonNull(conditionType, "conditionType");
    this.propertyId = propertyId;
    this.conditionType = conditionType;
    this.nullCondition = value == null;
    this.caseSensitive = caseSensitive;
    this.values = initializeValues(value);
    if (this.values.isEmpty()) {
      throw new IllegalArgumentException("No values specified for PropertyCondition: " + propertyId);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List getValues() {
    if (nullCondition) {
      return emptyList();
    }//null condition, uses 'x is null', not 'x = ?'

    return values;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getPropertyIds() {
    if (nullCondition) {
      return emptyList();
    }//null condition, uses 'x is null', not 'x = ?'

    return Collections.nCopies(values.size(), propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public String getPropertyId() {
    return propertyId;
  }

  /** {@inheritDoc} */
  @Override
  public String getConditionString(final Entity.Definition definition) {
    return createColumnPropertyConditionString(definition.getColumnProperty(propertyId),
            conditionType, getValues(), nullCondition, caseSensitive);
  }

  /** {@inheritDoc} */
  @Override
  public Condition expand(final Entity.Definition definition) {
    final Property property = definition.getProperty(propertyId);
    if (property instanceof ForeignKeyProperty) {
      return foreignKeyCondition((ForeignKeyProperty) property, conditionType, getValues());
    }

    return this;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(propertyId);
    stream.writeObject(conditionType);
    stream.writeBoolean(nullCondition);
    stream.writeBoolean(caseSensitive);
    stream.writeObject(values);
  }

  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    propertyId = (String) stream.readObject();
    conditionType = (ConditionType) stream.readObject();
    nullCondition = stream.readBoolean();
    caseSensitive = stream.readBoolean();
    values = (ArrayList) stream.readObject();
  }

  private static ArrayList initializeValues(final Object value) {
    final ArrayList values = new ArrayList();
    if (value instanceof Collection) {
      values.addAll((Collection) value);
    }
    else {
      values.add(value);
    }
    //replace Entity with Entity.Key
    for (int i = 0; i < values.size(); i++) {
      final Object val = values.get(i);
      if (val instanceof Entity) {
        values.set(i, ((Entity) val).getKey());
      }
      else {//assume it's all or nothing
        break;
      }
    }

    return values;
  }

  private static String createColumnPropertyConditionString(final ColumnProperty property,
                                                            final ConditionType conditionType, final List values,
                                                            final boolean isNullCondition, final boolean isCaseSensitive) {
    for (int i = 0; i < values.size(); i++) {
      property.validateType(values.get(i));
    }
    final String columnIdentifier = initializeColumnIdentifier(property, isNullCondition, isCaseSensitive);
    if (isNullCondition) {
      return columnIdentifier + (conditionType == LIKE ? " is null" : " is not null");
    }

    final int valueCount = values.size();

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

  private static String getValuePlaceholder(final ColumnProperty property, final boolean caseSensitive) {
    return property.isString() && !caseSensitive ? "upper(?)" : "?";
  }

  private static String getLikeCondition(final ColumnProperty property, final String columnIdentifier,
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

  private static String initializeColumnIdentifier(final ColumnProperty property, final boolean isNullCondition,
                                                   final boolean caseSensitive) {
    String columnName;
    if (property instanceof SubqueryProperty) {
      columnName = "(" + ((SubqueryProperty) property).getSubQuery() + ")";
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

  private static Condition foreignKeyCondition(final ForeignKeyProperty foreignKeyProperty,
                                               final ConditionType conditionType, final Collection values) {
    final List<Entity.Key> keys = getKeys(values);
    if (foreignKeyProperty.isCompositeKey()) {
      return Conditions.createCompositeKeyCondition(foreignKeyProperty.getProperties(), conditionType, keys);
    }

    if (keys.size() == 1) {
      final Entity.Key entityKey = keys.get(0);

      return Conditions.propertyCondition(foreignKeyProperty.getProperties().get(0).getPropertyId(), conditionType,
              entityKey == null ? null : entityKey.getFirstValue());
    }

    return Conditions.propertyCondition(foreignKeyProperty.getProperties().get(0).getPropertyId(), conditionType,
            Entities.getValues(keys));
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
}
