/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;

import java.util.Collection;
import java.util.Objects;

import static java.util.Collections.nCopies;
import static java.util.Objects.requireNonNull;

abstract class AbstractAttributeCriteria<T> extends AbstractCriteria implements AttributeCriteria<T> {

  private static final long serialVersionUID = 1;

  private final Attribute<T> attribute;
  private final Operator operator;
  private final boolean caseSensitive;

  protected AbstractAttributeCriteria(Attribute<T> attribute, Operator operator, Collection<? extends T> values,
                                      boolean caseSensitive) {
    super(requireNonNull(attribute).entityType(), nCopies(requireNonNull(values).size(), requireNonNull(attribute)), values);
    if (!caseSensitive && !attribute.isString()) {
      throw new IllegalStateException("Case insensitivity only applies to String based attributes: " + attribute);
    }
    this.attribute = attribute;
    this.operator = requireNonNull(operator);
    this.caseSensitive = caseSensitive;
  }

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
    if (!(object instanceof AbstractAttributeCriteria)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    AbstractAttributeCriteria<?> that = (AbstractAttributeCriteria<?>) object;
    return Objects.equals(operator, that.operator) &&
            caseSensitive == that.caseSensitive;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), operator, caseSensitive);
  }

  /**
   * @param columnExpression the column expression
   * @return a condition string based on this criteria
   */
  protected abstract String toString(String columnExpression);

  /**
   * @param operator the operator
   * @throws IllegalArgumentException in case the operator is not supported
   */
  protected abstract void validateOperator(Operator operator);
}
