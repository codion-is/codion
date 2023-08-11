/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.List;

public interface ForeignKey extends Attribute<Entity> {

  /**
   * @return the entity type referenced by this foreign key
   */
  EntityType referencedType();

  /**
   * @return the {@link Reference}s that comprise this key
   */
  List<Reference<?>> references();

  /**
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return the reference that is based on the given attribute
   */
  <T> Reference<T> reference(Column<T> attribute);

  /**
   * Represents a foreign key reference between attributes.
   * @param <T> the attribute type
   */
  interface Reference<T> {

    /**
     * @return the attribute in the detail entity
     */
    Column<T> attribute();

    /**
     * @return the attribute in the master entity
     */
    Column<T> referencedAttribute();
  }

  /**
   * Returns a new {@link Reference} based on the given attributes.
   * @param attribute the local attribute
   * @param referencedAttribute the referenced attribute
   * @param <T> the attribute type
   * @return a new {@link Reference} based on the given attributes
   */
  static <T> Reference<T> reference(Column<T> attribute, Column<T> referencedAttribute) {
    return new DefaultForeignKey.DefaultReference<>(attribute, referencedAttribute);
  }

  /**
   * Creates a new {@link ForeignKey} based on the given entityType and references.
   * @param entityType the entityType owning this foreign key
   * @param name the attribute name
   * @param references the references
   * @return a new {@link ForeignKey}
   * @see ForeignKey#reference(Column, Column)
   */
  static ForeignKey foreignKey(EntityType entityType, String name, List<ForeignKey.Reference<?>> references) {
    return new DefaultForeignKey(name, entityType, references);
  }
}
