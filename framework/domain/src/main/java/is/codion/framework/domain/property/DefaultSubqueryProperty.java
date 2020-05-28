/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

final class DefaultSubqueryProperty extends DefaultColumnProperty implements SubqueryProperty {

  private static final long serialVersionUID = 1;

  private final transient String subquery;

  /**
   * @param  propertyId the propertyId, since SubqueryProperties do not map to underlying table columns,
   * the property id should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   * @param subquery the sql query
   */
  DefaultSubqueryProperty(final Attribute<?> propertyId, final int type, final String caption, final String subquery) {
    super(propertyId, type, caption);
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
  SubqueryProperty.Builder builder() {
    return new DefaultSubqueryPropertyBuilder(this);
  }

  private static final class DefaultSubqueryPropertyBuilder
          extends DefaultColumnPropertyBuilder implements Property.Builder {

    private final DefaultSubqueryProperty subqueryProperty;

    private DefaultSubqueryPropertyBuilder(final DefaultSubqueryProperty subqueryProperty) {
      super(subqueryProperty);
      this.subqueryProperty = subqueryProperty;
    }

    @Override
    public SubqueryProperty get() {
      return subqueryProperty;
    }

    @Override
    public ColumnProperty.Builder readOnly(final boolean readOnly) {
      throw new UnsupportedOperationException("Subquery properties are always read only");
    }

    @Override
    public SubqueryProperty.Builder insertable(final boolean insertable) {
      throw new UnsupportedOperationException("Subquery properties are never insertable");
    }

    @Override
    public SubqueryProperty.Builder updatable(final boolean updatable) {
      throw new UnsupportedOperationException("Subquery properties are never updatable");
    }
  }
}
