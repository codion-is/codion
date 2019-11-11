/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.util.List;

/**
 * A wrapper property that represents a reference to another entity, typically but not necessarily based on a foreign key.
 * These do not map directly to a underlying table column, but wrap the actual column properties involved in the relation.
 * e.g.: Properties.foreignKeyProperty("reference_fk", Properties.columnProperty("reference_id")), where "reference_id" is the
 * actual name of the column involved in the reference, but "reference_fk" is simply a descriptive property ID
 */
public interface ForeignKeyProperty extends Property {

  /**
   * @return true if all reference properties comprising this
   * foreign key property are updatable
   */
  boolean isUpdatable();

  /**
   * @return the id of the entity referenced by this foreign key
   */
  String getForeignEntityId();

  /**
   * Returns an unmodifiable list containing the properties that comprise this foreign key
   * @return the reference properties
   */
  List<ColumnProperty> getProperties();

  /**
   * @return true if this foreign key is based on multiple columns
   */
  boolean isCompositeKey();

  /**
   * @return the default query fetch depth for this foreign key
   */
  int getFetchDepth();

  /**
   * @return true if this foreign key is not based on a physical (table) foreign key
   * and should not prevent deletion
   */
  boolean isSoftReference();

  /**
   * Provides setters for ForeignKeyProperty properties
   * @param <T> the ColumnProperty type
   */
  interface Builder<T extends ForeignKeyProperty> extends Property.Builder<T> {

    @Override
    T get();

    List<ColumnProperty.Builder> getPropertyBuilders();

    /**
     * @param fetchDepth the default query fetch depth for this foreign key
     * @return this ForeignKeyProperty instance
     */
    ForeignKeyProperty.Builder setFetchDepth(final int fetchDepth);

    /**
     * @param softReference true if this foreign key is not based on a physical (table) foreign key
     * and should not prevent deletion
     * @return this ForeignKeyProperty instance
     */
    ForeignKeyProperty.Builder setSoftReference(final boolean softReference);
  }
}
