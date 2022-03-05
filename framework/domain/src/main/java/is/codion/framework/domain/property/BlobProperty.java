/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

/**
 * A {@link java.sql.Types#BLOB} based column property
 */
public interface BlobProperty extends ColumnProperty<byte[]> {

  /**
   * @return true if this value should be loaded eagerly when selected
   */
  boolean isEagerlyLoaded();

  /**
   * Provides setters for BlobProperty properties
   */
  interface Builder extends ColumnProperty.Builder<byte[], BlobProperty, Builder> {

    /**
     * Specifies that this value should be loaded eagerly when selected
     * @return this instance
     */
    BlobProperty.Builder eagerlyLoaded();
  }
}
