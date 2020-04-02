/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.Operator;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.SubqueryProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.db.Operator.LIKE;

/**
 * Encapsulates a query condition based on a single property with one or more values.
 */
final class DefaultPropertyCondition implements PropertyCondition {

  private static final long serialVersionUID = 1;

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";

  /**
   * The property used in this condition
   */
  private final String propertyId;

  /**
   * The values used in this condition
   */
  private final List values;

  /**
   * True if this condition tests for null
   */
  private final boolean nullCondition;

  /**
   * The operator used in this condition
   */
  private final Operator operator;

  /**
   * True if this condition should be case sensitive, only applies to condition based on string properties
   */
  private boolean caseSensitive = true;

  /**
   * Instantiates a new PropertyCondition instance
   * @param propertyId the id of the property
   * @param operator the condition operator
   * @param value the value, can be a Collection
   */
  DefaultPropertyCondition(final String propertyId, final Operator operator, final Object value) {
    requireNonNull(propertyId, "propertyId");
    requireNonNull(operator, "operator");
    this.propertyId = propertyId;
    this.operator = operator;
    this.nullCondition = value == null;
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
  public Operator getOperator() {
    return operator;
  }

  /** {@inheritDoc} */
  @Override
  public String getConditionString(final ColumnProperty property) {
    return createColumnPropertyConditionString(property, operator, getValues(), nullCondition, caseSensitive);
  }

  /** {@inheritDoc} */
  @Override
  public PropertyCondition setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  private static List initializeValues(final Object value) {
    final List values = new ArrayList();
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
                                                            final Operator operator, final List values,
                                                            final boolean isNullCondition, final boolean isCaseSensitive) {
    for (int i = 0; i < values.size(); i++) {
      property.validateType(values.get(i));
    }
    final String columnIdentifier = initializeColumnIdentifier(property, isNullCondition, isCaseSensitive);
    if (isNullCondition) {
      return columnIdentifier + (operator == LIKE ? " is null" : " is not null");
    }

    final int valueCount = values.size();

    final String firstValuePlaceholder = getValuePlaceholder(property, isCaseSensitive);
    final String secondValuePlaceholder = valueCount == 2 ? getValuePlaceholder(property, isCaseSensitive) : null;

    switch (operator) {
      case LIKE:
        return getLikeCondition(property, columnIdentifier, firstValuePlaceholder, false, values, valueCount);
      case NOT_LIKE:
        return getLikeCondition(property, columnIdentifier, firstValuePlaceholder, true, values, valueCount);
      case LESS_THAN:
        return columnIdentifier + " <= " + firstValuePlaceholder;
      case GREATER_THAN:
        return columnIdentifier + " >= " + firstValuePlaceholder;
      case WITHIN_RANGE:
        return "(" + columnIdentifier + " >= " + firstValuePlaceholder + " and " + columnIdentifier + " <= " + secondValuePlaceholder + ")";
      case OUTSIDE_RANGE:
        return "(" + columnIdentifier + " <= " + firstValuePlaceholder + " or " + columnIdentifier + " >= " + secondValuePlaceholder + ")";
      default:
        throw new IllegalArgumentException("Unknown operator" + operator);
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
    final boolean exceedsLimit = valueCount > IN_CLAUSE_LIMIT;
    final StringBuilder stringBuilder = new StringBuilder(exceedsLimit ? "(" : "").append(columnIdentifier).append(not ? NOT_IN_PREFIX : IN_PREFIX);
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
    stringBuilder.append(")").append(exceedsLimit ? ")" : "");

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
}
