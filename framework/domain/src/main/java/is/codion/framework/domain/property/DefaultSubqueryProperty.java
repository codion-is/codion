/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

final class DefaultSubqueryProperty<T> extends DefaultColumnProperty<T> implements SubqueryProperty<T> {

  private static final long serialVersionUID = 1;

  private final transient String subquery;

  /**
   * @param attribute the attribute, since SubqueryProperties do not map to underlying table columns,
   * the attribute should not be based on a column, only be unique for this entity
   * @param caption the caption of this property
   * @param subquery the sql query
   */
  DefaultSubqueryProperty(Attribute<T> attribute, String caption, String subquery) {
    super(attribute, caption);
    this.subquery = subquery;
  }

  @Override
  public String getSubquery() {
    return subquery;
  }

  @Override
  <P extends ColumnProperty<T>, B extends ColumnProperty.Builder<T, P, B>> ColumnProperty.Builder<T, P, B> builder() {
    return new DefaultSubqueryPropertyBuilder<>(this);
  }

  private static final class DefaultSubqueryPropertyBuilder<T, P extends ColumnProperty<T>, B extends ColumnProperty.Builder<T, P, B>>
          extends DefaultColumnPropertyBuilder<T, P, B> implements Property.Builder<T, P, B> {

    private final DefaultSubqueryProperty<T> subqueryProperty;

    private DefaultSubqueryPropertyBuilder(DefaultSubqueryProperty<T> subqueryProperty) {
      super(subqueryProperty);
      this.subqueryProperty = subqueryProperty;
      super.readOnly(true);
    }

    @Override
    public B readOnly() {
      throw new UnsupportedOperationException("Subquery properties are read only by default: " + subqueryProperty.getAttribute());
    }

    @Override
    public B readOnly(boolean readOnly) {
      throw new UnsupportedOperationException("Subquery properties are read only by default: " + subqueryProperty.getAttribute());
    }

    @Override
    public B insertable(boolean insertable) {
      throw new UnsupportedOperationException("Subquery properties are never insertable: " + subqueryProperty.getAttribute());
    }

    @Override
    public B updatable(boolean updatable) {
      throw new UnsupportedOperationException("Subquery properties are never updatable: " + subqueryProperty.getAttribute());
    }
  }
}
