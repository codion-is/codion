/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

/**
 * Defines a custom condition type.
 */
public interface ConditionType {

  /**
   * @return the entity type
   */
  EntityType getEntityType();

  /**
   * @return the name
   */
  String getName();

  /**
   * Instantiates a new {@link ConditionType} for the given entity type
   * @param entityType the entityType
   * @param name the name
   * @return a new condition type
   */
  static ConditionType conditionType(EntityType entityType, String name) {
    return new DefaultConditionType(entityType, name);
  }
}
