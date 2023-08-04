/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

abstract class AbstractAttributeCondition<T> extends AbstractCondition implements AttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final Attribute<T> attribute;
  private final Operator operator;
  private final boolean caseSensitive;

  protected AbstractAttributeCondition(Attribute<T> attribute, Operator operator, boolean caseSensitive) {
    super(requireNonNull(attribute, "attribute").entityType());
    if (!caseSensitive && !attribute.isString()) {
      throw new IllegalStateException("Case insensitivity only applies to String based attributes: " + attribute);
    }
    this.attribute = attribute;
    this.operator = requireNonNull(operator);
    this.caseSensitive = caseSensitive;
  }

  @Override
  public final Attribute<T> attribute() {
    return attribute;
  }

  @Override
  public final Operator operator() {
    return operator;
  }

  @Override
  public final boolean caseSensitive() {
    return caseSensitive;
  }

  @Override
  public final String toString(EntityDefinition definition) {
    return toString(requireNonNull(definition).columnProperty(attribute).columnExpression());
  }

  @Override
  public final String toString() {
    return super.toString() + ": " + attribute;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof AbstractAttributeCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    AbstractAttributeCondition<?> that = (AbstractAttributeCondition<?>) object;
    return attribute.equals(that.attribute) &&
            operator == that.operator &&
            caseSensitive == that.caseSensitive;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), attribute, operator, caseSensitive);
  }

  protected abstract String toString(String columnExpression);
}
