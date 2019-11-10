/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.util.List;

final class DefaultForeignKeyPropertyBuilder extends DefaultPropertyBuilder<DefaultForeignKeyProperty>
        implements ForeignKeyPropertyBuilder<DefaultForeignKeyProperty> {

  DefaultForeignKeyPropertyBuilder(final DefaultForeignKeyProperty property) {
    super(property);
  }

  @Override
  public List<ColumnPropertyBuilder> getPropertyBuilders() {
    return property.columnPropertyBuilders;
  }

  @Override
  public ForeignKeyPropertyBuilder setFetchDepth(final int fetchDepth) {
    property.setFetchDepth(fetchDepth);
    return this;
  }

  @Override
  public ForeignKeyPropertyBuilder setSoftReference(final boolean softReference) {
    property.setSoftReference(softReference);
    return this;
  }
}
