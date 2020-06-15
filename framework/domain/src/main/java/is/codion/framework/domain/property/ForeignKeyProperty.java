/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

/**
 * A wrapper property that represents a reference to another entity, typically but not necessarily based on a foreign key.
 * These do not map directly to a underlying table column, but wrap the actual column properties involved in the relation.
 */
public interface ForeignKeyProperty extends Property<Entity> {

  /**
   * @return the underying attribute
   */
  @Override
  Attribute<Entity> getAttribute();

  /**
   * @return true if all reference properties comprising this foreign key property are updatable
   */
  boolean isUpdatable();

  /**
   * @return the entity type
   */
  EntityType<Entity> getEntityType();

  /**
   * @return the type of the entity referenced by this foreign key
   */
  EntityType<Entity> getReferencedEntityType();

  /**
   * Returns an unmodifiable list containing the attributes that comprise this foreign key
   * @return the reference attributes
   */
  List<Attribute<?>> getColumnAttributes();

  /**
   * Returns an unmodifiable list containing the properties that comprise this foreign key
   * @return the reference properties
   */
  List<ColumnProperty<?>> getColumnProperties();

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
     * @param fetchDepth the default query fetch depth for this foreign key
     * @return this instance
     */
    ForeignKeyProperty.Builder fetchDepth(int fetchDepth);

    /**
     * @param softReference true if this foreign key is not based on a physical (table) foreign key
     * and should not prevent deletion
     * @return this instance
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
