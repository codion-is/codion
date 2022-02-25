/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeNotBetweenCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T lowerBound;
  private final T upperBound;

  DefaultAttributeNotBetweenCondition(Attribute<T> attribute, T lowerBound, T upperBound,
                                      boolean exclusive) {
    super(attribute, exclusive ? Operator.NOT_BETWEEN_EXCLUSIVE : Operator.NOT_BETWEEN);
    this.lowerBound = requireNonNull(lowerBound, "A lower bound is required");
    this.upperBound = requireNonNull(upperBound, "An upper bound is required");
  }

  @Override
  public List<?> getValues() {
    return asList(lowerBound, upperBound);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return asList(getAttribute(), getAttribute());
  }

  @Override
  protected String getConditionString(String columnExpression) {
    return getOperator() == Operator.NOT_BETWEEN ?
            "(" + columnExpression + " <= ? or " + columnExpression + " >= ?)" :
            "(" + columnExpression + " < ? or " + columnExpression + " > ?)";
  }
}
