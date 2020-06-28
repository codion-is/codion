/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.SubqueryProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.db.Operator.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Encapsulates a query condition based on a single attribute with one or more values.
 */
final class DefaultAttributeCondition<T> extends AbstractCondition implements AttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private static final Map<Operator, ConditionStringProvider> OPERATOR_PROVIDER_MAP = new EnumMap<>(Operator.class);

  static {
    OPERATOR_PROVIDER_MAP.put(EQUALS, new EqualsConditionProvider(false));
    OPERATOR_PROVIDER_MAP.put(NOT_EQUALS, new EqualsConditionProvider(true));
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
    super(requireNonNull(attribute, "attribute").getEntityType());
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
  public List<?> getValues() {
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
  public String getWhereClause(final EntityDefinition definition) {
    for (int i = 0; i < values.size(); i++) {
      //better late than never
      attribute.validateType((T) values.get(i));
    }

    return OPERATOR_PROVIDER_MAP.get(operator).getConditionString(this, definition.getColumnProperty(attribute));
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
  public AttributeCondition<String> setCaseSensitive(final boolean caseSensitive) {
    if (!attribute.isString()) {
      throw new IllegalStateException("Attribute " + attribute + " is not a String attribute");
    }
    this.caseSensitive = caseSensitive;

    return (AttributeCondition<String>) this;
  }

  @Override
  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  @Override
  public String toString() {
    return super.toString() + ": " + attribute;
  }

  private boolean isNullCondition() {
    return nullCondition;
  }

  private List<Object> initializeValues(final Object conditionValue) {
    final List<Object> valueList = new ArrayList<>();
    if (conditionValue instanceof Collection) {
      valueList.addAll((Collection<Object>) conditionValue);
    }
    else {
      valueList.add(conditionValue);
    }
    if (operator.getValues() == Values.ONE && valueList.size() != 1) {
      throw new IllegalArgumentException("Single value expected for operator: " + operator);
    }
    if (operator.getValues() == Values.TWO && valueList.size() != 2) {
      throw new IllegalArgumentException("Two values expected for operator: " + operator);
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

  private static String getColumnIdentifier(final ColumnProperty<?> property) {
    return getColumnIdentifier(property, false, true);
  }

  private static String getColumnIdentifier(final ColumnProperty<?> property, final boolean isNullCondition,
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

  private static String getValuePlaceholder(final ColumnProperty<?> property, final boolean caseSensitive) {
    return property.getAttribute().isString() && !caseSensitive ? "upper(?)" : "?";
  }

  private interface ConditionStringProvider {

    String getConditionString(DefaultAttributeCondition<?> condition, ColumnProperty<?> property);
  }

  private static final class EqualsConditionProvider implements ConditionStringProvider {

    private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
    private static final String IN_PREFIX = " in (";
    private static final String NOT_IN_PREFIX = " not in (";

    private final boolean negated;

    private EqualsConditionProvider(final boolean negated) {
      this.negated = negated;
    }

    @Override
    public String getConditionString(final DefaultAttributeCondition<?> condition, final ColumnProperty<?> property) {
      final String columnIdentifier = getColumnIdentifier(property, condition.isNullCondition(), condition.isCaseSensitive());
      if (condition.isNullCondition()) {
        return columnIdentifier + (condition.getOperator() == EQUALS ? " is null" : " is not null");
      }

      return getLikeCondition(condition, property, columnIdentifier);
    }

    private String getLikeCondition(final DefaultAttributeCondition<?> condition, final ColumnProperty<?> property,
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
    public String getConditionString(final DefaultAttributeCondition<?> condition, final ColumnProperty<?> property) {
      return getColumnIdentifier(property) + " <= " + getValuePlaceholder(property, condition.isCaseSensitive());
    }
  }

  private static final class GreaterThanConditionProvider implements ConditionStringProvider {

    @Override
    public String getConditionString(final DefaultAttributeCondition<?> condition, final ColumnProperty<?> property) {
      return getColumnIdentifier(property) + " >= " + getValuePlaceholder(property, condition.isCaseSensitive());
    }
  }

  private static final class WithinRangeConditionProvider implements ConditionStringProvider {

    @Override
    public String getConditionString(final DefaultAttributeCondition<?> condition, final ColumnProperty<?> property) {
      final String columnIdentifier = getColumnIdentifier(property);
      final String valuePlaceholder = getValuePlaceholder(property, condition.isCaseSensitive());

      return "(" + columnIdentifier + " >= " + valuePlaceholder + " and " + columnIdentifier + " <= " + valuePlaceholder + ")";
    }
  }

  private static final class OutsideRangeConditionProvider implements ConditionStringProvider {

    @Override
    public String getConditionString(final DefaultAttributeCondition<?> condition, final ColumnProperty<?> property) {
      final String columnIdentifier = getColumnIdentifier(property);
      final String valuePlaceholder = getValuePlaceholder(property, condition.isCaseSensitive());

      return "(" + columnIdentifier + " <= " + valuePlaceholder + " or " + columnIdentifier + " >= " + valuePlaceholder + ")";
    }
  }

  static final class DefaultBuilder<T> implements AttributeCondition.Builder<T> {

    private final Attribute<T> attribute;

    DefaultBuilder(final Attribute<T> attribute) {
      this.attribute = requireNonNull(attribute, "attribute");
    }

    @Override
    public AttributeCondition<T> equalTo(final T value) {
      return new DefaultAttributeCondition<>(attribute, EQUALS, requireNonNull(value));
    }

    @Override
    public AttributeCondition<T> equalTo(final T... values) {
      return equalTo(asList(requireNonNull(values)));
    }

    @Override
    public AttributeCondition<T> equalTo(final Collection<? extends T> values) {
      return new DefaultAttributeCondition<>(attribute, EQUALS, requireNonNull(values));
    }

    @Override
    public AttributeCondition<T> notEqualTo(final T value) {
      return new DefaultAttributeCondition<>(attribute, NOT_EQUALS, requireNonNull(value));
    }

    @Override
    public AttributeCondition<T> notEqualTo(final T... values) {
      return notEqualTo(asList(requireNonNull(values)));
    }

    @Override
    public AttributeCondition<T> notEqualTo(final Collection<? extends T> values) {
      return new DefaultAttributeCondition<>(attribute, NOT_EQUALS, requireNonNull(values));
    }

    @Override
    public AttributeCondition<T> lessThan(final T value) {
      return new DefaultAttributeCondition<>(attribute, LESS_THAN, value);
    }

    @Override
    public AttributeCondition<T> greaterThan(final T value) {
      return new DefaultAttributeCondition<>(attribute, GREATER_THAN, value);
    }

    @Override
    public AttributeCondition<T> withinRange(final T lowerBound, final T upperBound) {
      return new DefaultAttributeCondition<>(attribute, WITHIN_RANGE, asList(lowerBound, upperBound));
    }

    @Override
    public AttributeCondition<T> outsideRange(final T lowerBound, final T upperBound) {
      return new DefaultAttributeCondition<>(attribute, OUTSIDE_RANGE, asList(lowerBound, upperBound));
    }

    @Override
    public AttributeCondition<T> isNull() {
      return new DefaultAttributeCondition<>(attribute, EQUALS, emptyList());
    }

    @Override
    public AttributeCondition<T> isNotNull() {
      return new DefaultAttributeCondition<>(attribute, NOT_EQUALS, emptyList());
    }

    @Override
    public AttributeCondition<Entity> equalTo(final Key key) {
      return new DefaultAttributeCondition<>((Attribute<Entity>) attribute, EQUALS, key);
    }

    @Override
    public AttributeCondition<Entity> equalTo(final Key... keys) {
      return equalTo(asList(requireNonNull(keys)));
    }

    @Override
    public AttributeCondition<Entity> equalTo(final List<Key> keys) {
      checkKeysParameter(keys);
      return new DefaultAttributeCondition<>((Attribute<Entity>) attribute, EQUALS, keys);
    }

    private static void checkKeysParameter(final List<Key> keys) {
      requireNonNull(keys, "keys");
      if (keys.isEmpty()) {
        throw new IllegalArgumentException("One or more keys must be provided for condition");
      }
    }
  }
}
