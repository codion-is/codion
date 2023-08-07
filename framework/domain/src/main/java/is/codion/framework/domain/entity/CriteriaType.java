/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

/**
 * Defines a custom criteria type.
 */
public interface CriteriaType {

  /**
   * @return the entity type
   */
  EntityType entityType();

  /**
   * @return the name
   */
  String name();

  /**
   * Instantiates a new {@link CriteriaType} for the given entity type
   * @param entityType the entityType
   * @param name the name
   * @return a new criteria type
   */
  static CriteriaType criteriaType(EntityType entityType, String name) {
    return new DefaultCriteriaType(entityType, name);
  }
}
