/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.util.List;

public interface ForeignKeyPropertyDefinition<T extends ForeignKeyProperty> extends PropertyDefinition<T> {

  @Override
  T get();

  List<ColumnPropertyDefinition> getPropertyDefiners();

  /**
   * @param fetchDepth the default query fetch depth for this foreign key
   * @return this ForeignKeyProperty instance
   */
  ForeignKeyPropertyDefinition setFetchDepth(final int fetchDepth);

  /**
   * @param softReference true if this foreign key is not based on a physical (table) foreign key
   * and should not prevent deletion
   * @return this ForeignKeyProperty instance
   */
  ForeignKeyPropertyDefinition setSoftReference(final boolean softReference);
}
