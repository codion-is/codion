/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

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
    super.setInsertable(false);
    super.setUpdatable(false);
    this.subquery = subquery;
  }

  @Override
  public String getSubQuery() {
    return subquery;
  }

  /**
   * @return a builder for this property instance
   */
  @Override
  SubqueryProperty.Builder<T> builder() {
    return new DefaultSubqueryPropertyBuilder<>(this);
  }

  private static final class DefaultSubqueryPropertyBuilder<T>
          extends DefaultColumnPropertyBuilder<T> implements Property.Builder<T> {

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
    public ColumnProperty.Builder<T> readOnly(final boolean readOnly) {
      throw new UnsupportedOperationException("Subquery properties are always read only");
    }

    @Override
    public SubqueryProperty.Builder<T> insertable(final boolean insertable) {
      throw new UnsupportedOperationException("Subquery properties are never insertable");
    }

    @Override
    public SubqueryProperty.Builder<T> updatable(final boolean updatable) {
      throw new UnsupportedOperationException("Subquery properties are never updatable");
    }
  }
}
