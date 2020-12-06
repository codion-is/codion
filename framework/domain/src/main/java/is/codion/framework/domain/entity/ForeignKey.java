/*
 * Copyright (c) 2020 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.List;

public interface ForeignKey extends Attribute<Entity> {

  /**
   * @return the entity type referenced by this foreign key
   */
  EntityType<?> getReferencedEntityType();

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
  }

  /**
   * Returns a new {@link Reference} based on the given attributes.
   * @param attribute the local attribute
   * @param referencedAttribute the referenced attribute
   * @param <T> the attribute type
   * @return a new {@link Reference} based on the given attributes
   */
  static <T> Reference<T> reference(final Attribute<T> attribute, final Attribute<T> referencedAttribute) {
    return new DefaultForeignKey.DefaultReference<>(attribute, referencedAttribute);
  }
}
