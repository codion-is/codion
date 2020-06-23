/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;

/**
 * Defines a custom condition type.
 */
public interface ConditionType extends Serializable {

  /**
   * @return the entity type
   */
  EntityType<?> getEntityType();

  /**
   * @return the name
   */
  String getName();
}
