/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

/**
 * A {@link java.sql.Types#BLOB} based column attribute
 */
public interface BlobColumnDefinition extends ColumnDefinition<byte[]> {

  /**
   * @return true if this value should be loaded eagerly when selected
   */
  boolean eagerlyLoaded();

  /**
   * Builds a {@link ColumnDefinition} for a boolean column.
   */
  interface Builder extends ColumnDefinition.Builder<byte[], Builder> {

    /**
     * Specifies that this value should be loaded eagerly when selected
     * @param eagerlyLoaded true if this value should be eagerly loaded
     * @return this instance
     */
    Builder eagerlyLoaded(boolean eagerlyLoaded);
  }
}
