/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;

import java.io.Serializable;

/**
 * Provides background/foreground colors for entities.
 */
public interface ColorProvider extends Serializable {

  /**
   * Returns the Object representing the specific color to use for the given attribute
   * in the given entity, null in case of no specific color
   * @param entity the entity
   * @param attribute the attribute
   * @return the color to use for this entity and attribute, null if no color is specified
   */
  Object color(Entity entity, Attribute<?> attribute);
}
