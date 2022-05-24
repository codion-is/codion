/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeGreaterThanCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T value;

  DefaultAttributeGreaterThanCondition(Attribute<T> attribute, T value, boolean orEqual) {
    super(attribute, orEqual ? Operator.GREATER_THAN_OR_EQUAL : Operator.GREATER_THAN);
    this.value = requireNonNull(value, "A lower bound is required");
  }

  @Override
  public List<?> getValues() {
    return singletonList(value);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return singletonList(getAttribute());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultAttributeGreaterThanCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    DefaultAttributeGreaterThanCondition<?> that = (DefaultAttributeGreaterThanCondition<?>) object;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }

  @Override
  protected String getConditionString(String columnExpression) {
    return columnExpression + (getOperator() == Operator.GREATER_THAN_OR_EQUAL ? " >= ?" : " > ?");
  }
}
