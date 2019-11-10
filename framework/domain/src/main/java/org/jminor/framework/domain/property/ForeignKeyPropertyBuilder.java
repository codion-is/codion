/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.util.List;

/**
 * Provides setters for ForeignKeyProperty properties
 * @param <T> the ColumnProperty type
 */
public interface ForeignKeyPropertyBuilder<T extends ForeignKeyProperty> extends PropertyBuilder<T> {

  @Override
  T get();

  List<ColumnPropertyBuilder> getPropertyBuilders();

  /**
   * @param fetchDepth the default query fetch depth for this foreign key
   * @return this ForeignKeyProperty instance
   */
  ForeignKeyPropertyBuilder setFetchDepth(final int fetchDepth);

  /**
   * @param softReference true if this foreign key is not based on a physical (table) foreign key
   * and should not prevent deletion
   * @return this ForeignKeyProperty instance
   */
  ForeignKeyPropertyBuilder setSoftReference(final boolean softReference);
}
