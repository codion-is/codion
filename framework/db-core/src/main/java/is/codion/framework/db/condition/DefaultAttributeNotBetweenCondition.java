/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeNotBetweenCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T lowerBound;
  private final T upperBound;

  DefaultAttributeNotBetweenCondition(final Attribute<T> attribute, final T lowerBound, final T upperBound,
                                      final boolean exclusive) {
    super(attribute, exclusive ? Operator.NOT_BETWEEN_EXCLUSIVE : Operator.NOT_BETWEEN);
    this.lowerBound = requireNonNull(lowerBound);
    this.upperBound = requireNonNull(upperBound);
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
  protected String getWhereClause(final String columnIdentifier) {
    return getOperator() == Operator.NOT_BETWEEN ?
            "(" + columnIdentifier + " <= ? or " + columnIdentifier + " >= ?)" :
            "(" + columnIdentifier + " < ? or " + columnIdentifier + " > ?)";
  }
}
