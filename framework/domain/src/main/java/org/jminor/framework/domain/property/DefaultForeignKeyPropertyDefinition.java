/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.util.List;

final class DefaultForeignKeyPropertyDefinition extends DefaultPropertyDefinition<DefaultProperty.DefaultForeignKeyProperty>
        implements ForeignKeyPropertyDefinition<DefaultProperty.DefaultForeignKeyProperty> {

  DefaultForeignKeyPropertyDefinition(final DefaultProperty.DefaultForeignKeyProperty property) {
    super(property);
  }

  @Override
  public List<ColumnPropertyDefinition> getPropertyDefiners() {
    return property.columnPropertyDefinitions;
  }

  @Override
  public ForeignKeyPropertyDefinition setFetchDepth(final int fetchDepth) {
    property.setFetchDepth(fetchDepth);
    return this;
  }

  @Override
  public ForeignKeyPropertyDefinition setSoftReference(final boolean softReference) {
    property.setSoftReference(softReference);
    return this;
  }
}
