/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.SubqueryProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static is.codion.common.db.Operator.LIKE;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Encapsulates a query condition based on a single attribute with one or more values.
 */
final class DefaultAttributeCondition implements AttributeCondition {

  private static final long serialVersionUID = 1;

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";

  /**
   * The attribute used in this condition
   */
  private final Attribute<?> attribute;

  /**
   * The values used in this condition
   */
  private final List<Object> values;

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
   * Instantiates a new DefaultAttributeCondition instance
   * @param attribute attribute
   * @param operator the condition operator
   * @param values the values, can be a Collection
   */
  DefaultAttributeCondition(final Attribute<?> attribute, final Operator operator, final Object... values) {
    requireNonNull(attribute, "attribute");
    requireNonNull(operator, "operator");
    this.attribute = attribute;
    this.operator = operator;
    this.nullCondition = values == null || values.length == 1 && values[0] == null;
    this.values = initializeValues(values == null ? new Object[] {null} : values);
    if (this.values.isEmpty()) {
      throw new IllegalArgumentException("No values specified for AttributeCondition: " + attribute);
    }
  }

  @Override
  public List<Object> getValues() {
    if (nullCondition) {
      return emptyList();
    }//null condition, uses 'x is null', not 'x = ?'

    return values;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    if (nullCondition) {
      return emptyList();
    }//null condition, uses 'x is null', not 'x = ?'

    return Collections.nCopies(values.size(), attribute);
  }

  @Override
  public Attribute<?> getAttribute() {
    return attribute;
  }

  @Override
  public Operator getOperator() {
    return operator;
  }

  @Override
  public <T> String getConditionString(final ColumnProperty<T> property) {
    if (!attribute.equals(property.getAttribute())) {
      throw new IllegalArgumentException("Property '" + property + "' is not based on attribute: " + attribute);
    }
    return createColumnPropertyConditionString((ColumnProperty<Object>) property, operator, getValues(), nullCondition, caseSensitive);
  }

  @Override
  public AttributeCondition setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  private static List<Object> initializeValues(final Object... conditionValues) {
    final List<Object> valueList = new ArrayList<>();
    for (final Object value : conditionValues) {
      if (value instanceof Collection) {
        valueList.addAll((Collection<Object>) value);
      }
      else {
        valueList.add(value);
      }
    }
    //replace Entity with Entity.Key
    for (int i = 0; i < valueList.size(); i++) {
      final Object value = valueList.get(i);
      if (value instanceof Entity) {
        valueList.set(i, ((Entity) value).getKey());
      }
      else {//assume it's all or nothing
        break;
      }
    }

    return valueList;
  }

  private static String createColumnPropertyConditionString(final ColumnProperty<Object> property,
                                                            final Operator operator, final List<Object> values,
                                                            final boolean isNullCondition, final boolean isCaseSensitive) {
    for (int i = 0; i < values.size(); i++) {
      property.getAttribute().validateType(values.get(i));
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

  private static String getValuePlaceholder(final ColumnProperty<?> property, final boolean caseSensitive) {
    return property.getAttribute().isString() && !caseSensitive ? "upper(?)" : "?";
  }

  private static String getLikeCondition(final ColumnProperty<?> property, final String columnIdentifier,
                                         final String valuePlaceholder, final boolean notLike, final List<Object> values,
                                         final int valueCount) {
    if (valueCount > 1) {
      return getInList(columnIdentifier, valuePlaceholder, valueCount, notLike);
    }
    if (property.getAttribute().isString() && containsWildcards((String) values.get(0))) {
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

  private static String initializeColumnIdentifier(final ColumnProperty<?> property, final boolean isNullCondition,
                                                   final boolean caseSensitive) {
    String columnName;
    if (property instanceof SubqueryProperty) {
      columnName = "(" + ((SubqueryProperty<?>) property).getSubQuery() + ")";
    }
    else {
      columnName = property.getColumnName();
    }

    if (!isNullCondition && property.getAttribute().isString() && !caseSensitive) {
      columnName = "upper(" + columnName + ")";
    }

    return columnName;
  }

  private static boolean containsWildcards(final String value) {
    return value.contains("%") || value.contains("_");
  }
}
