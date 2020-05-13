/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.domain.property;

/**
 * A {@link java.sql.Types#BLOB} based column property
 */
public interface BlobProperty extends ColumnProperty {

  /**
   * @return true if this value should be loaded eagerly when selected
   */
  boolean isEagerlyLoaded();

  /**
   * Provides setters for BlobProperty properties
   */
  interface Builder extends ColumnProperty.Builder {

    /**
     * @return the property
     */
    BlobProperty get();

    /**
     * Specifies whether the value should be loaded eagerly when selected
     * @param eagerlyLoaded if true then this value is loaded automatically when entities are selected
     * @return this instance
     */
    BlobProperty.Builder eagerlyLoaded(boolean eagerlyLoaded);
  }
}
