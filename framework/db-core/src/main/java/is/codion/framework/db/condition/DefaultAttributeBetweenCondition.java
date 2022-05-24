/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeBetweenCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T lowerBound;
  private final T upperBound;

  DefaultAttributeBetweenCondition(Attribute<T> attribute, T lowerBound, T upperBound,
                                   boolean exclusive) {
    super(attribute, exclusive ? Operator.BETWEEN_EXCLUSIVE : Operator.BETWEEN);
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
    return getOperator() == Operator.BETWEEN ?
            "(" + columnExpression + " >= ? and " + columnExpression + " <= ?)" :
            "(" + columnExpression + " > ? and " + columnExpression + " < ?)";
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultAttributeBetweenCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    DefaultAttributeBetweenCondition<?> that = (DefaultAttributeBetweenCondition<?>) object;
    return Objects.equals(lowerBound, that.lowerBound) &&
            Objects.equals(upperBound, that.upperBound);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), lowerBound, upperBound);
  }
}
