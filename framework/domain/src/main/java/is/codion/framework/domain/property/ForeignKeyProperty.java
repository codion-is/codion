/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityId;

import java.util.List;

/**
 * A wrapper property that represents a reference to another entity, typically but not necessarily based on a foreign key.
 * These do not map directly to a underlying table column, but wrap the actual column properties involved in the relation.
 * e.g.: Properties.foreignKeyProperty("reference_fk", Properties.columnProperty("reference_id")), where "reference_id" is the
 * actual name of the column involved in the reference, but "reference_fk" is simply a descriptive property id
 */
public interface ForeignKeyProperty extends Property<Entity> {

  /**
   * @return the underying attribute
   */
  @Override
  Attribute<Entity> getAttribute();

  /**
   * @return true if all reference properties comprising this foreign key property are insertable
   */
  boolean isInsertable();

  /**
   * @return true if all reference properties comprising this foreign key property are updatable
   */
  boolean isUpdatable();

  /**
   * @return the id of the entity referenced by this foreign key
   */
  EntityId getForeignEntityId();

  /**
   * Returns an unmodifiable list containing the properties that comprise this foreign key
   * @return the reference properties
   */
  List<ColumnProperty<?>> getColumnProperties();

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
   */
  interface Builder extends Property.Builder<Entity> {

    /**
     * @return the property
     */
    ForeignKeyProperty get();

    /**
     * @return the builders for the underlying column properties comprising this foreign key
     */
    List<ColumnProperty.Builder<?>> getColumnPropertyBuilders();

    /**
     * @param fetchDepth the default query fetch depth for this foreign key
     * @return this ForeignKeyProperty instance
     */
    ForeignKeyProperty.Builder fetchDepth(int fetchDepth);

    /**
     * @param softReference true if this foreign key is not based on a physical (table) foreign key
     * and should not prevent deletion
     * @return this ForeignKeyProperty instance
     */
    ForeignKeyProperty.Builder softReference(boolean softReference);

    /**
     * @param insertable specifies whether this property should be included during insert operations
     * @return this instance
     */
    ForeignKeyProperty.Builder insertable(boolean insertable);

    /**
     * @param updatable specifies whether this property should be included during update operations
     * @return this instance
     */
    ForeignKeyProperty.Builder updatable(boolean updatable);

    /**
     * @param readOnly specifies whether this property should be included during insert and update operations
     * @return this instance
     */
    ForeignKeyProperty.Builder readOnly(boolean readOnly);
  }
}
