/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;

import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultAttributeLessThanCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T value;

  DefaultAttributeLessThanCondition(final Attribute<T> attribute, final T value) {
    super(attribute);
    this.value = requireNonNull(value);
  }

  @Override
  public Operator getOperator() {
    return Operator.LESS_THAN;
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
  public String getWhereClause(final EntityDefinition definition) {
    final ColumnProperty<T> property = definition.getColumnProperty(getAttribute());

    return getColumnIdentifier(property) + " <= ?";
  }
}
