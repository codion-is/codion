/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
  DefaultSubqueryProperty(final Attribute<T> attribute, final String caption, final String subquery) {
    super(attribute, caption);
    super.readOnly();
    this.subquery = subquery;
  }

  @Override
  public String getSubQuery() {
    return subquery;
  }

  @Override
  <B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> builder() {
    return new DefaultSubqueryPropertyBuilder<>(this);
  }

  private static final class DefaultSubqueryPropertyBuilder<T, B extends ColumnProperty.Builder<T, B>>
          extends DefaultColumnPropertyBuilder<T, B> implements Property.Builder<T, B> {

    private final DefaultSubqueryProperty<T> subqueryProperty;

    private DefaultSubqueryPropertyBuilder(final DefaultSubqueryProperty<T> subqueryProperty) {
      super(subqueryProperty);
      this.subqueryProperty = subqueryProperty;
    }

    @Override
    public SubqueryProperty<T> get() {
      return subqueryProperty;
    }

    @Override
    public B readOnly() {
      throw new UnsupportedOperationException("Subquery properties are read only by default: " + property.getAttribute());
    }

    @Override
    public B readOnly(final boolean readOnly) {
      throw new UnsupportedOperationException("Subquery properties are read only by default: " + property.getAttribute());
    }

    @Override
    public B insertable(final boolean insertable) {
      throw new UnsupportedOperationException("Subquery properties are never insertable: " + property.getAttribute());
    }

    @Override
    public B updatable(final boolean updatable) {
      throw new UnsupportedOperationException("Subquery properties are never updatable: " + property.getAttribute());
    }
  }
}
