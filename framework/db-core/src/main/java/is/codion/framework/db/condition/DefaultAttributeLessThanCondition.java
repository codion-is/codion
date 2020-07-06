/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultAttributeLessThanCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T value;

  DefaultAttributeLessThanCondition(final Attribute<T> attribute, final T value, final boolean orEquals) {
    super(attribute, orEquals ? Operator.LESS_THAN_OR_EQUAL : Operator.LESS_THAN);
    this.value = requireNonNull(value);
  }

  @Override
  public List<?> getValues() {
    return Collections.singletonList(value);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return Collections.singletonList(getAttribute());
  }

  @Override
  protected String getWhereClause(final String columnIdentifier) {
    return columnIdentifier + (getOperator() == Operator.LESS_THAN_OR_EQUAL ? " <= ?" : " < ?");
  }
}
