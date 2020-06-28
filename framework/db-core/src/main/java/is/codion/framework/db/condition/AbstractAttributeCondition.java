/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.SubqueryProperty;

import static java.util.Objects.requireNonNull;

abstract class AbstractAttributeCondition<T> extends AbstractCondition implements AttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final Attribute<T> attribute;

  protected AbstractAttributeCondition(final Attribute<T> attribute) {
    super(requireNonNull(attribute, "attribute").getEntityType());
    this.attribute = attribute;
  }

  @Override
  public final Attribute<T> getAttribute() {
    return attribute;
  }

  @Override
  public AttributeCondition<String> setCaseSensitive(final boolean caseSensitive) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final String toString() {
    return super.toString() + ": " + attribute;
  }

  protected final String getColumnIdentifier(final ColumnProperty<?> property) {
    if (property instanceof SubqueryProperty) {
      return "(" + ((SubqueryProperty<?>) property).getSubQuery() + ")";
    }

    return property.getColumnName();
  }
}
