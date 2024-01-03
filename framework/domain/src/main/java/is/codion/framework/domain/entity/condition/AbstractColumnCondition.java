/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Column;

import java.util.Collection;
import java.util.Objects;

import static java.util.Collections.nCopies;
import static java.util.Objects.requireNonNull;

abstract class AbstractColumnCondition<T> extends AbstractCondition implements ColumnCondition<T> {

  private static final long serialVersionUID = 1;

  private final Column<T> column;
  private final Operator operator;
  private final boolean caseSensitive;

  protected AbstractColumnCondition(Column<T> column, Operator operator, Collection<? extends T> values,
                                    boolean caseSensitive) {
    super(requireNonNull(column).entityType(), nCopies(requireNonNull(values).size(), requireNonNull(column)), values);
    if (!caseSensitive && !(column.type().isString() || column.type().isCharacter())) {
      throw new IllegalStateException("Case insensitivity only applies to String and Character based columns: " + column);
    }
    this.column = column;
    this.operator = requireNonNull(operator);
    this.caseSensitive = caseSensitive;
  }

  public final Column<T> column() {
    return column;
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
    return toString(requireNonNull(definition).columns().definition(column).expression());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof AbstractColumnCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    AbstractColumnCondition<?> that = (AbstractColumnCondition<?>) object;
    return Objects.equals(operator, that.operator) &&
            caseSensitive == that.caseSensitive;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), operator, caseSensitive);
  }

  /**
   * @param columnExpression the column expression
   * @return a condition string based on this condition
   */
  protected abstract String toString(String columnExpression);

  /**
   * @param operator the operator
   * @throws IllegalArgumentException in case the operator is not supported
   */
  protected abstract void validateOperator(Operator operator);
}
