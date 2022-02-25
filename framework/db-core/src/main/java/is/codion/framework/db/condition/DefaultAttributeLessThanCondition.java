/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeLessThanCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T value;

  DefaultAttributeLessThanCondition(Attribute<T> attribute, T value, boolean orEqual) {
    super(attribute, orEqual ? Operator.LESS_THAN_OR_EQUAL : Operator.LESS_THAN);
    this.value = requireNonNull(value, "An upper bound is required");
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
  protected String getConditionString(String columnExpression) {
    return columnExpression + (getOperator() == Operator.LESS_THAN_OR_EQUAL ? " <= ?" : " < ?");
  }
}
