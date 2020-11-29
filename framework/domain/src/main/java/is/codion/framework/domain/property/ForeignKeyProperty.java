/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKeyAttribute;

import java.util.List;

/**
 * A property that represents a reference to another entity, typically but not necessarily based on a foreign key.
 */
public interface ForeignKeyProperty extends Property<Entity> {

  /**
   * @return the foreign key attribute this property is based on.
   */
  @Override
  ForeignKeyAttribute getAttribute();

  /**
   * @return the type of the entity referenced by this foreign key
   */
  EntityType<?> getReferencedEntityType();

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
   * Returns true if the given foreign key reference attribute as read-only, as in, not updated when the foreign key value is set.
   * @param referenceAttribute the reference attribute
   * @return true if the given foreign key reference attribute as read-only
   */
  boolean isReadOnly(Attribute<?> referenceAttribute);

  /**
   * @return the {@link Reference}s that comprise this key
   */
  List<ForeignKeyAttribute.Reference<?>> getReferences();

  /**
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return the reference that is based on the given attribute
   */
  <T> ForeignKeyAttribute.Reference<T> getReference(Attribute<T> attribute);

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
     * Marks the given foreign key reference attribute as read-only, as in, not updated when the foreign key value is set.
     * @param referenceAttribute the reference attribute
     * @param <T> the attribute type
     * @return this instance
     */
    <T> ForeignKeyProperty.Builder readOnly(Attribute<T> referenceAttribute);
  }
}
