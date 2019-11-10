/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.util.List;

final class DefaultForeignKeyPropertyBuilder extends DefaultPropertyBuilder<DefaultForeignKeyProperty>
        implements ForeignKeyProperty.Builder<DefaultForeignKeyProperty> {

  DefaultForeignKeyPropertyBuilder(final DefaultForeignKeyProperty property) {
    super(property);
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty.Builder> getPropertyBuilders() {
    return property.columnPropertyBuilders;
  }

  /** {@inheritDoc} */
  @Override
  public ForeignKeyProperty.Builder setFetchDepth(final int fetchDepth) {
    property.setFetchDepth(fetchDepth);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public ForeignKeyProperty.Builder setSoftReference(final boolean softReference) {
    property.setSoftReference(softReference);
    return this;
  }
}
