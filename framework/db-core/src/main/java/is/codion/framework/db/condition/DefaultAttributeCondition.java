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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.db.Operator.*;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Encapsulates a query condition based on a single attribute with one or more values.
 */
final class DefaultAttributeCondition<T> implements AttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private static final Map<Operator, ConditionStringProvider> OPERATOR_PROVIDER_MAP = new EnumMap<>(Operator.class);

  static {
    OPERATOR_PROVIDER_MAP.put(LIKE, new LikeConditionProvider(false));
    OPERATOR_PROVIDER_MAP.put(NOT_LIKE, new LikeConditionProvider(true));
    OPERATOR_PROVIDER_MAP.put(LESS_THAN, new LessThanConditionProvider());
    OPERATOR_PROVIDER_MAP.put(GREATER_THAN, new GreaterThanConditionProvider());
    OPERATOR_PROVIDER_MAP.put(WITHIN_RANGE, new WithinRangeConditionProvider());
    OPERATOR_PROVIDER_MAP.put(OUTSIDE_RANGE, new OutsideRangeConditionProvider());
  }

  /**
   * The attribute used in this condition
   */
  private final Attribute<T> attribute;

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
   * True if this condition should be case sensitive, only applies to conditions based on string attributes
   */
  private boolean caseSensitive = true;

  /**
   * Instantiates a new DefaultAttributeCondition instance
   * @param attribute attribute
   * @param operator the condition operator
   * @param values the values, can be a Collection
   */
  DefaultAttributeCondition(final Attribute<T> attribute, final Operator operator, final Object value) {
    requireNonNull(attribute, "attribute");
    requireNonNull(operator, "operator");
    requireNonNull(value, "value");
    this.attribute = attribute;
    this.operator = operator;
    this.values = initializeValues(value);
    this.nullCondition = this.values.isEmpty();
    if (this.nullCondition && !operator.isNullCompatible()) {
      throw new IllegalArgumentException("Operator " + operator + " is not null compatible");
    }
  }

  @Override
  public List<Object> getValues() {
    return values;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    if (values.size() == 1) {
      return singletonList(attribute);
    }

    return Collections.nCopies(values.size(), attribute);
  }

  @Override
  public Attribute<T> getAttribute() {
    return attribute;
  }

  @Override
  public Operator getOperator() {
    return operator;
  }

  @Override
  public boolean isNullCondition() {
    return nullCondition;
  }

  @Override
  public String getConditionString(final ColumnProperty<T> property) {
    if (!attribute.equals(property.getAttribute())) {
      throw new IllegalArgumentException("Property '" + property + "' is not based on attribute: " + attribute);
    }
    for (int i = 0; i < values.size(); i++) {
      property.getAttribute().validateType((T) values.get(i));
    }

    return OPERATOR_PROVIDER_MAP.get(operator).getConditionString(this, property);
  }

  @Override
  public AttributeCondition<T> setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  @Override
  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  static String getColumnIdentifier(final ColumnProperty<?> property) {
    return getColumnIdentifier(property, false, true);
  }

  static String getColumnIdentifier(final ColumnProperty<?> property, final boolean isNullCondition,
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

  static String getValuePlaceholder(final ColumnProperty<?> property, final boolean caseSensitive) {
    return property.getAttribute().isString() && !caseSensitive ? "upper(?)" : "?";
  }

  private static List<Object> initializeValues(final Object conditionValue) {
    final List<Object> valueList = new ArrayList<>();
    if (conditionValue instanceof Collection) {
      valueList.addAll((Collection<Object>) conditionValue);
    }
    else {
      valueList.add(conditionValue);
    }
    //replace Entity with Entity.Key
    for (int i = 0; i < valueList.size(); i++) {
      final Object value = valueList.get(i);
      requireNonNull(value, "value");
      if (value instanceof Entity) {
        valueList.set(i, ((Entity) value).getKey());
      }
      else {//assume it's all or nothing
        break;
      }
    }

    return valueList;
  }

  private interface ConditionStringProvider {

    String getConditionString(AttributeCondition<?> condition, ColumnProperty<?> property);
  }

  private static final class LikeConditionProvider implements ConditionStringProvider {

    private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
    private static final String IN_PREFIX = " in (";
    private static final String NOT_IN_PREFIX = " not in (";

    private final boolean negated;

    private LikeConditionProvider(final boolean negated) {
      this.negated = negated;
    }

    @Override
    public String getConditionString(final AttributeCondition<?> condition, final ColumnProperty<?> property) {
      final String columnIdentifier = getColumnIdentifier(property, condition.isNullCondition(), condition.isCaseSensitive());
      if (condition.isNullCondition()) {
        return columnIdentifier + (condition.getOperator() == LIKE ? " is null" : " is not null");
      }

      return getLikeCondition(condition, property, columnIdentifier);
    }

    private String getLikeCondition(final AttributeCondition<?> condition, final ColumnProperty<?> property,
                                        final String columnIdentifier) {
      final String valuePlaceholder = getValuePlaceholder(property, condition.isCaseSensitive());
      if (condition.getValues().size() > 1) {
        return getInList(columnIdentifier, valuePlaceholder, condition.getValues().size(), negated);
      }
      if (property.getAttribute().isString() && containsWildcards((String) condition.getValues().get(0))) {
        return columnIdentifier + (negated ? " not like " : " like ") + valuePlaceholder;
      }
      else {
        return columnIdentifier + (negated ? " <> " : " = ") + valuePlaceholder;
      }
    }

    private static String getInList(final String columnIdentifier, final String valuePlaceholder, final int valueCount, final boolean negated) {
      final boolean exceedsLimit = valueCount > IN_CLAUSE_LIMIT;
      final StringBuilder stringBuilder = new StringBuilder(exceedsLimit ? "(" : "").append(columnIdentifier).append(negated ? NOT_IN_PREFIX : IN_PREFIX);
      int cnt = 1;
      for (int i = 0; i < valueCount; i++) {
        stringBuilder.append(valuePlaceholder);
        if (cnt++ == IN_CLAUSE_LIMIT && i < valueCount - 1) {
          stringBuilder.append(negated ? ") and " : ") or ").append(columnIdentifier).append(negated ? NOT_IN_PREFIX : IN_PREFIX);
          cnt = 1;
        }
        else if (i < valueCount - 1) {
          stringBuilder.append(", ");
        }
      }
      stringBuilder.append(")").append(exceedsLimit ? ")" : "");

      return stringBuilder.toString();
    }

    private static boolean containsWildcards(final String value) {
      return value.contains("%") || value.contains("_");
    }
  }

  private static final class LessThanConditionProvider implements ConditionStringProvider {

    @Override
    public String getConditionString(final AttributeCondition<?> condition, final ColumnProperty<?> property) {
      return getColumnIdentifier(property) + " <= " + getValuePlaceholder(property, condition.isCaseSensitive());
    }
  }

  private static final class GreaterThanConditionProvider implements ConditionStringProvider {

    @Override
    public String getConditionString(final AttributeCondition<?> condition, final ColumnProperty<?> property) {
      return getColumnIdentifier(property) + " >= " + getValuePlaceholder(property, condition.isCaseSensitive());
    }
  }

  private static final class WithinRangeConditionProvider implements ConditionStringProvider {

    @Override
    public String getConditionString(final AttributeCondition<?> condition, final ColumnProperty<?> property) {
      final String columnIdentifier = getColumnIdentifier(property);
      final String valuePlaceholder = getValuePlaceholder(property, condition.isCaseSensitive());

      return "(" + columnIdentifier + " >= " + valuePlaceholder + " and " + columnIdentifier + " <= " + valuePlaceholder + ")";
    }
  }

  private static final class OutsideRangeConditionProvider implements ConditionStringProvider {

    @Override
    public String getConditionString(final AttributeCondition<?> condition, final ColumnProperty<?> property) {
      final String columnIdentifier = getColumnIdentifier(property);
      final String valuePlaceholder = getValuePlaceholder(property, condition.isCaseSensitive());

      return "(" + columnIdentifier + " <= " + valuePlaceholder + " or " + columnIdentifier + " >= " + valuePlaceholder + ")";
    }
  }
}
