/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.identity.Identity;

/**
 * A factory for {@link Attribute} instances.
 */
public final class Attributes {

  private Attributes() {}

  /**
   * Creates a new {@link Attribute}.
   * @param name the attribute name
   * @param typeClass the class representing the attribute value type
   * @param entityId the id of the entity to associate this attribute with
   * @param <T> the attribute type
   * @return a new {@link Attribute}
   */
  public static <T> Attribute<T> attribute(final String name, final Class<T> typeClass, final Identity entityId) {
    return new DefaultAttribute<>(name, typeClass, entityId);
  }

  /**
   * Creates a new {@link Attribute} associated with the given entityId.
   * Use this when you don't have access to an actual Attribute instance, only its name
   * and entityId, but need to access the value associated with it.
   * @param name the attribute name
   * @param entityId the id of the entity to associate this attribute with
   * @return a new {@link Attribute}
   */
  public static Attribute<Object> attribute(final String name, final Identity entityId) {
    return new DefaultAttribute<>(name, Object.class, entityId);
  }

  /**
   * Creates a new {@link Attribute<Entity>}.
   * @param name the attribute name
   * @param entityId the id of the entity to associate this attribute with
   * @return a new {@link Attribute<Entity>}
   */
  public static Attribute<Entity> entityAttribute(final String name, final Identity entityId) {
    return new DefaultEntityAttribute(name, entityId);
  }

  /**
   * Creates a new {@link BlobAttribute}.
   * @param name the attribute name
   * @param entityId the id of the entity to associate this attribute with
   * @return a new {@link BlobAttribute}
   */
  public static BlobAttribute blobAttribute(final String name, final Identity entityId) {
    return new DefaultBlobAttribute(name, entityId);
  }
}
