/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

/**
 * A {@link java.sql.Types#BLOB} based column property
 */
public interface BlobProperty extends ColumnProperty {

  /**
   * @return true if this value should not be loaded eagerly when selected
   */
  boolean isLazyLoaded();

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
     * @param lazyLoaded if true then this value is not loaded automatically when selected
     * @return this instance
     */
    BlobProperty.Builder setLazyLoaded(final boolean lazyLoaded);
  }
}
