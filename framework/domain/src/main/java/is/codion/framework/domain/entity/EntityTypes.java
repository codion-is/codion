/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

/**
 * A factory for {@link EntityType} instances.
 */
public final class EntityTypes {

  /**
   * @param name the entity type name
   * @param domainName the name of the domain to associate this entity type with
   * @param entityClass the entity representation class
   * @param <T> the entity representation type
   * @return a {@link EntityType} instance with the given name
   */
  public static <T extends Entity> EntityType<T> entityType(final String name, final String domainName,
                                                            final Class<T> entityClass) {
    return new DefaultEntityType<>(domainName, name, entityClass);
  }
}
