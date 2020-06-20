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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.db.Operator.*;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Encapsulates a query condition based on a single attribute with one or more values.
 */
final class DefaultAttributeCondition implements AttributeCondition {

  private static final long serialVersionUID = 1;

  private static final Map<Operator, ConditionStringProvider> OPERATOR_PROVIDER_MAP = new HashMap<>();

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
    validateValues(operator, values);
    this.attribute = attribute;
    this.operator = operator;
    this.nullCondition = values == null || values.length == 1 && values[0] == null;
    this.values = nullCondition ? null : initializeValues(values);
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
  public boolean isNullCondition() {
    return nullCondition;
  }

  @Override
  public <T> String getConditionString(final ColumnProperty<T> property) {
    if (!attribute.equals(property.getAttribute())) {
      throw new IllegalArgumentException("Property '" + property + "' is not based on attribute: " + attribute);
    }
    final ColumnProperty<Object> objectColumnProperty = (ColumnProperty<Object>) property;
    if (!nullCondition) {
      for (int i = 0; i < values.size(); i++) {
        objectColumnProperty.getAttribute().validateType(values.get(i));
      }
    }

    return OPERATOR_PROVIDER_MAP.get(operator).getConditionString(this, objectColumnProperty);
  }

  @Override
  public AttributeCondition setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  @Override
  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  private static void validateValues(final Operator operator, final Object[] values) {
    if (values == null && !operator.isNullCompatible()) {
      throw new IllegalArgumentException("Operator " + operator + " is not null compatible");
    }
    if (values != null) {
      for (final Object value : values) {
        if (value == null && !operator.isNullCompatible()) {
          throw new IllegalArgumentException("Operator " + operator + " is not null compatible");
        }
      }
    }
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

  private interface ConditionStringProvider {

    String getConditionString(AttributeCondition condition, ColumnProperty<Object> property);
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
    public String getConditionString(final AttributeCondition condition, final ColumnProperty<Object> property) {
      final String columnIdentifier = getColumnIdentifier(property, condition.isNullCondition(), condition.isCaseSensitive());
      if (condition.isNullCondition()) {
        return columnIdentifier + (condition.getOperator() == LIKE ? " is null" : " is not null");
      }

      return getLikeCondition(condition, property, columnIdentifier);
    }

    private String getLikeCondition(final AttributeCondition condition, final ColumnProperty<?> property,
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
    public String getConditionString(final AttributeCondition condition, final ColumnProperty<Object> property) {
      return getColumnIdentifier(property) + " <= " + getValuePlaceholder(property, condition.isCaseSensitive());
    }
  }

  private static final class GreaterThanConditionProvider implements ConditionStringProvider {

    @Override
    public String getConditionString(final AttributeCondition condition, final ColumnProperty<Object> property) {
      return getColumnIdentifier(property) + " >= " + getValuePlaceholder(property, condition.isCaseSensitive());
    }
  }

  private static final class WithinRangeConditionProvider implements ConditionStringProvider {

    @Override
    public String getConditionString(final AttributeCondition condition, final ColumnProperty<Object> property) {
      final String columnIdentifier = getColumnIdentifier(property);
      final String valuePlaceholder = getValuePlaceholder(property, condition.isCaseSensitive());

      return "(" + columnIdentifier + " >= " + valuePlaceholder + " and " + columnIdentifier + " <= " + valuePlaceholder + ")";
    }
  }

  private static final class OutsideRangeConditionProvider implements ConditionStringProvider {

    @Override
    public String getConditionString(final AttributeCondition condition, final ColumnProperty<Object> property) {
      final String columnIdentifier = getColumnIdentifier(property);
      final String valuePlaceholder = getValuePlaceholder(property, condition.isCaseSensitive());

      return "(" + columnIdentifier + " <= " + valuePlaceholder + " or " + columnIdentifier + " >= " + valuePlaceholder + ")";
    }
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
}
