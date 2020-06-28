/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeOutsideRangeCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T lowerBound;
  private final T upperBound;

  DefaultAttributeOutsideRangeCondition(final Attribute<T> attribute, final T lowerBound, final T upperBound) {
    super(attribute);
    this.lowerBound = requireNonNull(lowerBound);
    this.upperBound = requireNonNull(upperBound);
  }

  @Override
  public Operator getOperator() {
    return Operator.WITHIN_RANGE;
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
  public String getWhereClause(final EntityDefinition definition) {
    final ColumnProperty<T> property = definition.getColumnProperty(getAttribute());
    final String columnIdentifier = getColumnIdentifier(property);

    return "(" + columnIdentifier + " <= ? or " + columnIdentifier + " >= ?)";
  }
}
