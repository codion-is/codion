/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeGreaterThanCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T value;

  DefaultAttributeGreaterThanCondition(final Attribute<T> attribute, final T value, final boolean orEqual) {
    super(attribute, orEqual ? Operator.GREATER_THAN_OR_EQUAL : Operator.GREATER_THAN);
    this.value = requireNonNull(value, "A condition value is missing");
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
  protected String getWhereClause(final String columnIdentifier) {
    return columnIdentifier + (getOperator() == Operator.GREATER_THAN_OR_EQUAL ? " >= ?" : " > ?");
  }
}
