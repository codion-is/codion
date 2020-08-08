/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

/**
 * A property that represents a reference to another entity, typically but not necessarily based on a foreign key.
 */
public interface ForeignKeyProperty extends Property<Entity> {

  /**
   * @return the underying attribute
   */
  @Override
  Attribute<Entity> getAttribute();

  /**
   * @return the entity type
   */
  EntityType<?> getEntityType();

  /**
   * @return the type of the entity referenced by this foreign key
   */
  EntityType<Entity> getReferencedEntityType();

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
   * @return the {@link Reference}s that comprise this key
   */
  List<Reference<?>> getReferences();

  /**
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return the reference that is based on the given attribute
   */
  <T> Reference<T> getReference(Attribute<T> attribute);

  /**
   * Represents a foreign key reference between attributes.
   * @param <T> the attribute type
   */
  interface Reference<T> {

    /**
     * @return the attribute in the detail entity
     */
    Attribute<T> getAttribute();

    /**
     * @return the attribute in the master entity
     */
    Attribute<T> getReferencedAttribute();

    /**
     * @return true if this attribute should not be set when setting the foreign key entity
     */
    boolean isReadOnly();
  }

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
     * Adds a reference to this foreign key
     * @param attribute the attribute
     * @param referencedAttribute the referenced attribute in the foreign entity
     * @param <T> the attribute type
     * @return this instance
     */
    <T> ForeignKeyProperty.Builder reference(Attribute<T> attribute, Attribute<T> referencedAttribute);

    /**
     * Adds a reference to this foreign key, that is not updated when the foreign key value is set
     * @param attribute the attribute
     * @param referencedAttribute the referenced attribute in the foreign entity
     * @param <T> the attribute type
     * @return this instance
     */
    <T> ForeignKeyProperty.Builder referenceReadOnly(Attribute<T> attribute, Attribute<T> referencedAttribute);
  }
}
