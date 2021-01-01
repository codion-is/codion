/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

/**
 * Defines a custom condition type.
 */
public interface ConditionType {

  /**
   * @return the entity type
   */
  EntityType<?> getEntityType();

  /**
   * @return the name
   */
  String getName();
}
