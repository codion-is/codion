/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.util.List;

/**
 * A property that represents a reference to another entity, typically but not necessarily based on a foreign key.
 */
public interface ForeignKeyProperty extends Property<Entity> {

  /**
   * @return the foreign key attribute this property is based on.
   */
  @Override
  ForeignKey attribute();

  /**
   * @return the type of the entity referenced by this foreign key
   */
  EntityType referencedType();

  /**
   * @return the default query fetch depth for this foreign key
   */
  int fetchDepth();

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
   * @return the {@link ForeignKey.Reference}s that comprise this foreign key
   */
  List<ForeignKey.Reference<?>> references();

  /**
   * @return the attributes to select when fetching entities referenced via this foreign key, an empty list in case of all attributes
   */
  List<Attribute<?>> selectAttributes();

  /**
   * Provides setters for ForeignKeyProperty properties
   */
  interface Builder extends Property.Builder<Entity, Builder> {

    /**
     * @param fetchDepth the default query fetch depth for this foreign key
     * @return this instance
     */
    ForeignKeyProperty.Builder fetchDepth(int fetchDepth);

    /**
     * Specifies that this foreign key is not based on a physical (table) foreign key
     * and should not prevent deletion
     * @param softReference true if this is a soft foreign key, which does not prevent deletion
     * @return this instance
     */
    ForeignKeyProperty.Builder softReference(boolean softReference);

    /**
     * Marks the given foreign key reference attribute as read-only, as in, not updated when the foreign key value is set.
     * @param referenceAttribute the reference attribute
     * @return this instance
     */
    ForeignKeyProperty.Builder readOnly(Attribute<?> referenceAttribute);

    /**
     * Specifies the attributes from the referenced entity to select. Note that the primary key attributes
     * are always selected and do not have to be added via this method.
     * @param attributes the attributes to select
     * @return this instance
     */
    ForeignKeyProperty.Builder selectAttributes(Attribute<?>... attributes);
  }
}
