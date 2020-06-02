/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

/**
 * A factory for {@link Attribute} instances.
 */
public final class Attributes {

  private Attributes() {}

  /**
   * Creates a new {@link Attribute} associated with the given entityType.
   * @param name the attribute name
   * @param typeClass the class representing the attribute value type
   * @param entityType the type of the entity to associate this attribute with
   * @param <T> the attribute type
   * @return a new {@link Attribute}
   */
  public static <T> Attribute<T> attribute(final String name, final Class<T> typeClass, final EntityType entityType) {
    return new DefaultAttribute<>(name, typeClass, entityType);
  }

  /**
   * Creates a new {@link Attribute} associated with the given entityType.
   * Use this when you don't have access to an actual Attribute instance, only its name
   * and entityType, but need to access the value associated with it.
   * @param name the attribute name
   * @param entityType the type of the entity to associate this attribute with
   * @return a new {@link Attribute}
   */
  public static Attribute<Object> attribute(final String name, final EntityType entityType) {
    return new DefaultAttribute<>(name, Object.class, entityType);
  }

  /**
   * Creates a new {@link Attribute} associated with the given entityType.
   * @param name the attribute name
   * @param entityType the type of the entity to associate this attribute with
   * @return a new {@link Attribute}
   */
  public static Attribute<Entity> entityAttribute(final String name, final EntityType entityType) {
    return new DefaultEntityAttribute(name, entityType);
  }

  /**
   * Creates a new {@link Attribute} associated with the given entityType.
   * @param name the attribute name
   * @param entityType the type of the entity to associate this attribute with
   * @return a new {@link Attribute}
   */
  public static Attribute<byte[]> blobAttribute(final String name, final EntityType entityType) {
    return new DefaultBlobAttribute(name, entityType);
  }
}
