/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.SubqueryProperty;

import static java.util.Objects.requireNonNull;

abstract class AbstractAttributeCondition<T> extends AbstractCondition implements AttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final Attribute<T> attribute;
  private final Operator operator;

  protected AbstractAttributeCondition(final Attribute<T> attribute, final Operator operator) {
    super(requireNonNull(attribute, "attribute").getEntityType());
    this.attribute = attribute;
    this.operator = operator;
  }

  @Override
  public final Attribute<T> getAttribute() {
    return attribute;
  }

  @Override
  public final Operator getOperator() {
    return operator;
  }

  @Override
  public AttributeCondition<String> caseSensitive(final boolean caseSensitive) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final String getConditionString(final EntityDefinition definition) {
    return getConditionString(getColumnExpression(definition));
  }

  protected abstract String getConditionString(String columnExpression);

  @Override
  public final String toString() {
    return super.toString() + ": " + attribute;
  }

  private String getColumnExpression(final EntityDefinition definition) {
    ColumnProperty<T> property = definition.getColumnProperty(attribute);
    if (property instanceof SubqueryProperty) {
      return "(" + ((SubqueryProperty<?>) property).getSubquery() + ")";
    }

    return property.getColumnExpression();
  }
}
