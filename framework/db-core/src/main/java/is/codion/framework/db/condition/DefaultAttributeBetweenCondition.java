/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeBetweenCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T lowerBound;
  private final T upperBound;

  DefaultAttributeBetweenCondition(final Attribute<T> attribute, final T lowerBound, final T upperBound,
                                   final boolean exclusive) {
    super(attribute, exclusive ? Operator.BETWEEN_EXCLUSIVE : Operator.BETWEEN);
    this.lowerBound = requireNonNull(lowerBound, "A condition value is missing");
    this.upperBound = requireNonNull(upperBound, "A condition value is missing");
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
    return getOperator() == Operator.BETWEEN ?
            "(" + columnIdentifier + " >= ? and " + columnIdentifier + " <= ?)" :
            "(" + columnIdentifier + " > ? and " + columnIdentifier + " < ?)";
  }
}
