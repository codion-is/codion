/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

final class DefaultSubqueryProperty extends DefaultColumnProperty implements SubqueryProperty {

  private static final long serialVersionUID = 1;

  private final transient String subquery;

  /**
   * @param propertyId the property ID, since SubqueryProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   * @param subquery the sql query
   * @param columnType the actual column type
   */
  DefaultSubqueryProperty(final String propertyId, final int type, final String caption, final String subquery,
                          final int columnType) {
    super(propertyId, type, caption, columnType);
    super.setReadOnly(true);
    super.setUpdatable(false);
    this.subquery = subquery;
  }

  /** {@inheritDoc} */
  @Override
  public String getSubQuery() {
    return subquery;
  }

  void setReadOnly(final boolean readOnly) {
    throw new UnsupportedOperationException("Subquery properties are always read only");
  }
}
