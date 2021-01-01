/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A factory for {@link EntityType} instances.
 */
public final class EntityTypes {

  private EntityTypes() {}

  /**
   * @param name the entity type name
   * @param domainName the name of the domain to associate this entity type with
   * @param entityClass the entity representation class
   * @param <T> the entity representation type
   * @return a {@link EntityType} instance with the given name
   */
  public static <T extends Entity> EntityType<T> entityType(final String name, final String domainName,
                                                            final Class<T> entityClass) {
    return new DefaultEntityType<>(domainName, name, entityClass, getResourceBundleName(entityClass));
  }

  /**
   * @param name the entity type name
   * @param domainName the name of the domain to associate this entity type with
   * @param resourceBundleName the name of a resource bundle to use for captions, if any
   * @param <T> the entity representation type
   * @return a {@link EntityType} instance with the given name
   */
  public static <T extends Entity> EntityType<T> entityType(final String name, final String domainName,
                                                            final String resourceBundleName) {
    return new DefaultEntityType<>(domainName, name, (Class<T>) Entity.class, resourceBundleName);
  }

  /**
   * @param name the entity type name
   * @param domainName the name of the domain to associate this entity type with
   * @param entityClass the entity representation class
   * @param resourceBundleName the name of a resource bundle to use for captions, if any
   * @param <T> the entity representation type
   * @return a {@link EntityType} instance with the given name
   */
  public static <T extends Entity> EntityType<T> entityType(final String name, final String domainName,
                                                            final Class<T> entityClass, final String resourceBundleName) {
    return new DefaultEntityType<>(domainName, name, entityClass, resourceBundleName);
  }

  private static <T extends Entity> String getResourceBundleName(final Class<T> entityClass) {
    try {
      ResourceBundle.getBundle(entityClass.getName());

      return entityClass.getName();
    }
    catch (final MissingResourceException e) {
      return null;
    }
  }
}
